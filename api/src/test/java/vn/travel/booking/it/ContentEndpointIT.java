package vn.travel.booking.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.travel.booking.web.generated.model.Departure;
import vn.travel.booking.web.generated.model.DepartureStatus;
import vn.travel.booking.web.generated.model.ErrorCode;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.HotelStay;
import vn.travel.booking.web.generated.model.ItineraryDay;
import vn.travel.booking.web.generated.model.Lecture;
import vn.travel.booking.web.generated.model.PostPage;
import vn.travel.booking.web.generated.model.ProductPage;
import vn.travel.booking.web.generated.model.Ref;
import vn.travel.booking.web.generated.model.Theme;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Đợt 1b — chủ đề, lịch trình, khách sạn, ngày khởi hành, bài viết, thuyết trình.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ContentEndpointIT {

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

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Hai sản phẩm: một tour đoàn 3 ngày có đủ lịch trình, chặng nghỉ và bốn ngày
     * khởi hành ở đủ bốn trạng thái; một {@code COMBO} để thử luật "loại này không
     * có lịch trình theo ngày".
     *
     * <p>Ngày lịch trình thứ ba <b>chỉ có bản {@code da}</b> — trường hợp rìa khó
     * chịu nhất của chính sách không-fallback: lịch trình thủng ngày ở giữa.
     */
    @BeforeEach
    void prepareData() {
        jdbc.execute("""
                DELETE FROM seat_hold;
                DELETE FROM departure_price;
                DELETE FROM departure;
                DELETE FROM post_tag;
                DELETE FROM post_translation;
                DELETE FROM post;
                DELETE FROM tag_translation;
                DELETE FROM tag;
                DELETE FROM lecture_translation;
                DELETE FROM lecture;
                DELETE FROM itinerary_day_translation;
                DELETE FROM itinerary_day;
                DELETE FROM product_hotel_stay;
                DELETE FROM product_theme;
                DELETE FROM theme_translation;
                DELETE FROM theme;
                DELETE FROM hotel_translation;
                DELETE FROM hotel;
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product_combo;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM pax_type;

                INSERT INTO region (id, code, sort_order) VALUES
                  ('e1000000-0000-4000-8000-0000000000a1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('e1000000-0000-4000-8000-0000000000a1','da','nordvietnam','Nordvietnam'),
                  ('e1000000-0000-4000-8000-0000000000a1','vi','mien-bac','Miền Bắc');

                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('e1000000-0000-4000-8000-0000000000b1','e1000000-0000-4000-8000-0000000000a1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('e1000000-0000-4000-8000-0000000000b1','da','hanoi','Hanoi'),
                  ('e1000000-0000-4000-8000-0000000000b1','vi','ha-noi','Hà Nội');

                INSERT INTO hotel (id, destination_id, name, stars) VALUES
                  ('e1000000-0000-4000-8000-0000000000c1',
                   'e1000000-0000-4000-8000-0000000000b1','Sofitel Legend Metropole',5);
                INSERT INTO hotel_translation (hotel_id, locale, description) VALUES
                  ('e1000000-0000-4000-8000-0000000000c1','da','Klassisk kolonihotel.');

                INSERT INTO theme (id, code, sort_order) VALUES
                  ('e1000000-0000-4000-8000-0000000000d1','TREKKING',1),
                  ('e1000000-0000-4000-8000-0000000000d2','RIVER_CRUISE',2);
                INSERT INTO theme_translation (theme_id, locale, slug, name) VALUES
                  ('e1000000-0000-4000-8000-0000000000d1','da','trekking','Trekking'),
                  ('e1000000-0000-4000-8000-0000000000d1','vi','di-bo-duong-dai','Đi bộ đường dài'),
                  ('e1000000-0000-4000-8000-0000000000d2','da','flodkrydstogt','Flodkrydstogt');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image) VALUES
                  ('e1000000-0000-4000-8000-0000000000e1','GROUP_TOUR',
                   'e1000000-0000-4000-8000-0000000000b1',3,'/img/p1.jpg'),
                  ('e1000000-0000-4000-8000-0000000000e2','COMBO',
                   'e1000000-0000-4000-8000-0000000000b1',4,'/img/p2.jpg');

                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES ('e1000000-0000-4000-8000-0000000000e1',12,20,12,'da',2);
                INSERT INTO product_combo (product_id, nights, valid_from, valid_to)
                VALUES ('e1000000-0000-4000-8000-0000000000e2',4,'2027-01-01','2027-12-31');

                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status) VALUES
                  ('e1000000-0000-4000-8000-0000000000e1','da','nord-til-syd','Nord til syd','Hele landet.',
                   ARRAY['Et.','To.'], ARRAY['A','B','C'],'Rismarker','PUBLISHED'),
                  ('e1000000-0000-4000-8000-0000000000e1','vi','bac-vao-nam','Bắc vào Nam','Trọn đất nước.',
                   ARRAY['Một.','Hai.'], ARRAY['A','B','C'],'Ruộng bậc thang','PUBLISHED'),
                  ('e1000000-0000-4000-8000-0000000000e2','da','byophold','Byophold','Fire nætter.',
                   ARRAY['Et.','To.'], ARRAY['A','B','C'],'Gade','PUBLISHED');

                INSERT INTO product_market (product_id, market, is_published, price_from) VALUES
                  ('e1000000-0000-4000-8000-0000000000e1','DK',TRUE,24990.00),
                  ('e1000000-0000-4000-8000-0000000000e2','DK',TRUE,6490.00);

                INSERT INTO product_theme (product_id, theme_id) VALUES
                  ('e1000000-0000-4000-8000-0000000000e1','e1000000-0000-4000-8000-0000000000d1');

                INSERT INTO product_hotel_stay (product_id, hotel_id, nights, sort_order) VALUES
                  ('e1000000-0000-4000-8000-0000000000e1','e1000000-0000-4000-8000-0000000000c1',2,1);

                INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
                  ('e1000000-0000-4000-8000-0000000000f1','e1000000-0000-4000-8000-0000000000e1',1,
                   'e1000000-0000-4000-8000-0000000000b1','e1000000-0000-4000-8000-0000000000c1'),
                  ('e1000000-0000-4000-8000-0000000000f2','e1000000-0000-4000-8000-0000000000e1',2,
                   'e1000000-0000-4000-8000-0000000000b1',NULL),
                  ('e1000000-0000-4000-8000-0000000000f3','e1000000-0000-4000-8000-0000000000e1',3,
                   NULL,NULL);
                INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
                  ('e1000000-0000-4000-8000-0000000000f1','da','Ankomst','Ankomst til Hanoi.'),
                  ('e1000000-0000-4000-8000-0000000000f1','vi','Đến nơi','Đến Hà Nội.'),
                  ('e1000000-0000-4000-8000-0000000000f2','da','Byrundtur','Rundtur i den gamle bydel.'),
                  ('e1000000-0000-4000-8000-0000000000f2','vi','Dạo phố','Dạo phố cổ.'),
                  ('e1000000-0000-4000-8000-0000000000f3','da','Hjemrejse','Flyet går om aftenen.');

                INSERT INTO departure (id, product_id, market, depart_date, return_date, days,
                                       base_status, capacity, seats_booked) VALUES
                  ('e1100000-0000-4000-8000-000000000001','e1000000-0000-4000-8000-0000000000e1','DK',
                   '2027-03-01','2027-03-03',3,'OPEN',20,4),
                  ('e1100000-0000-4000-8000-000000000002','e1000000-0000-4000-8000-0000000000e1','DK',
                   '2027-04-01','2027-04-03',3,'OPEN',20,14),
                  -- Còn 2 chỗ nhưng MỚI 10 khách, dưới ngưỡng đảm bảo 12 → FEW_SEATS.
                  -- Để 18 khách thì kết quả là GUARANTEED chứ không phải FEW_SEATS:
                  -- GUARANTEED xét TRƯỚC, và đó chính là chỗ dễ đặt kỳ vọng sai.
                  ('e1100000-0000-4000-8000-000000000003','e1000000-0000-4000-8000-0000000000e1','DK',
                   '2027-05-01','2027-05-03',3,'OPEN',12,10),
                  ('e1100000-0000-4000-8000-000000000004','e1000000-0000-4000-8000-0000000000e1','DK',
                   '2027-06-01','2027-06-03',3,'PENDING',20,0);

                INSERT INTO pax_type (id, market, code, min_age, max_age, discount_rate, sort_order)
                VALUES ('e1200000-0000-4000-8000-000000000001','DK','ADULT',12,NULL,0,1);

                INSERT INTO departure_price (departure_id, pax_type_id, occupancy, amount, currency) VALUES
                  ('e1100000-0000-4000-8000-000000000001','e1200000-0000-4000-8000-000000000001',
                   'DOUBLE',24990.00,'DKK'),
                  ('e1100000-0000-4000-8000-000000000001','e1200000-0000-4000-8000-000000000001',
                   'SINGLE',29990.00,'DKK');

                INSERT INTO tag (id, code, sort_order) VALUES
                  ('e1300000-0000-4000-8000-000000000001','FOOD',1),
                  ('e1300000-0000-4000-8000-000000000002','CULTURE',2);
                INSERT INTO tag_translation (tag_id, locale, slug, name) VALUES
                  ('e1300000-0000-4000-8000-000000000001','da','mad','Mad'),
                  ('e1300000-0000-4000-8000-000000000002','da','kultur','Kultur');

                INSERT INTO post (id, published_at) VALUES
                  ('e1400000-0000-4000-8000-000000000001', now() - interval '2 days'),
                  ('e1400000-0000-4000-8000-000000000002', now() - interval '1 day'),
                  ('e1400000-0000-4000-8000-000000000003', NULL);
                INSERT INTO post_translation (post_id, locale, slug, title, excerpt, body, status) VALUES
                  ('e1400000-0000-4000-8000-000000000001','da','street-food','Street food','Uddrag.',
                   ARRAY['Afsnit et.'],'PUBLISHED'),
                  ('e1400000-0000-4000-8000-000000000002','da','tempelbyen','Tempelbyen','Uddrag.',
                   ARRAY['Afsnit et.'],'PUBLISHED'),
                  ('e1400000-0000-4000-8000-000000000003','da','endnu-ikke','Endnu ikke','Uddrag.',
                   ARRAY['Afsnit et.'],'PUBLISHED');
                INSERT INTO post_tag (post_id, tag_id) VALUES
                  ('e1400000-0000-4000-8000-000000000001','e1300000-0000-4000-8000-000000000001'),
                  ('e1400000-0000-4000-8000-000000000001','e1300000-0000-4000-8000-000000000002'),
                  ('e1400000-0000-4000-8000-000000000002','e1300000-0000-4000-8000-000000000002');

                INSERT INTO lecture (id, market, event_date, start_time, city, venue, seats, seats_taken) VALUES
                  ('e1500000-0000-4000-8000-000000000001','DK', CURRENT_DATE + 30,'18:30','Odense','Bibliotek',60,12),
                  ('e1500000-0000-4000-8000-000000000002','DK', CURRENT_DATE - 5, '18:30','Aarhus',NULL,60,60);
                INSERT INTO lecture_translation (lecture_id, locale, title, description) VALUES
                  ('e1500000-0000-4000-8000-000000000001','da','Vietnam i dybden','Et foredrag.'),
                  ('e1500000-0000-4000-8000-000000000002','da','Allerede afholdt','Et foredrag.');
                """);
    }

    // ------------------------------------------------------------ chủ đề

    @Test
    @DisplayName("Chủ đề: đếm trong phạm vi (market, locale), chủ đề rỗng vẫn hiện")
    void themesCountScopedToMarketAndLocale() {
        Theme[] da = call("/api/v1/dk/themes", "da", Theme[].class).getBody();

        assertEquals(2, da.length);
        assertEquals("Trekking", da[0].getName());
        assertEquals(1, da[0].getProductCount());
        assertEquals(0, da[1].getProductCount(),
                "Chủ đề chưa có sản phẩm vẫn là lựa chọn hợp lệ của bộ lọc");
    }

    @Test
    @DisplayName("Chủ đề chưa dịch biến mất khỏi locale đó")
    void untranslatedThemeDisappears() {
        Theme[] vi = call("/api/v1/dk/themes", "vi", Theme[].class).getBody();

        assertEquals(1, vi.length);
        assertEquals("Đi bộ đường dài", vi[0].getName());
    }

    @Test
    @DisplayName("Lọc sản phẩm theo chủ đề: lặp lại được, nhiều giá trị nghĩa là HOẶC")
    void filterProductsByThemeRepeatableOr() {
        assertEquals(1, totalProducts("/api/v1/dk/products?theme=trekking"));

        // Hai chủ đề, sản phẩm chỉ mang một — vẫn ra, vì HOẶC.
        assertEquals(1, totalProducts("/api/v1/dk/products?theme=trekking&theme=flodkrydstogt"));

        assertEquals(0, totalProducts("/api/v1/dk/products?theme=flodkrydstogt"));
    }

    // ------------------------------------------------------------ lịch trình

    @Test
    @DisplayName("Lịch trình sắp theo ngày; ngày bay không có nơi ngủ đêm")
    void itineraryOrderedByDayFlightDayHasNoStay() {
        ItineraryDay[] days = call("/api/v1/dk/products/nord-til-syd/itinerary", "da",
                ItineraryDay[].class).getBody();

        assertEquals(3, days.length);
        assertEquals(List.of(1, 2, 3), Arrays.stream(days).map(ItineraryDay::getDayNumber).toList());
        assertEquals("Hanoi", days[0].getDestination().getName());
        assertEquals("Sofitel Legend Metropole", days[0].getHotelName());
        assertNull(days[2].getDestination(), "Ngày bay không có nơi ngủ đêm");
    }

    @Test
    @DisplayName("Ngày lịch trình chưa dịch biến mất — lịch trình thủng ngày, và quy tắc kiểm 3 sinh ra để bắt nó")
    void untranslatedDayLeavesGapInItinerary() {
        ItineraryDay[] vi = call("/api/v1/dk/products/bac-vao-nam/itinerary", "vi",
                ItineraryDay[].class).getBody();

        assertEquals(2, vi.length, "Ngày 3 chỉ có bản da nên biến mất khỏi locale vi");
        assertEquals(List.of(1, 2), Arrays.stream(vi).map(ItineraryDay::getDayNumber).toList());
    }

    @Test
    @DisplayName("COMBO không có lịch trình theo ngày — trả 404, không trả danh sách rỗng")
    void comboHasNoDailyItineraryReturns404() {
        ResponseEntity<ErrorResponse> response =
                call("/api/v1/dk/products/byophold/itinerary", "da", ErrorResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.NOT_FOUND, response.getBody().getCode());
    }

    // ------------------------------------------------------------ khách sạn

    @Test
    @DisplayName("Chặng nghỉ: tên khách sạn không dịch, mô tả thì có")
    void hotelStayNameNotTranslatedDescriptionIs() {
        HotelStay[] da = call("/api/v1/dk/products/nord-til-syd/hotels", "da", HotelStay[].class).getBody();
        assertEquals(1, da.length);
        assertEquals("Sofitel Legend Metropole", da[0].getName());
        assertEquals(5, da[0].getStars());
        assertEquals(2, da[0].getNights());
        assertEquals("Klassisk kolonihotel.", da[0].getDescription());

        HotelStay[] vi = call("/api/v1/dk/products/bac-vao-nam/hotels", "vi", HotelStay[].class).getBody();
        assertEquals("Sofitel Legend Metropole", vi[0].getName(),
                "Tên riêng giữ nguyên ở mọi ngôn ngữ — docs/24 mục 5");
        assertNull(vi[0].getDescription(),
                "Chưa có bản mô tả tiếng Việt thì bỏ trường, nhưng chặng nghỉ vẫn hiện");
    }

    // ------------------------------------------------------------ khởi hành

    @Test
    @DisplayName("Trạng thái ngày khởi hành là giá trị TÍNH RA, không phải cột trong CSDL")
    void departureStatusIsComputedNotStored() {
        Departure[] d = call("/api/v1/dk/products/nord-til-syd/departures", "da",
                Departure[].class).getBody();

        assertEquals(4, d.length);
        assertEquals(DepartureStatus.OPEN, d[0].getStatus());
        assertEquals(DepartureStatus.GUARANTEED, d[1].getStatus(),
                "14 khách đã đặt, ngưỡng 12 — cột base_status vẫn ghi OPEN");
        assertEquals(DepartureStatus.FEW_SEATS, d[2].getStatus());
        assertEquals(DepartureStatus.PENDING, d[3].getStatus());

        assertEquals("24990.00", d[0].getPriceFrom().getAmount(), "Giá thấp nhất của ngày đó");
        assertNull(d[1].getPriceFrom(), "Chưa nhập giá thì bỏ trường, không trả 0");
    }

    @Test
    @DisplayName("Chỗ đang giữ bị trừ khỏi số chỗ khả dụng — quên vế này là bán trùng chỗ cuối")
    void heldSeatsSubtractedFromAvailable() {
        assertEquals(16, firstDepartureDate().getSeatsAvailable());

        jdbc.update("""
                INSERT INTO seat_hold (id, departure_id, seats, session_ref, expires_at)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), 14, 'session-thu', now() + interval '20 minutes')
                """, "e1600000-0000-4000-8000-000000000001", "e1100000-0000-4000-8000-000000000001");

        Departure afterHold = firstDepartureDate();
        assertEquals(2, afterHold.getSeatsAvailable());
        assertEquals(DepartureStatus.FEW_SEATS, afterHold.getStatus(),
                "Trạng thái phải đổi theo số chỗ thật, không theo cột base_status");
    }

    @Test
    @DisplayName("Giữ chỗ hết hạn trả chỗ về kho ngay, không chờ job quét dọn")
    void expiredHoldReleasesSeatsImmediately() {
        jdbc.update("""
                INSERT INTO seat_hold (id, departure_id, seats, session_ref, expires_at)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), 14, 'session-cu', now() - interval '1 minute')
                """, "e1600000-0000-4000-8000-000000000002", "e1100000-0000-4000-8000-000000000001");

        assertEquals(16, firstDepartureDate().getSeatsAvailable(),
                "released_at vẫn NULL, nhưng expires_at đã qua nên chỗ không còn được tính");
    }

    @Test
    @DisplayName("Ngày khởi hành KHÔNG BAO GIỜ được cache")
    void departuresAreNeverCached() {
        String cache = call("/api/v1/dk/products/nord-til-syd/departures", "da", Departure[].class)
                .getHeaders().getCacheControl();

        assertNotNull(cache);
        assertTrue(cache.contains("no-store"),
                "Hiện số chỗ cũ là dẫn khách vào một giao dịch chắc chắn thất bại ở bước cuối");
    }

    // ------------------------------------------------------------ bài viết

    @Test
    @DisplayName("Bài viết: chỉ bài đã xuất bản, mới nhất trước, kèm thẻ")
    void publishedPostsNewestFirstWithTags() {
        PostPage postPage = call("/api/v1/dk/posts", "da", PostPage.class).getBody();

        assertEquals(2, postPage.getTotalItems(), "Bài chưa có published_at không được lọt ra");
        assertEquals("Tempelbyen", postPage.getItems().get(0).getTitle());
        assertEquals(List.of("Mad", "Kultur"),
                postPage.getItems().get(1).getTags().stream().map(Ref::getName).toList());
    }

    @Test
    @DisplayName("Lọc bài theo thẻ: lặp lại được, nghĩa là HOẶC, và không nhân đôi bản ghi")
    void filterPostsByTagRepeatableOrNoDuplicates() {
        assertEquals(1, totalPosts("/api/v1/dk/posts?tag=mad"));
        assertEquals(2, totalPosts("/api/v1/dk/posts?tag=kultur"));

        assertEquals(2, totalPosts("/api/v1/dk/posts?tag=mad&tag=kultur"),
                "Bài mang cả hai thẻ chỉ được đếm MỘT lần — dùng JOIN thay EXISTS là ra 3");
    }

    @Test
    @DisplayName("Bài chưa dịch trả 404 ở locale đó")
    void untranslatedPostReturns404() {
        assertEquals(HttpStatus.OK,
                call("/api/v1/dk/posts/street-food", "da", Object.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                call("/api/v1/dk/posts/street-food", "vi", ErrorResponse.class).getStatusCode());
    }

    // ------------------------------------------------------------ thuyết trình

    @Test
    @DisplayName("Chỉ buổi thuyết trình CHƯA diễn ra, lọc ở truy vấn")
    void onlyUpcomingLecturesFilteredInQuery() {
        Lecture[] l = call("/api/v1/dk/lectures", "da", Lecture[].class).getBody();

        assertEquals(1, l.length, "Buổi đã qua phải bị lọc ở truy vấn, không ở tầng hiển thị");
        assertEquals("Vietnam i dybden", l[0].getTitle());
        assertEquals("Odense", l[0].getCity());
        assertEquals("18:30", l[0].getStartTime());
        assertEquals(48, l[0].getSeatsAvailable(), "60 chỗ trừ 12 đã nhận — giá trị tính ra");
    }

    // ------------------------------------------------------------ tiện ích

    private long totalProducts(String path) {
        return call(path, "da", ProductPage.class).getBody().getTotalItems();
    }

    private long totalPosts(String path) {
        return call(path, "da", PostPage.class).getBody().getTotalItems();
    }

    private Departure firstDepartureDate() {
        return call("/api/v1/dk/products/nord-til-syd/departures", "da", Departure[].class)
                .getBody()[0];
    }

    private <T> ResponseEntity<T> call(String path, String locale, Class<T> type) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build()
                .get()
                .uri(path)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(type);
    }
}
