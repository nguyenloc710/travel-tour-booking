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
import vn.travel.booking.web.generated.model.AdminMediaList;
import vn.travel.booking.web.generated.model.ErrorCode;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.FieldError;
import vn.travel.booking.web.generated.model.MediaKind;
import vn.travel.booking.web.generated.model.ProductStop;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gắn tệp vào thực thể, và khối "bản đồ lộ trình" ở bề mặt khách.
 *
 * <p>Bài quan trọng nhất là {@link #stopsHideMediaWithoutAltInThatLocale()}: một
 * tấm ảnh không có {@code alt} ở locale đang đọc thì <b>biến mất</b> ở locale đó.
 * Nó trông như một lỗi, nên nó cần một bài test nói rằng đó là chủ ý — và cưỡng
 * chế bằng {@code INNER JOIN} chứ không bằng {@code if} (ADR-003).
 *
 * <p>Không cần MinIO ở đây: bài này kiểm bảng nối và đường đọc, nên các dòng
 * {@code media_asset} nạp thẳng bằng SQL. Luồng tải lên có bài riêng —
 * {@code MediaUploadIT}, và nó chạy MinIO thật.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MediaLinkIT {

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
        registry.add("travel.storage.public-base-url", () -> "http://kho.test/travel-media");
    }

    private static final String PASSWORD = "mat-khau-rat-dai";
    private static final String TOUR = "ff000000-0000-4000-8000-000000000001";
    private static final String COMBO = "ff000000-0000-4000-8000-000000000002";
    private static final String HANOI = "ff000000-0000-4000-8000-0000000000d1";
    private static final String ANH_DU_HAI_BAN = "ff000000-0000-4000-8000-0000000000e1";
    private static final String ANH_CHI_CO_DA = "ff000000-0000-4000-8000-0000000000e2";
    private static final String VIDEO = "ff000000-0000-4000-8000-0000000000e3";

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void chuanBi() {
        jdbc.execute("""
                DELETE FROM destination_media;
                DELETE FROM product_image;
                DELETE FROM media_asset_translation;
                DELETE FROM media_asset;
                DELETE FROM itinerary_day_translation;
                DELETE FROM itinerary_day;
                DELETE FROM product_market;
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
                  ('ff000000-0000-4000-8000-0000000000a1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('ff000000-0000-4000-8000-0000000000a1','da','nordvietnam','Nordvietnam'),
                  ('ff000000-0000-4000-8000-0000000000a1','vi','mien-bac','Miền Bắc');
                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('ff000000-0000-4000-8000-0000000000d1',
                   'ff000000-0000-4000-8000-0000000000a1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('ff000000-0000-4000-8000-0000000000d1','da','hanoi','Hanoi'),
                  ('ff000000-0000-4000-8000-0000000000d1','vi','ha-noi','Hà Nội');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('ff000000-0000-4000-8000-000000000001','GROUP_TOUR',
                        'ff000000-0000-4000-8000-0000000000d1',3,'/img/x.jpg'),
                       ('ff000000-0000-4000-8000-000000000002','COMBO',
                        'ff000000-0000-4000-8000-0000000000d1',4,'/img/y.jpg');
                INSERT INTO product_group_tour
                  (product_id, min_pax, max_pax, guaranteed_threshold, tour_leader_language, fitness_level)
                VALUES ('ff000000-0000-4000-8000-000000000001',10,20,8,'da',2);
                INSERT INTO product_combo (product_id, nights, valid_from, valid_to)
                VALUES ('ff000000-0000-4000-8000-000000000002',3,'2027-01-01','2027-12-31');

                INSERT INTO product_translation
                  (product_id, locale, slug, title, short_description, long_description,
                   why_choose_this, hero_image_alt, status)
                VALUES ('ff000000-0000-4000-8000-000000000001','da','nord-til-syd','Nord til syd',
                        'Kort.', ARRAY['Et.','To.'], ARRAY['A','B','C'], 'Rismarker','PUBLISHED'),
                       ('ff000000-0000-4000-8000-000000000001','vi','bac-vao-nam','Bắc vào Nam',
                        'Ngắn.', ARRAY['Một.','Hai.'], ARRAY['A','B','C'], 'Ruộng','PUBLISHED'),
                       ('ff000000-0000-4000-8000-000000000002','da','combo-hanoi','Combo Hanoi',
                        'Kort.', ARRAY['Et.','To.'], ARRAY['A','B','C'], 'Byen','PUBLISHED');
                INSERT INTO product_market (product_id, market, is_published) VALUES
                  ('ff000000-0000-4000-8000-000000000001','DK',TRUE),
                  ('ff000000-0000-4000-8000-000000000002','DK',TRUE);
                UPDATE market SET is_active = TRUE WHERE code = 'VN';
                INSERT INTO product_market (product_id, market, is_published) VALUES
                  ('ff000000-0000-4000-8000-000000000001','VN',TRUE);

                -- Ba tệp: một ảnh có alt cả hai bản, một ảnh CHỈ có bản da, và một
                -- video kèm ảnh bìa là chính tấm ảnh đầu.
                INSERT INTO media_asset (id, kind, path, width, height, byte_size, source) VALUES
                  ('ff000000-0000-4000-8000-0000000000e1','IMAGE','diem-den/hanoi-1.jpg',1600,900,120000,'SELF'),
                  ('ff000000-0000-4000-8000-0000000000e2','IMAGE','diem-den/hanoi-2.jpg',1600,900,130000,'SELF');
                INSERT INTO media_asset
                  (id, kind, path, width, height, byte_size, source, content_type,
                   duration_seconds, poster_asset_id)
                VALUES ('ff000000-0000-4000-8000-0000000000e3','VIDEO','diem-den/hanoi.mp4',
                        1920,1080,9000000,'SELF','video/mp4',42,
                        'ff000000-0000-4000-8000-0000000000e1');

                INSERT INTO media_asset_translation (asset_id, locale, alt) VALUES
                  ('ff000000-0000-4000-8000-0000000000e1','da','Gaden i Hanoi'),
                  ('ff000000-0000-4000-8000-0000000000e1','vi','Phố Hà Nội'),
                  ('ff000000-0000-4000-8000-0000000000e2','da','Kun dansk alt'),
                  ('ff000000-0000-4000-8000-0000000000e3','da','Hanoi fra oven'),
                  ('ff000000-0000-4000-8000-0000000000e3','vi','Hà Nội từ trên cao');

                -- Lịch trình: hai ngày ở Hà Nội, một ngày trên tàu.
                INSERT INTO itinerary_day (id, product_id, day_number, destination_id) VALUES
                  ('ff000000-0000-4000-8000-0000000000c1','ff000000-0000-4000-8000-000000000001',1,
                   'ff000000-0000-4000-8000-0000000000d1'),
                  ('ff000000-0000-4000-8000-0000000000c2','ff000000-0000-4000-8000-000000000001',2,
                   'ff000000-0000-4000-8000-0000000000d1'),
                  ('ff000000-0000-4000-8000-0000000000c3','ff000000-0000-4000-8000-000000000001',3,NULL);
                INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
                  ('ff000000-0000-4000-8000-0000000000c1','da','Ankomst','x'),
                  ('ff000000-0000-4000-8000-0000000000c2','da','Byen','y'),
                  ('ff000000-0000-4000-8000-0000000000c3','da','Nattog','z');
                """);

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        themNhanVien("ff100000-0000-4000-8000-000000000001", "admin@travel.test", hash, "ADMIN");
    }

    // ------------------------------------------------------------ bảng nối

    @Test
    @DisplayName("Điểm đến nhận cả ảnh lẫn video, và giữ đúng thứ tự đã sắp")
    void destinationTakesImagesAndVideo() {
        Session admin = login("admin@travel.test");

        ResponseEntity<AdminMediaList> luu = admin.call(HttpMethod.PUT,
                "/api/v1/admin/destinations/" + HANOI + "/media",
                """
                {"assetIds":["%s","%s","%s"]}
                """.formatted(VIDEO, ANH_DU_HAI_BAN, ANH_CHI_CO_DA),
                AdminMediaList.class);

        assertEquals(HttpStatus.OK, luu.getStatusCode());
        // Vị trí trong mảng CHÍNH LÀ sort_order — video đứng đầu vì người sắp đặt
        // nó đứng đầu, không vì nó là video.
        assertEquals(MediaKind.VIDEO, luu.getBody().getItems().get(0).getKind());
        assertEquals(42, luu.getBody().getItems().get(0).getDurationSeconds());
        assertNotNull(luu.getBody().getItems().get(0).getPosterUrl());
        assertEquals(MediaKind.IMAGE, luu.getBody().getItems().get(1).getKind());

        ResponseEntity<AdminMediaList> doc = admin.call(HttpMethod.GET,
                "/api/v1/admin/destinations/" + HANOI + "/media", null, AdminMediaList.class);
        assertEquals(3, doc.getBody().getItems().size());
        assertEquals(MediaKind.VIDEO, doc.getBody().getItems().get(0).getKind());
    }

    @Test
    @DisplayName("Bộ ảnh sản phẩm KHÔNG nhận video — bảng nối vẫn tên product_image")
    void productGalleryRefusesVideo() {
        ResponseEntity<ErrorResponse> loi = login("admin@travel.test").call(HttpMethod.PUT,
                "/api/v1/admin/products/" + TOUR + "/images",
                """
                {"assetIds":["%s","%s"]}
                """.formatted(ANH_DU_HAI_BAN, VIDEO),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_FAILED, loi.getBody().getCode());

        FieldError f = loi.getBody().getFields().getFirst();
        assertEquals("assetIds[1]", f.getPath());
        assertEquals("chiNhanAnh", f.getParams().get("reason"));
        assertEquals("VIDEO", f.getParams().get("kind"));
    }

    @Test
    @DisplayName("Cùng một tệp hai lần trong một mảng là lỗi, chỉ đúng phần tử sau")
    void duplicateAssetRejected() {
        ResponseEntity<ErrorResponse> loi = login("admin@travel.test").call(HttpMethod.PUT,
                "/api/v1/admin/destinations/" + HANOI + "/media",
                """
                {"assetIds":["%s","%s"]}
                """.formatted(ANH_DU_HAI_BAN, ANH_DU_HAI_BAN),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals("assetIds[1]", loi.getBody().getFields().getFirst().getPath());
        assertEquals("trungLap", loi.getBody().getFields().getFirst().getParams().get("reason"));
    }

    // ------------------------------------------------------- chặng dừng

    @Test
    @DisplayName("Chặng dừng dựng từ lịch trình: một điểm đến, hai đêm, hai ngày")
    void stopsComeFromItinerary() {
        gan(HANOI, ANH_DU_HAI_BAN, VIDEO);

        ResponseEntity<ProductStop[]> stops = khach("da", "/api/v1/dk/products/nord-til-syd/stops");

        assertEquals(HttpStatus.OK, stops.getStatusCode());
        // Ngày 3 không ngủ ở điểm đến nào nên không sinh ra chặng — đó là ngày
        // trên tàu, không phải dữ liệu thiếu.
        assertEquals(1, stops.getBody().length);

        ProductStop chang = stops.getBody()[0];
        assertEquals("hanoi", chang.getDestination().getSlug());
        assertEquals("Hanoi", chang.getDestination().getName());
        assertEquals(2, chang.getNights());
        assertEquals(List.of(1, 2), chang.getDayNumbers());
        assertEquals(2, chang.getMedia().size());

        // Địa chỉ đầy đủ ghép ở backend từ đường dẫn tương đối — ADR-011 mục 2.
        assertEquals("http://kho.test/travel-media/diem-den/hanoi-1.jpg",
                chang.getMedia().get(0).getUrl());
        assertEquals("Gaden i Hanoi", chang.getMedia().get(0).getAlt());
        assertEquals(MediaKind.VIDEO, chang.getMedia().get(1).getKind());
        assertEquals("http://kho.test/travel-media/diem-den/hanoi-1.jpg",
                chang.getMedia().get(1).getPosterUrl());
    }

    @Test
    @DisplayName("Tệp thiếu alt ở locale nào thì BIẾN MẤT ở locale đó")
    void stopsHideMediaWithoutAltInThatLocale() {
        gan(HANOI, ANH_DU_HAI_BAN, ANH_CHI_CO_DA, VIDEO);

        ProductStop[] da = khach("da", "/api/v1/dk/products/nord-til-syd/stops").getBody();
        ProductStop[] vi = khach("vi", "/api/v1/vn/products/bac-vao-nam/stops").getBody();

        assertEquals(3, da[0].getMedia().size(), "bản da có đủ ba tệp");
        // `ANH_CHI_CO_DA` không có alt bản vi nên nó không tới được mắt khách đọc
        // tiếng Việt. Hiện nó kèm alt tiếng Đan còn tệ hơn: trình đọc màn hình sẽ
        // đọc đúng câu đó (docs/24 mục 6).
        assertEquals(2, vi[0].getMedia().size(), "bản vi thiếu tấm chưa dịch alt");
        assertTrue(vi[0].getMedia().stream().noneMatch(m -> "Kun dansk alt".equals(m.getAlt())));
        assertEquals("Hà Nội", vi[0].getDestination().getName());
    }

    @Test
    @DisplayName("COMBO không có lộ trình theo chặng — 404, không phải mảng rỗng")
    void comboHasNoStops() {
        // Đọc thành chuỗi: thân 404 là một ErrorResponse, không phải mảng chặng.
        assertEquals(HttpStatus.NOT_FOUND,
                khachRaw("da", "/api/v1/dk/products/combo-hanoi/stops").getStatusCode());
    }

    @Test
    @DisplayName("Điểm đến chưa có tệp nào vẫn ra một chặng, media rỗng")
    void stopWithoutMediaStillAppears() {
        ProductStop[] stops = khach("da", "/api/v1/dk/products/nord-til-syd/stops").getBody();

        // Chặng chưa có tệp vẫn phải hiện ra: khối bản đồ lộ trình nói "đi qua
        // đâu, mấy đêm", và câu đó đúng cả khi chưa ai chọn ảnh.
        assertEquals(1, stops.length);
        assertTrue(stops[0].getMedia().isEmpty());
        assertEquals(2, stops[0].getNights());
    }

    // ------------------------------------------------------------ tiện ích

    private void gan(String destinationId, String... assetIds) {
        String mang = String.join(",", java.util.Arrays.stream(assetIds)
                .map(id -> "\"" + id + "\"").toList());
        ResponseEntity<AdminMediaList> luu = login("admin@travel.test").call(HttpMethod.PUT,
                "/api/v1/admin/destinations/" + destinationId + "/media",
                "{\"assetIds\":[" + mang + "]}", AdminMediaList.class);
        assertEquals(HttpStatus.OK, luu.getStatusCode());
    }

    private ResponseEntity<String> khachRaw(String locale, String path) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build()
                .get()
                .uri(path)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(String.class);
    }

    private ResponseEntity<ProductStop[]> khach(String locale, String path) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build()
                .get()
                .uri(path)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(ProductStop[].class);
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
