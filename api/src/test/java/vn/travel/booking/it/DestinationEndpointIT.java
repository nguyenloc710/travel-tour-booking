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
import vn.travel.booking.web.generated.model.Destination;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.ProductPage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Điểm đến — cùng ba chính sách như sản phẩm, kiểm lại ở một đường đọc khác.
 *
 * <p>Kiểm lại chứ không tin là đã đúng: mỗi adapter tự viết điều kiện lọc của
 * mình, nên một adapter quên {@code NOT soft_delete} thì test của adapter kia
 * vẫn xanh.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DestinationEndpointIT {

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
    int cong;

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Ba điểm đến, mỗi cái bắt một trường hợp:
     *
     * <ul>
     *   <li><b>Hanoi</b> — đủ hai bản dịch, có một sản phẩm bán ở cả hai thị trường
     *   <li><b>Hoi An</b> — đủ hai bản dịch, sản phẩm chỉ bán ở {@code DK}
     *   <li><b>Sapa</b> — <b>chỉ có bản {@code da}</b> và <b>không có sản phẩm nào</b>
     * </ul>
     *
     * <p>Sapa gánh hai việc: thử chính sách không-fallback, và thử rằng điểm đến
     * rỗng vẫn xuất hiện với số 0 thay vì biến mất.
     */
    @BeforeEach
    void chuanBiDuLieu() {
        jdbc.execute("""
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product_cruise;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;

                UPDATE market SET is_active = TRUE WHERE code = 'VN';

                INSERT INTO region (id, code, sort_order) VALUES
                  ('a1000000-0000-4000-8000-000000000001','NORTH',1),
                  ('a1000000-0000-4000-8000-000000000002','CENTRAL',2);

                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('a1000000-0000-4000-8000-000000000001','da','nordvietnam','Nordvietnam'),
                  ('a1000000-0000-4000-8000-000000000001','vi','mien-bac','Miền Bắc'),
                  ('a1000000-0000-4000-8000-000000000002','da','centralvietnam','Det centrale Vietnam'),
                  ('a1000000-0000-4000-8000-000000000002','vi','mien-trung','Miền Trung');

                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('b1000000-0000-4000-8000-000000000001','a1000000-0000-4000-8000-000000000001','HANOI',1),
                  ('b1000000-0000-4000-8000-000000000002','a1000000-0000-4000-8000-000000000001','SAPA',2),
                  ('b1000000-0000-4000-8000-000000000003','a1000000-0000-4000-8000-000000000002','HOIAN',1);

                INSERT INTO destination_translation (destination_id, locale, slug, name, summary) VALUES
                  ('b1000000-0000-4000-8000-000000000001','da','hanoi','Hanoi','Hovedstaden i nord.'),
                  ('b1000000-0000-4000-8000-000000000001','vi','ha-noi','Hà Nội','Thủ đô miền Bắc.'),
                  ('b1000000-0000-4000-8000-000000000002','da','sapa','Sapa',NULL),
                  ('b1000000-0000-4000-8000-000000000003','da','hoi-an','Hoi An','Lanternebyen.'),
                  ('b1000000-0000-4000-8000-000000000003','vi','hoi-an','Hội An','Phố đèn lồng.');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image) VALUES
                  ('c1000000-0000-4000-8000-000000000001','GROUP_TOUR',
                   'b1000000-0000-4000-8000-000000000001',14,'/img/p1.jpg'),
                  ('c1000000-0000-4000-8000-000000000002','CRUISE',
                   'b1000000-0000-4000-8000-000000000003',3,'/img/p2.jpg');

                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES ('c1000000-0000-4000-8000-000000000001',12,20,10,'da',2);
                INSERT INTO product_cruise (product_id, ship_name, port_count)
                VALUES ('c1000000-0000-4000-8000-000000000002','Bhaya Classic',4);

                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status) VALUES
                  ('c1000000-0000-4000-8000-000000000001','da','nord-til-syd','Nord til syd','Hele landet.',
                   ARRAY['Et.','To.'], ARRAY['A','B','C'],'Rismarker','PUBLISHED'),
                  ('c1000000-0000-4000-8000-000000000001','vi','bac-vao-nam','Bắc vào Nam','Trọn đất nước.',
                   ARRAY['Một.','Hai.'], ARRAY['A','B','C'],'Ruộng bậc thang','PUBLISHED'),
                  ('c1000000-0000-4000-8000-000000000002','da','halong-krydstogt','Halong krydstogt','To nætter.',
                   ARRAY['Et.','To.'], ARRAY['A','B','C'],'Halong','PUBLISHED');

                INSERT INTO product_market (product_id, market, is_published, price_from) VALUES
                  ('c1000000-0000-4000-8000-000000000001','DK',TRUE,24990.00),
                  ('c1000000-0000-4000-8000-000000000001','VN',TRUE,18900000.00),
                  ('c1000000-0000-4000-8000-000000000002','DK',TRUE,8990.00);
                """);
    }

    @Test
    @DisplayName("Locale da: đủ ba điểm đến, sắp theo miền rồi tới thứ tự trong miền")
    void locale_da() {
        Destination[] ds = danhSach("dk", "da", Map.of());

        assertEquals(List.of("Hanoi", "Sapa", "Hoi An"),
                Arrays.stream(ds).map(Destination::getName).toList(),
                "Sắp theo bảng chữ cái thì Hoi An lên đầu — danh sách phải đọc được như hành trình Bắc vào Nam");
    }

    @Test
    @DisplayName("Điểm đến chưa có sản phẩm nào vẫn xuất hiện, với số 0")
    void diemDenRongVanHien() {
        Destination sapa = tim(danhSach("dk", "da", Map.of()), "Sapa");

        assertEquals(0, sapa.getProductCount(),
                "Dùng JOIN thay vì truy vấn con thì Sapa biến mất, và trang điểm đến vừa viết xong không bao giờ hiện ra");
        assertNull(sapa.getSummary(), "Chưa có mô tả thì bỏ hẳn trường, không trả chuỗi rỗng");
    }

    @Test
    @DisplayName("Điểm đến thiếu bản dịch vi BIẾN MẤT khỏi locale vi")
    void khongFallback() {
        Destination[] ds = danhSach("dk", "vi", Map.of());

        assertEquals(2, ds.length, "Sapa chỉ có bản da nên phải biến mất");
        assertTrue(Arrays.stream(ds).noneMatch(d -> "Sapa".equals(d.getName())),
                "Không được hiện bản tiếng Đan thay thế — docs/02 mục 4");
        assertEquals("Hà Nội", ds[0].getName());
        assertEquals("Thủ đô miền Bắc.", ds[0].getSummary());
    }

    @Test
    @DisplayName("Số sản phẩm đếm trong phạm vi (market, locale)")
    void demTheoMarketVaLocale() {
        // DK/da: Hà Nội có P1, Hội An có P2.
        assertEquals(1, tim(danhSach("dk", "da", Map.of()), "Hanoi").getProductCount());
        assertEquals(1, tim(danhSach("dk", "da", Map.of()), "Hoi An").getProductCount());

        // DK/vi: P2 chưa dịch vi nên Hội An về 0, dù vẫn bán ở DK.
        assertEquals(0, tim(danhSach("dk", "vi", Map.of()), "Hội An").getProductCount());

        // VN: P2 chưa gán thị trường VN nên không được tính.
        assertEquals(1, tim(danhSach("vn", "vi", Map.of()), "Hà Nội").getProductCount());
        assertEquals(0, tim(danhSach("vn", "vi", Map.of()), "Hội An").getProductCount());
    }

    @Test
    @DisplayName("Lọc theo miền dùng slug của locale đang xem")
    void locTheoMien() {
        assertEquals(2, danhSach("dk", "da", Map.of("region", "nordvietnam")).length);
        assertEquals(1, danhSach("dk", "vi", Map.of("region", "mien-bac")).length,
                "Sapa chỉ có bản da nên miền Bắc ở locale vi chỉ còn Hà Nội");
        assertEquals(0, danhSach("dk", "vi", Map.of("region", "nordvietnam")).length,
                "Slug tiếng Đan không được dùng ở locale vi");
    }

    @Test
    @DisplayName("Điểm đến xoá mềm biến mất khỏi danh sách")
    void xoaMem() {
        jdbc.update("UPDATE destination SET soft_delete = TRUE WHERE id = CAST(? AS uuid)",
                "b1000000-0000-4000-8000-000000000002");

        assertEquals(2, danhSach("dk", "da", Map.of()).length);
    }

    @Test
    @DisplayName("Chi tiết một điểm đến, và slug phụ thuộc locale")
    void chiTiet() {
        ResponseEntity<Destination> da = goi("/api/v1/dk/destinations/hanoi", "da", Destination.class);
        assertEquals(HttpStatus.OK, da.getStatusCode());
        assertEquals("Hanoi", da.getBody().getName());
        assertEquals("nordvietnam", da.getBody().getRegion().getSlug());
        assertEquals("da", da.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));

        assertEquals(HttpStatus.NOT_FOUND,
                goi("/api/v1/dk/destinations/hanoi", "vi", ErrorResponse.class).getStatusCode(),
                "Slug tiếng Đan không mở được ở locale vi");
        assertEquals(HttpStatus.OK,
                goi("/api/v1/dk/destinations/ha-noi", "vi", Destination.class).getStatusCode());
    }

    @Test
    @DisplayName("Điểm đến chưa dịch trả 404 kèm mã, không trả bản da")
    void chuaDichTra404() {
        ResponseEntity<ErrorResponse> vi = goi("/api/v1/dk/destinations/sapa", "vi", ErrorResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, vi.getStatusCode());
        assertEquals("NOT_FOUND", vi.getBody().getCode());
    }

    @Test
    @DisplayName("Lọc sản phẩm theo điểm đến, dùng slug của locale đang xem")
    void locSanPhamTheoDiemDen() {
        ProductPage dk = goi("/api/v1/dk/products?destination=hoi-an", "da", ProductPage.class).getBody();
        assertEquals(1, dk.getTotalItems());
        assertEquals("Halong krydstogt", dk.getItems().get(0).getTitle());

        ProductPage vi = goi("/api/v1/dk/products?destination=ha-noi", "vi", ProductPage.class).getBody();
        assertEquals(1, vi.getTotalItems());
        assertEquals("Bắc vào Nam", vi.getItems().get(0).getTitle());
    }

    @Test
    @DisplayName("Thị trường đang tắt trả 404")
    void thiTruongDangTat() {
        jdbc.update("UPDATE market SET is_active = FALSE WHERE code = 'VN'");

        assertEquals(HttpStatus.NOT_FOUND,
                goi("/api/v1/vn/destinations", "vi", ErrorResponse.class).getStatusCode());
    }

    // ------------------------------------------------------------ tiện ích

    private Destination[] danhSach(String market, String locale, Map<String, String> thamSo) {
        ResponseEntity<Destination[]> phanHoi = client().get()
                .uri(b -> {
                    b.path("/api/v1/" + market + "/destinations");
                    thamSo.forEach((ten, gia_tri) -> b.queryParam(ten, gia_tri));
                    return b.build();
                })
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(Destination[].class);

        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
        return phanHoi.getBody();
    }

    private static Destination tim(Destination[] ds, String ten) {
        return Arrays.stream(ds)
                .filter(d -> ten.equals(d.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không thấy điểm đến " + ten));
    }

    private <T> ResponseEntity<T> goi(String duongDan, String locale, Class<T> kieu) {
        return client().get()
                .uri(duongDan)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(kieu);
    }

    private RestClient client() {
        // defaultStatusHandler nuốt lỗi để test đọc được cả phản hồi 4xx.
        return RestClient.builder()
                .baseUrl("http://localhost:" + cong)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build();
    }
}
