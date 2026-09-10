package vn.travel.booking.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.travel.booking.web.generated.model.AdminItinerary;
import vn.travel.booking.web.generated.model.AdminItineraryDay;
import vn.travel.booking.web.generated.model.ErrorCode;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.FieldError;
import vn.travel.booking.web.generated.model.FieldRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lịch trình từng ngày — đường ghi của trang quản trị.
 *
 * <p>Bài quan trọng nhất là {@link #savingSourceKeepsExistingTranslation()}:
 * lưu lại bản nguồn <b>không được</b> xoá công của người dịch. Đó là hệ quả của
 * việc giữ nguyên {@code itinerary_day.id} khi ngày còn trong mảng, và nếu ai đó
 * đổi sang "xoá hết rồi chèn lại" thì bài này là thứ duy nhất chặn.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ItineraryIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("travel")
                    .withUsername("travel")
                    .withPassword("travel");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final String PASSWORD = "mat-khau-rat-dai";
    private static final String TOUR = "ee000000-0000-4000-8000-000000000001";
    private static final String COMBO = "ee000000-0000-4000-8000-000000000002";
    private static final String DIEM_DEN = "ee000000-0000-4000-8000-0000000000d1";
    private static final String KHACH_SAN = "ee000000-0000-4000-8000-0000000000b1";

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void chuanBi() {
        jdbc.execute("""
                DELETE FROM itinerary_day_translation;
                DELETE FROM itinerary_day;
                DELETE FROM hotel_translation;
                DELETE FROM hotel;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product_combo;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM staff_user_role;
                DELETE FROM staff_user;

                INSERT INTO region (id, code, sort_order) VALUES
                  ('ee000000-0000-4000-8000-0000000000a1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('ee000000-0000-4000-8000-0000000000a1','da','nordvietnam','Nordvietnam'),
                  ('ee000000-0000-4000-8000-0000000000a1','vi','mien-bac','Miền Bắc');
                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('ee000000-0000-4000-8000-0000000000d1',
                   'ee000000-0000-4000-8000-0000000000a1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('ee000000-0000-4000-8000-0000000000d1','da','hanoi','Hanoi'),
                  ('ee000000-0000-4000-8000-0000000000d1','vi','ha-noi','Hà Nội');
                INSERT INTO hotel (id, destination_id, name, stars) VALUES
                  ('ee000000-0000-4000-8000-0000000000b1',
                   'ee000000-0000-4000-8000-0000000000d1','Hotel Metropole',5);

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('ee000000-0000-4000-8000-000000000001','GROUP_TOUR',
                        'ee000000-0000-4000-8000-0000000000d1',3,'/img/x.jpg'),
                       ('ee000000-0000-4000-8000-000000000002','COMBO',
                        'ee000000-0000-4000-8000-0000000000d1',4,'/img/y.jpg');
                INSERT INTO product_group_tour
                  (product_id, min_pax, max_pax, guaranteed_threshold, tour_leader_language, fitness_level)
                VALUES ('ee000000-0000-4000-8000-000000000001',10,20,8,'da',2);
                INSERT INTO product_combo (product_id, nights, valid_from, valid_to)
                VALUES ('ee000000-0000-4000-8000-000000000002',3,'2027-01-01','2027-12-31');

                INSERT INTO product_translation
                  (product_id, locale, slug, title, short_description, long_description,
                   why_choose_this, hero_image_alt, status)
                VALUES ('ee000000-0000-4000-8000-000000000001','da','nord-til-syd','Nord til syd',
                        'Kort.', ARRAY['Et.','To.'], ARRAY['A','B','C'], 'Rismarker','PUBLISHED'),
                       ('ee000000-0000-4000-8000-000000000002','da','combo-hanoi','Combo Hanoi',
                        'Kort.', ARRAY['Et.','To.'], ARRAY['A','B','C'], 'Byen','PUBLISHED');
                """);

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        themNhanVien("ee100000-0000-4000-8000-000000000001", "admin@travel.test", hash, "ADMIN");
        themNhanVien("ee100000-0000-4000-8000-000000000002", "dich@travel.test", hash, "TRANSLATOR");
    }

    // ------------------------------------------------------------ vòng khép kín

    @Test
    @DisplayName("Lưu ba ngày rồi đọc lại — kèm điểm đến, khách sạn và bản nguồn")
    void saveThenRead() {
        Session admin = login("admin@travel.test");

        ResponseEntity<AdminItinerary> luu = admin.call(HttpMethod.PUT, itinerary(TOUR),
                """
                {"days":[
                  {"dayNumber":1,"destinationId":"%s","hotelId":"%s",
                   "title":"Ankomst til Hanoi","description":"Vi henter jer i lufthavnen."},
                  {"dayNumber":2,"destinationId":"%s",
                   "title":"Gamle kvarter","description":"Gåtur og gadekøkken."},
                  {"dayNumber":3,"title":"Nattog mod nord","description":"Vi sover på toget."}
                ]}
                """.formatted(DIEM_DEN, KHACH_SAN, DIEM_DEN),
                AdminItinerary.class);

        assertEquals(HttpStatus.OK, luu.getStatusCode());
        assertEquals(3, luu.getBody().getDays().size());

        AdminItineraryDay ngay1 = luu.getBody().getDays().getFirst();
        assertEquals(1, ngay1.getDayNumber());
        // Tên trả sẵn ở ngôn ngữ nguồn để bảng đọc được mà không phải tra thêm.
        assertEquals("Hanoi", ngay1.getDestinationName());
        assertEquals("Hotel Metropole", ngay1.getHotelName());
        assertEquals(1, ngay1.getTranslations().size());
        assertEquals("da", ngay1.getTranslations().getFirst().getLocale());
        assertTrue(ngay1.getTranslations().getFirst().getIsSource());

        // Ngày trên tàu: không điểm đến, không khách sạn — trạng thái hợp lệ,
        // không phải dữ liệu thiếu.
        AdminItineraryDay ngay3 = luu.getBody().getDays().get(2);
        assertNull(ngay3.getDestinationId());
        assertNull(ngay3.getHotelName());

        ResponseEntity<AdminItinerary> doc = admin.call(HttpMethod.GET, itinerary(TOUR), null,
                AdminItinerary.class);
        assertEquals(HttpStatus.OK, doc.getStatusCode());
        assertEquals(3, doc.getBody().getDays().size());
    }

    @Test
    @DisplayName("Lưu lại bản nguồn KHÔNG xoá bản dịch đã có")
    void savingSourceKeepsExistingTranslation() {
        Session admin = login("admin@travel.test");
        luuBaNgay(admin);

        admin.call(HttpMethod.PUT, itinerary(TOUR) + "/translations/vi",
                """
                {"days":[
                  {"dayNumber":1,"title":"Đến Hà Nội","description":"Xe đón tại sân bay."},
                  {"dayNumber":2,"title":"Phố cổ","description":"Đi bộ và ăn hàng."},
                  {"dayNumber":3,"title":"Tàu đêm","description":"Ngủ trên tàu."}
                ]}
                """,
                AdminItinerary.class);

        // Sửa bản nguồn: đổi chữ ngày 2, giữ nguyên số ngày.
        ResponseEntity<AdminItinerary> lai = admin.call(HttpMethod.PUT, itinerary(TOUR),
                """
                {"days":[
                  {"dayNumber":1,"destinationId":"%s","title":"Ankomst","description":"Ny tekst."},
                  {"dayNumber":2,"title":"Gamle kvarter","description":"Rettet tekst."},
                  {"dayNumber":3,"title":"Nattog","description":"Vi sover på toget."}
                ]}
                """.formatted(DIEM_DEN),
                AdminItinerary.class);

        assertEquals(HttpStatus.OK, lai.getStatusCode());
        // Bản vi vẫn còn ở cả ba ngày: id của ngày được giữ nguyên, nên bản dịch
        // treo vào nó không bị cuốn theo.
        for (AdminItineraryDay d : lai.getBody().getDays()) {
            assertEquals(2, d.getTranslations().size(), "ngày " + d.getDayNumber());
        }
        assertEquals("Đến Hà Nội", chu(lai.getBody().getDays().getFirst(), "vi"));
        assertEquals("Ny tekst.", moTa(lai.getBody().getDays().getFirst(), "da"));
    }

    // ------------------------------------------------------------ luật

    @Test
    @DisplayName("Số ngày phải bằng durationDays, không thì 400 chỉ đúng ô days")
    void dayCountMustMatchDuration() {
        ResponseEntity<ErrorResponse> loi = login("admin@travel.test").call(
                HttpMethod.PUT, itinerary(TOUR),
                """
                {"days":[{"dayNumber":1,"title":"En dag","description":"Kun én."}]}
                """,
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_FAILED, loi.getBody().getCode());

        FieldError f = loi.getBody().getFields().getFirst();
        assertEquals("days", f.getPath());
        assertEquals(FieldRule.SIZE, f.getCode());
        // Giới hạn là durationDays của chính sản phẩm này, không phải hằng số.
        assertEquals(3, f.getParams().get("min"));
        assertEquals(3, f.getParams().get("max"));
    }

    @Test
    @DisplayName("dayNumber thủng thì báo đúng ô của ngày sai")
    void dayNumbersMustBeContiguous() {
        ResponseEntity<ErrorResponse> loi = login("admin@travel.test").call(
                HttpMethod.PUT, itinerary(TOUR),
                """
                {"days":[
                  {"dayNumber":1,"title":"En","description":"x"},
                  {"dayNumber":3,"title":"Tre","description":"y"},
                  {"dayNumber":4,"title":"Fire","description":"z"}
                ]}
                """,
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        List<FieldError> fields = loi.getBody().getFields();
        // Thủng ở phần tử thứ hai và thứ ba: cả hai đều lệch số mong đợi.
        assertEquals("days[1].dayNumber", fields.getFirst().getPath());
        assertEquals(FieldRule.INVALID, fields.getFirst().getCode());
        assertEquals(2, fields.getFirst().getParams().get("expected"));
    }

    @Test
    @DisplayName("Điểm đến không tồn tại thì báo đúng ô của ngày đó")
    void unknownDestinationRejected() {
        ResponseEntity<ErrorResponse> loi = login("admin@travel.test").call(
                HttpMethod.PUT, itinerary(TOUR),
                """
                {"days":[
                  {"dayNumber":1,"title":"En","description":"x"},
                  {"dayNumber":2,"destinationId":"ee000000-0000-4000-8000-00000000dead",
                   "title":"To","description":"y"},
                  {"dayNumber":3,"title":"Tre","description":"z"}
                ]}
                """,
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals("days[1].destinationId", loi.getBody().getFields().getFirst().getPath());
    }

    @Test
    @DisplayName("COMBO không có lịch trình theo ngày — 404, không phải mảng rỗng")
    void comboHasNoItinerary() {
        Session admin = login("admin@travel.test");

        assertEquals(HttpStatus.NOT_FOUND,
                admin.call(HttpMethod.GET, itinerary(COMBO), null, ErrorResponse.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                admin.call(HttpMethod.PUT, itinerary(COMBO),
                        """
                        {"days":[{"dayNumber":1,"title":"x","description":"y"}]}
                        """,
                        ErrorResponse.class).getStatusCode());
    }

    @Test
    @DisplayName("Người dịch: 403 ở bản nguồn, ghi được bản vi, và không đổi được cấu trúc")
    void translatorOwnsOnlyTheTranslation() {
        luuBaNgay(login("admin@travel.test"));
        Session dich = login("dich@travel.test");

        assertEquals(HttpStatus.FORBIDDEN,
                dich.call(HttpMethod.PUT, itinerary(TOUR) + "/translations/da",
                        """
                        {"days":[{"dayNumber":1,"title":"x","description":"y"}]}
                        """,
                        ErrorResponse.class).getStatusCode());

        // Cấu trúc là việc của EDITOR — người dịch không có cửa vào đó.
        assertEquals(HttpStatus.FORBIDDEN,
                dich.call(HttpMethod.PUT, itinerary(TOUR),
                        """
                        {"days":[{"dayNumber":1,"title":"x","description":"y"}]}
                        """,
                        ErrorResponse.class).getStatusCode());

        // Dịch nửa chừng: gửi hai ngày trong khi lịch trình có ba.
        ResponseEntity<ErrorResponse> nuaChung = dich.call(HttpMethod.PUT,
                itinerary(TOUR) + "/translations/vi",
                """
                {"days":[
                  {"dayNumber":1,"title":"Đến Hà Nội","description":"x"},
                  {"dayNumber":2,"title":"Phố cổ","description":"y"}
                ]}
                """,
                ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, nuaChung.getStatusCode());
        assertEquals("days", nuaChung.getBody().getFields().getFirst().getPath());

        ResponseEntity<AdminItinerary> du = dich.call(HttpMethod.PUT,
                itinerary(TOUR) + "/translations/vi",
                """
                {"days":[
                  {"dayNumber":1,"title":"Đến Hà Nội","description":"x"},
                  {"dayNumber":2,"title":"Phố cổ","description":"y"},
                  {"dayNumber":3,"title":"Tàu đêm","description":"z"}
                ]}
                """,
                AdminItinerary.class);
        assertEquals(HttpStatus.OK, du.getStatusCode());
        assertEquals(2, du.getBody().getDays().getFirst().getTranslations().size());
    }

    // ------------------------------------------------------------ tiện ích

    private static String itinerary(String productId) {
        return "/api/v1/admin/products/" + productId + "/itinerary";
    }

    private void luuBaNgay(Session session) {
        ResponseEntity<AdminItinerary> luu = session.call(HttpMethod.PUT, itinerary(TOUR),
                """
                {"days":[
                  {"dayNumber":1,"destinationId":"%s","hotelId":"%s",
                   "title":"Ankomst til Hanoi","description":"Vi henter jer i lufthavnen."},
                  {"dayNumber":2,"destinationId":"%s",
                   "title":"Gamle kvarter","description":"Gåtur og gadekøkken."},
                  {"dayNumber":3,"title":"Nattog mod nord","description":"Vi sover på toget."}
                ]}
                """.formatted(DIEM_DEN, KHACH_SAN, DIEM_DEN),
                AdminItinerary.class);
        assertEquals(HttpStatus.OK, luu.getStatusCode());
    }

    private static String chu(AdminItineraryDay d, String locale) {
        return d.getTranslations().stream()
                .filter(t -> t.getLocale().equals(locale))
                .map(t -> t.getTitle())
                .findFirst().orElse(null);
    }

    private static String moTa(AdminItineraryDay d, String locale) {
        return d.getTranslations().stream()
                .filter(t -> t.getLocale().equals(locale))
                .map(t -> t.getDescription())
                .findFirst().orElse(null);
    }

    private void themNhanVien(String id, String email, String hash, String role) {
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash, is_active)
                VALUES (?::uuid, ?, ?, ?, TRUE)
                """, id, email, email, hash);
        jdbc.update("""
                INSERT INTO staff_user_role (id, staff_user_id, role_code)
                VALUES (gen_random_uuid(), ?::uuid, ?)
                """, id, role);
    }

    private Session login(String email) {
        Session session = new Session();
        assertEquals(HttpStatus.NO_CONTENT, session.login(email, PASSWORD).getStatusCode());
        return session;
    }

    /** Giữ cookie phiên và thẻ CSRF giữa các lời gọi — đúng như trình duyệt làm. */
    private final class Session {

        private final List<String> cookies = new ArrayList<>();

        ResponseEntity<String> login(String email, String password) {
            return call(HttpMethod.POST, "/api/v1/admin/session",
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password),
                    String.class);
        }

        <T> ResponseEntity<T> call(HttpMethod httpMethod, String path, String body, Class<T> type) {
            final RestClient.RequestBodySpec request = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultStatusHandler(status -> true, (req, res) -> { })
                    .build()
                    .method(httpMethod)
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON);

            if (!cookies.isEmpty()) {
                request.header(HttpHeaders.COOKIE, String.join("; ", cookies));
                csrf().ifPresent(token -> request.header("X-XSRF-TOKEN", token));
            }
            if (body != null) {
                request.body(body);
            }

            ResponseEntity<T> response = request.retrieve().toEntity(type);
            response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE)
                    .forEach(c -> cookies.add(c.split(";", 2)[0]));
            return response;
        }

        private Optional<String> csrf() {
            return cookies.stream()
                    .filter(c -> c.startsWith("XSRF-TOKEN="))
                    .map(c -> c.substring("XSRF-TOKEN=".length()))
                    .reduce((a, b) -> b);
        }
    }
}
