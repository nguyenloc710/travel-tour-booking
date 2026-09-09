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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void prepareData() {
        jdbc.execute("""
                DELETE FROM destination_image;
                DELETE FROM media_asset_translation;
                DELETE FROM media_asset;
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

                -- Ảnh minh hoạ. Hà Nội có HAI tấm, sort_order NGƯỢC thứ tự chèn:
                -- tầng đọc phải lấy tấm sort_order = 1, không phải tấm chèn trước.
                -- Hội An có một tấm CHỈ CÓ alt tiếng Đan. Sapa không có ảnh nào.
                INSERT INTO media_asset (id, path, width, height, byte_size, source, licence_ref) VALUES
                  ('d1000000-0000-4000-8000-000000000001','diem-den/ha-noi.jpg',1400,933,200000,'PURCHASED','ref-1'),
                  ('d1000000-0000-4000-8000-000000000002','diem-den/ha-noi-2.jpg',1200,800,150000,'PURCHASED','ref-2'),
                  ('d1000000-0000-4000-8000-000000000003','diem-den/hoi-an.jpg',900,600,100000,'PURCHASED','ref-3');

                INSERT INTO media_asset_translation (asset_id, locale, alt) VALUES
                  ('d1000000-0000-4000-8000-000000000001','da','Foerste'),
                  ('d1000000-0000-4000-8000-000000000001','vi','Tấm đầu'),
                  ('d1000000-0000-4000-8000-000000000002','da','Anden'),
                  ('d1000000-0000-4000-8000-000000000002','vi','Tấm hai'),
                  ('d1000000-0000-4000-8000-000000000003','da','Kun dansk');

                INSERT INTO destination_image (destination_id, asset_id, sort_order) VALUES
                  ('b1000000-0000-4000-8000-000000000001','d1000000-0000-4000-8000-000000000002',2),
                  ('b1000000-0000-4000-8000-000000000001','d1000000-0000-4000-8000-000000000001',1),
                  ('b1000000-0000-4000-8000-000000000003','d1000000-0000-4000-8000-000000000003',1);

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
    void localeDaReturnsAllThreeDestinations() {
        Destination[] ds = list("dk", "da", Map.of());

        assertEquals(List.of("Hanoi", "Sapa", "Hoi An"),
                Arrays.stream(ds).map(Destination::getName).toList(),
                "Sắp theo bảng chữ cái thì Hoi An lên đầu — danh sách phải đọc được như hành trình Bắc vào Nam");
    }

    @Test
    @DisplayName("Điểm đến chưa có sản phẩm nào vẫn xuất hiện, với số 0")
    void emptyDestinationStillListedWithZero() {
        Destination sapa = find(list("dk", "da", Map.of()), "Sapa");

        assertEquals(0, sapa.getProductCount(),
                "Dùng JOIN thay vì truy vấn con thì Sapa biến mất, và trang điểm đến vừa viết xong không bao giờ hiện ra");
        assertNull(sapa.getSummary(), "Chưa có mô tả thì bỏ hẳn trường, không trả chuỗi rỗng");
    }

    @Test
    @DisplayName("Điểm đến thiếu bản dịch vi BIẾN MẤT khỏi locale vi")
    void untranslatedDestinationDisappearsInVi() {
        Destination[] ds = list("dk", "vi", Map.of());

        assertEquals(2, ds.length, "Sapa chỉ có bản da nên phải biến mất");
        assertTrue(Arrays.stream(ds).noneMatch(d -> "Sapa".equals(d.getName())),
                "Không được hiện bản tiếng Đan thay thế — docs/02 mục 4");
        assertEquals("Hà Nội", ds[0].getName());
        assertEquals("Thủ đô miền Bắc.", ds[0].getSummary());
    }

    @Test
    @DisplayName("Ảnh minh hoạ: lấy tấm sort_order nhỏ nhất, URL ghép từ đường dẫn tương đối")
    void heroImageUsesLowestSortOrder() {
        Destination hn = find(list("dk", "da", Map.of()), "Hanoi");

        assertNotNull(hn.getImage());
        assertEquals("Foerste", hn.getImage().getAlt(),
                "Tấm chèn TRƯỚC có sort_order 2 — quên ORDER BY thì bài này đỏ");
        // CSDL lưu `diem-den/ha-noi.jpg`; địa chỉ gốc ở cấu hình, không ở dữ liệu.
        assertTrue(hn.getImage().getUrl().endsWith("/diem-den/ha-noi.jpg"), hn.getImage().getUrl());
        assertEquals(1400, hn.getImage().getWidth());
        assertEquals(933, hn.getImage().getHeight());
    }

    /**
     * Điểm đến chưa có ảnh vẫn phải hiện ra — cùng một cái bẫy mà
     * {@code emptyDestinationStillListedWithZero} canh cho {@code productCount}, chỉ khác chỗ:
     * {@code JOIN} thay vì {@code LEFT JOIN LATERAL} thì Sapa biến mất khỏi
     * danh sách mà không ai báo.
     */
    @Test
    @DisplayName("Điểm đến chưa có ảnh vẫn xuất hiện, chỉ là không có trường image")
    void destinationWithoutImageStillListed() {
        Destination sapa = find(list("dk", "da", Map.of()), "Sapa");

        assertNull(sapa.getImage(), "Không có ảnh thì bỏ hẳn trường, không trả đối tượng toàn null");
    }

    /**
     * Luật không fallback áp vào ảnh. Tấm của Hội An chỉ có {@code alt} tiếng
     * Đan; ở locale {@code vi} nó phải vắng, và điểm đến vẫn hiện bình thường.
     */
    @Test
    @DisplayName("Ảnh thiếu alt ở locale nào thì vắng khỏi locale đó, điểm đến vẫn còn")
    void imageMissingAltAbsentInThatLocale() {
        assertNotNull(find(list("dk", "da", Map.of()), "Hoi An").getImage());

        Destination hoiAnVi = find(list("dk", "vi", Map.of()), "Hội An");
        assertNull(hoiAnVi.getImage(),
                "Hiện ảnh kèm alt tiếng Đan giữa trang tiếng Việt là đọc sai cho đúng "
                        + "nhóm người phụ thuộc vào alt nhất");
    }

    @Test
    @DisplayName("Số sản phẩm đếm trong phạm vi (market, locale)")
    void countScopedToMarketAndLocale() {
        // DK/da: Hà Nội có P1, Hội An có P2.
        assertEquals(1, find(list("dk", "da", Map.of()), "Hanoi").getProductCount());
        assertEquals(1, find(list("dk", "da", Map.of()), "Hoi An").getProductCount());

        // DK/vi: P2 chưa dịch vi nên Hội An về 0, dù vẫn bán ở DK.
        assertEquals(0, find(list("dk", "vi", Map.of()), "Hội An").getProductCount());

        // VN: P2 chưa gán thị trường VN nên không được tính.
        assertEquals(1, find(list("vn", "vi", Map.of()), "Hà Nội").getProductCount());
        assertEquals(0, find(list("vn", "vi", Map.of()), "Hội An").getProductCount());
    }

    @Test
    @DisplayName("Lọc theo miền dùng slug của locale đang xem")
    void filterByRegionUsesCurrentLocaleSlug() {
        assertEquals(2, list("dk", "da", Map.of("region", "nordvietnam")).length);
        assertEquals(1, list("dk", "vi", Map.of("region", "mien-bac")).length,
                "Sapa chỉ có bản da nên miền Bắc ở locale vi chỉ còn Hà Nội");
        assertEquals(0, list("dk", "vi", Map.of("region", "nordvietnam")).length,
                "Slug tiếng Đan không được dùng ở locale vi");
    }

    @Test
    @DisplayName("Điểm đến xoá mềm biến mất khỏi danh sách")
    void softDeletedDestinationDisappears() {
        jdbc.update("UPDATE destination SET soft_delete = TRUE WHERE id = CAST(? AS uuid)",
                "b1000000-0000-4000-8000-000000000002");

        assertEquals(2, list("dk", "da", Map.of()).length);
    }

    @Test
    @DisplayName("Chi tiết một điểm đến, và slug phụ thuộc locale")
    void destinationDetailSlugDependsOnLocale() {
        ResponseEntity<Destination> da = call("/api/v1/dk/destinations/hanoi", "da", Destination.class);
        assertEquals(HttpStatus.OK, da.getStatusCode());
        assertEquals("Hanoi", da.getBody().getName());
        assertEquals("nordvietnam", da.getBody().getRegion().getSlug());
        assertEquals("da", da.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));

        assertEquals(HttpStatus.NOT_FOUND,
                call("/api/v1/dk/destinations/hanoi", "vi", ErrorResponse.class).getStatusCode(),
                "Slug tiếng Đan không mở được ở locale vi");
        assertEquals(HttpStatus.OK,
                call("/api/v1/dk/destinations/ha-noi", "vi", Destination.class).getStatusCode());
    }

    @Test
    @DisplayName("Điểm đến chưa dịch trả 404 kèm mã, không trả bản da")
    void untranslatedReturns404() {
        ResponseEntity<ErrorResponse> vi = call("/api/v1/dk/destinations/sapa", "vi", ErrorResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, vi.getStatusCode());
        assertEquals("NOT_FOUND", vi.getBody().getCode());
    }

    @Test
    @DisplayName("Lọc sản phẩm theo điểm đến, dùng slug của locale đang xem")
    void filterProductsByDestination() {
        ProductPage dk = call("/api/v1/dk/products?destination=hoi-an", "da", ProductPage.class).getBody();
        assertEquals(1, dk.getTotalItems());
        assertEquals("Halong krydstogt", dk.getItems().get(0).getTitle());

        ProductPage vi = call("/api/v1/dk/products?destination=ha-noi", "vi", ProductPage.class).getBody();
        assertEquals(1, vi.getTotalItems());
        assertEquals("Bắc vào Nam", vi.getItems().get(0).getTitle());
    }

    @Test
    @DisplayName("Thị trường đang tắt trả 404")
    void disabledMarketReturns404() {
        jdbc.update("UPDATE market SET is_active = FALSE WHERE code = 'VN'");

        assertEquals(HttpStatus.NOT_FOUND,
                call("/api/v1/vn/destinations", "vi", ErrorResponse.class).getStatusCode());
    }

    // ------------------------------------------------------------ tiện ích

    private Destination[] list(String market, String locale, Map<String, String> params) {
        ResponseEntity<Destination[]> response = client().get()
                .uri(b -> {
                    b.path("/api/v1/" + market + "/destinations");
                    params.forEach((name, gia_tri) -> b.queryParam(name, gia_tri));
                    return b.build();
                })
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(Destination[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private static Destination find(Destination[] ds, String name) {
        return Arrays.stream(ds)
                .filter(d -> name.equals(d.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không thấy điểm đến " + name));
    }

    private <T> ResponseEntity<T> call(String path, String locale, Class<T> type) {
        return client().get()
                .uri(path)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(type);
    }

    private RestClient client() {
        // defaultStatusHandler nuốt lỗi để test đọc được cả phản hồi 4xx.
        return RestClient.builder()
                .baseUrl("http://localhost:" + cong)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build();
    }
}
