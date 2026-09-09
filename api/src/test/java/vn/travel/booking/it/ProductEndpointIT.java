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
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.GalleryImage;
import vn.travel.booking.web.generated.model.GroupTourDetail;
import vn.travel.booking.web.generated.model.ProductPage;
import vn.travel.booking.web.generated.model.ProductSummary;
import vn.travel.booking.web.generated.model.SlugRedirect;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lõi danh mục trên Postgres THẬT — G3 tiêu chí ra 1, 2, 4, 5, 7.
 *
 * <p>Không dùng H2: hai thứ dễ sai nhất của dự án này là ICU collation và
 * {@code unaccent}, mà H2 không mô phỏng được cái nào (docs/10 mục 9).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductEndpointIT {

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
     * Năm sản phẩm, chọn để mỗi cái bắt một trường hợp rìa:
     *
     * <ul>
     *   <li><b>P1</b> có cả hai bản dịch và bán ở cả hai thị trường — trường hợp bình thường
     *   <li><b>P2</b> <b>chỉ có bản {@code da}</b> — phải biến mất khỏi locale {@code vi}
     *   <li><b>P3</b> tên có dấu tiếng Việt — để thử tìm không dấu
     *   <li><b>P4</b> chỉ xuất bản ở {@code VN} — phải biến mất khỏi thị trường {@code DK}
     *   <li><b>P5</b> tên bắt đầu bằng {@code Aa} — để thử collation Đan Mạch
     * </ul>
     *
     * <p>Chạy bằng một câu {@code execute} nhiều lệnh chứ không nhiều câu
     * {@code update}: hai constraint trigger của {@code product} là
     * {@code DEFERRABLE INITIALLY DEFERRED}, nên sản phẩm và bản dịch nguồn của
     * nó phải nằm trong <b>cùng một transaction</b>. Mỗi {@code jdbc.update} là
     * một transaction riêng, và câu đầu tiên sẽ đỏ ngay.
     */
    @BeforeEach
    void prepareData() {
        jdbc.execute("""
                DELETE FROM slug_history;
                DELETE FROM product_image;
                DELETE FROM media_asset_translation;
                DELETE FROM media_asset;
                DELETE FROM departure_price;
                DELETE FROM departure;
                DELETE FROM pax_type;
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product_cruise;
                DELETE FROM product_day_tour;
                DELETE FROM product_private;
                DELETE FROM product_combo;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;

                -- Thị trường VN đang tắt trong dữ liệu tra cứu vì sáu con số nghiệp
                -- vụ của nó chưa ai quyết (docs/41 mục 4, câu hỏi Q-2). Test bật lên
                -- để kiểm chính sách hai thị trường; có một test riêng tắt lại.
                UPDATE market SET is_active = TRUE WHERE code = 'VN';

                INSERT INTO region (id, code, sort_order) VALUES
                  ('a0000000-0000-4000-8000-000000000001','NORTH',1),
                  ('a0000000-0000-4000-8000-000000000002','CENTRAL',2);

                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('a0000000-0000-4000-8000-000000000001','da','nordvietnam','Nordvietnam'),
                  ('a0000000-0000-4000-8000-000000000001','vi','mien-bac','Miền Bắc'),
                  ('a0000000-0000-4000-8000-000000000002','da','centralvietnam','Det centrale Vietnam'),
                  ('a0000000-0000-4000-8000-000000000002','vi','mien-trung','Miền Trung');

                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('b0000000-0000-4000-8000-000000000001','a0000000-0000-4000-8000-000000000001','HANOI',1),
                  ('b0000000-0000-4000-8000-000000000002','a0000000-0000-4000-8000-000000000002','HOIAN',2);

                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('b0000000-0000-4000-8000-000000000001','da','hanoi','Hanoi'),
                  ('b0000000-0000-4000-8000-000000000001','vi','ha-noi','Hà Nội'),
                  ('b0000000-0000-4000-8000-000000000002','da','hoi-an','Hoi An'),
                  ('b0000000-0000-4000-8000-000000000002','vi','hoi-an','Hội An');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                                     hero_image, is_new, rating, review_count) VALUES
                  ('c0000000-0000-4000-8000-000000000001','GROUP_TOUR',
                   'b0000000-0000-4000-8000-000000000001',14,'/img/p1.jpg',TRUE,4.7,32),
                  ('c0000000-0000-4000-8000-000000000002','CRUISE',
                   'b0000000-0000-4000-8000-000000000002',3,'/img/p2.jpg',FALSE,NULL,0),
                  ('c0000000-0000-4000-8000-000000000003','DAY_TOUR',
                   'b0000000-0000-4000-8000-000000000002',NULL,'/img/p3.jpg',FALSE,4.2,11),
                  ('c0000000-0000-4000-8000-000000000004','PRIVATE_TOUR',
                   'b0000000-0000-4000-8000-000000000001',10,'/img/p4.jpg',FALSE,NULL,0),
                  ('c0000000-0000-4000-8000-000000000005','COMBO',
                   'b0000000-0000-4000-8000-000000000002',4,'/img/p5.jpg',FALSE,NULL,0);

                -- Bộ ảnh. Ba tấm cho GROUP_TOUR, đặt sort_order NGƯỢC thứ tự chèn để
                -- test bắt được nếu tầng đọc quên ORDER BY.
                INSERT INTO media_asset (id, path, width, height, byte_size, source, licence_ref) VALUES
                  ('d0000000-0000-4000-8000-000000000001','tour/mot.jpg',1400,933,200000,'PURCHASED','ref-1'),
                  ('d0000000-0000-4000-8000-000000000002','tour/hai.jpg',1200,800,150000,'PURCHASED','ref-2'),
                  ('d0000000-0000-4000-8000-000000000003','tour/ba.jpg',900,600,100000,'PURCHASED','ref-3'),
                  ('d0000000-0000-4000-8000-000000000004','tour/xoa-mem.jpg',800,600,90000,'PURCHASED','ref-4');

                -- Tấm `ba` cố ý KHÔNG có bản dịch `vi`: nó phải biến mất khỏi locale đó.
                INSERT INTO media_asset_translation (asset_id, locale, alt) VALUES
                  ('d0000000-0000-4000-8000-000000000001','da','Rismarker'),
                  ('d0000000-0000-4000-8000-000000000001','vi','Ruộng bậc thang'),
                  ('d0000000-0000-4000-8000-000000000002','da','Lanterner'),
                  ('d0000000-0000-4000-8000-000000000002','vi','Đèn lồng'),
                  ('d0000000-0000-4000-8000-000000000003','da','Kun dansk'),
                  ('d0000000-0000-4000-8000-000000000004','da','Blødt slettet'),
                  ('d0000000-0000-4000-8000-000000000004','vi','Đã xoá mềm');

                UPDATE media_asset SET soft_delete = TRUE
                 WHERE id = 'd0000000-0000-4000-8000-000000000004';

                INSERT INTO product_image (product_id, asset_id, sort_order) VALUES
                  ('c0000000-0000-4000-8000-000000000001','d0000000-0000-4000-8000-000000000003',3),
                  ('c0000000-0000-4000-8000-000000000001','d0000000-0000-4000-8000-000000000001',1),
                  ('c0000000-0000-4000-8000-000000000001','d0000000-0000-4000-8000-000000000002',2),
                  ('c0000000-0000-4000-8000-000000000001','d0000000-0000-4000-8000-000000000004',4);

                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level) VALUES
                  ('c0000000-0000-4000-8000-000000000001',12,20,10,'da',2);
                INSERT INTO product_cruise (product_id, ship_name, port_count) VALUES
                  ('c0000000-0000-4000-8000-000000000002','Bhaya Classic',4);
                INSERT INTO product_day_tour (product_id, duration_hours, cutoff_hours) VALUES
                  ('c0000000-0000-4000-8000-000000000003',5,12);
                INSERT INTO product_private (product_id, lead_time_days, quote_valid_days) VALUES
                  ('c0000000-0000-4000-8000-000000000004',21,14);
                INSERT INTO product_combo (product_id, nights, valid_from, valid_to) VALUES
                  ('c0000000-0000-4000-8000-000000000005',4,'2027-01-01','2027-12-31');

                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status) VALUES
                  ('c0000000-0000-4000-8000-000000000001','da','vietnam-fra-nord-til-syd',
                   'Vietnam fra nord til syd','Hele landet på fjorten dage.',
                   ARRAY['Første afsnit.','Andet afsnit.'],
                   ARRAY['Dansk rejseleder','Små grupper','Alt inkluderet'],
                   'Rismarker ved Sapa','PUBLISHED'),
                  ('c0000000-0000-4000-8000-000000000001','vi','viet-nam-tu-bac-vao-nam',
                   'Việt Nam từ Bắc vào Nam','Trọn vẹn đất nước trong mười bốn ngày.',
                   ARRAY['Đoạn một.','Đoạn hai.'],
                   ARRAY['Trưởng đoàn nói tiếng Việt','Đoàn nhỏ','Trọn gói'],
                   'Ruộng bậc thang Sa Pa','PUBLISHED'),

                  ('c0000000-0000-4000-8000-000000000002','da','aalborg-krydstogt',
                   'Ålborg-gruppens krydstogt','Tre dage i Halong-bugten.',
                   ARRAY['Første afsnit.','Andet afsnit.'],
                   ARRAY['Egen kahyt','Alle måltider','Kajak'],
                   'Halong-bugten ved solnedgang','PUBLISHED'),

                  ('c0000000-0000-4000-8000-000000000003','da','hoi-an-efter-moerkets-frembrud',
                   'Hoi An efter mørkets frembrud','En aften blandt lanternerne.',
                   ARRAY['Første afsnit.','Andet afsnit.'],
                   ARRAY['Lokal guide','Aftensmad','Bådtur'],
                   'Lanterner i Hoi An','PUBLISHED'),
                  ('c0000000-0000-4000-8000-000000000003','vi','hoi-an-ve-count',
                   'Hội An về đêm','Một buổi tối giữa những chiếc đèn lồng.',
                   ARRAY['Đoạn một.','Đoạn hai.'],
                   ARRAY['Hướng dẫn viên địa phương','Bữa tối','Đi thuyền'],
                   'Đèn lồng Hội An','PUBLISHED'),

                  ('c0000000-0000-4000-8000-000000000004','da','skraeddersyet-rejse',
                   'Skræddersyet rejse','Din egen rute.',
                   ARRAY['Første afsnit.','Andet afsnit.'],
                   ARRAY['Egen rute','Privat guide','Fleksible datoer'],
                   'Privat bil i Hanoi','PUBLISHED'),
                  ('c0000000-0000-4000-8000-000000000004','vi','tour-rieng-theo-yeu-cau',
                   'Tour riêng theo yêu cầu','Lộ trình của riêng bạn.',
                   ARRAY['Đoạn một.','Đoạn hai.'],
                   ARRAY['Lộ trình riêng','Hướng dẫn riêng','Ngày linh hoạt'],
                   'Xe riêng ở Hà Nội','PUBLISHED'),

                  ('c0000000-0000-4000-8000-000000000005','da','aarhus-kulturtur',
                   'Aarhus kulturtur','Fire nætter med kultur.',
                   ARRAY['Første afsnit.','Andet afsnit.'],
                   ARRAY['Hotel i centrum','Morgenmad','Museumsbillet'],
                   'Gadeliv i Hoi An','PUBLISHED');

                -- price_from KHÔNG đặt tay: từ migration V5 nó là cột vật chất hoá
                -- do trigger sở hữu, tính từ departure_price nguyên giá phòng đôi.
                -- Đặt tay ở đây thì trigger ghi đè ngay lúc INSERT, và bài test
                -- kiểm một giá trị mà hệ thống thật không bao giờ sinh ra.
                INSERT INTO product_market (product_id, market, is_published) VALUES
                  ('c0000000-0000-4000-8000-000000000001','DK',TRUE),
                  ('c0000000-0000-4000-8000-000000000001','VN',TRUE),
                  ('c0000000-0000-4000-8000-000000000002','DK',TRUE),
                  ('c0000000-0000-4000-8000-000000000003','DK',TRUE),
                  ('c0000000-0000-4000-8000-000000000004','VN',TRUE),
                  ('c0000000-0000-4000-8000-000000000005','DK',TRUE);

                -- pax_type không có trong migration R__: loại khách của từng thị
                -- trường là dữ liệu nghiệp vụ chưa chốt (Q-2).
                INSERT INTO pax_type (id, market, code, min_age, max_age, discount_rate, sort_order) VALUES
                  ('c0000000-0000-4000-8000-0000000000a1','DK','ADULT',18,NULL,0.0000,1),
                  ('c0000000-0000-4000-8000-0000000000a2','VN','ADULT',18,NULL,0.0000,1);

                INSERT INTO departure (id, product_id, market, depart_date, return_date,
                                       days, capacity) VALUES
                  ('c0000000-0000-4000-8000-0000000000d1','c0000000-0000-4000-8000-000000000001','DK','2027-03-14','2027-03-27',14,20),
                  ('c0000000-0000-4000-8000-0000000000d2','c0000000-0000-4000-8000-000000000001','VN','2027-03-14','2027-03-27',14,20),
                  ('c0000000-0000-4000-8000-0000000000d3','c0000000-0000-4000-8000-000000000002','DK','2027-04-02','2027-04-08',7,16),
                  ('c0000000-0000-4000-8000-0000000000d4','c0000000-0000-4000-8000-000000000004','VN','2027-04-02','2027-04-08',7,16),
                  ('c0000000-0000-4000-8000-0000000000d5','c0000000-0000-4000-8000-000000000005','DK','2027-05-02','2027-05-06',5,24);

                INSERT INTO departure_price (departure_id, pax_type_id, occupancy, amount, currency) VALUES
                  ('c0000000-0000-4000-8000-0000000000d1','c0000000-0000-4000-8000-0000000000a1','DOUBLE',24990.00,'DKK'),
                  ('c0000000-0000-4000-8000-0000000000d2','c0000000-0000-4000-8000-0000000000a2','DOUBLE',18900000,'VND'),
                  ('c0000000-0000-4000-8000-0000000000d3','c0000000-0000-4000-8000-0000000000a1','DOUBLE',8990.00,'DKK'),
                  ('c0000000-0000-4000-8000-0000000000d4','c0000000-0000-4000-8000-0000000000a2','DOUBLE',12000000,'VND'),
                  ('c0000000-0000-4000-8000-0000000000d5','c0000000-0000-4000-8000-0000000000a1','DOUBLE',6490.00,'DKK');
                """);
    }

    // ------------------------------------------------------------ listing

    @Test
    @DisplayName("Thị trường DK, locale da: bốn sản phẩm đã xuất bản, không có sản phẩm của VN")
    void listingDkDaShowsOnlyPublishedDkProducts() {
        ProductPage productPage = page(callListing("dk", "da", Map.of()));

        assertEquals(4, productPage.getTotalItems(),
                "P4 chỉ xuất bản ở VN nên không được lọt vào thị trường DK");
        assertTrue(productPage.getItems().stream().noneMatch(p -> "Skræddersyet rejse".equals(p.getTitle())));
    }

    @Test
    @DisplayName("Sản phẩm thiếu bản dịch vi BIẾN MẤT khỏi listing vi — không hiện bản da")
    void untranslatedProductDisappearsNoFallback() {
        ProductPage da = page(callListing("dk", "da", Map.of()));
        ProductPage vi = page(callListing("dk", "vi", Map.of()));

        assertEquals(4, da.getTotalItems());
        assertEquals(2, vi.getTotalItems(),
                "Chỉ P1 và P3 có bản vi; P2 và P5 chỉ có bản da nên phải biến mất");

        List<String> viName = vi.getItems().stream().map(ProductSummary::getTitle).toList();
        assertTrue(viName.stream().noneMatch(t -> t.contains("Ålborg")),
                "Hiện bản tiếng Đan thay thế là phá chính sách của docs/02 mục 4");

        // Đây chính là lý do "Xem tất cả N tour" không được hardcode: cùng một
        // thị trường, hai locale ra hai con số khác nhau.
        assertTrue(da.getTotalItems() != vi.getTotalItems());
    }

    @Test
    @DisplayName("Sắp theo tiêu đề dùng collation Đan Mạch: Aa và Å xếp SAU z")
    void sortsByTitleWithDanishCollation() {
        List<String> name = page(callListing("dk", "da", Map.of("sort", "title,asc")))
                .getItems().stream().map(ProductSummary::getTitle).toList();

        assertEquals(List.of(
                        "Hoi An efter mørkets frembrud",
                        "Vietnam fra nord til syd",
                        "Ålborg-gruppens krydstogt",
                        "Aarhus kulturtur"),
                name,
                "String.compareTo() của Java xếp Aarhus lên đầu — sai với người Đan Mạch");
    }

    @Test
    @DisplayName("Tìm không dấu: gõ hoi an ra Hội An")
    void accentInsensitiveSearch() {
        ProductPage result = page(callListing("dk", "vi", Map.of("q", "hoi an")));

        assertEquals(1, result.getTotalItems());
        assertEquals("Hội An về đêm", result.getItems().get(0).getTitle());
    }

    @Test
    @DisplayName("Lọc theo miền dùng slug CỦA LOCALE ĐANG XEM, không dùng mã miền")
    void filterByRegionUsesCurrentLocaleSlug() {
        assertEquals(1, page(callListing("dk", "da", Map.of("region", "nordvietnam"))).getTotalItems());
        assertEquals(1, page(callListing("dk", "vi", Map.of("region", "mien-bac"))).getTotalItems());
        assertEquals(0, page(callListing("dk", "vi", Map.of("region", "nordvietnam"))).getTotalItems(),
                "Slug tiếng Đan không được dùng ở locale vi");
    }

    @Test
    @DisplayName("Lọc theo loại sản phẩm")
    void filterByProductType() {
        ProductPage result = page(callListing("dk", "da", Map.of("productType", "CRUISE")));
        assertEquals(1, result.getTotalItems());
        assertEquals("Ålborg-gruppens krydstogt", result.getItems().get(0).getTitle());
    }

    @Test
    @DisplayName("Phân trang theo offset, tổng số đếm từ dữ liệu")
    void offsetPaginationWithTotalFromData() {
        ProductPage p0 = page(callListing("dk", "da", Map.of("size", "2", "page", "0")));
        ProductPage p1 = page(callListing("dk", "da", Map.of("size", "2", "page", "1")));
        ProductPage p2 = page(callListing("dk", "da", Map.of("size", "2", "page", "2")));

        assertEquals(4, p0.getTotalItems());
        assertEquals(2, p0.getTotalPages());
        assertEquals(2, p0.getItems().size());
        assertEquals(2, p1.getItems().size());
        assertTrue(p2.getItems().isEmpty(), "Trang vượt quá tổng số trả rỗng, không lỗi");

        assertTrue(p0.getItems().stream().noneMatch(a ->
                        p1.getItems().stream().anyMatch(b -> b.getSlug().equals(a.getSlug()))),
                "Thiếu tiêu chí sắp xếp phụ thì phân trang offset lặp bản ghi");
    }

    @Test
    @DisplayName("Giá làm tròn theo số chữ số thập phân của THỊ TRƯỜNG: DKK 2, VND 0")
    void priceRoundedByMarketFractionDigits() {
        ProductSummary dk = page(callListing("dk", "da", Map.of("q", "Vietnam"))).getItems().get(0);
        ProductSummary vn = page(callListing("vn", "vi", Map.of("q", "Viet"))).getItems().get(0);

        assertEquals("24990.00", dk.getPriceFrom().getAmount());
        assertEquals("DKK", dk.getPriceFrom().getCurrency());

        // Cùng một tour, giá của hai thị trường là hai con số do người nhập —
        // không phải kết quả nhân tỷ giá. Hệ thống này không có tỷ giá.
        assertEquals("18900000", vn.getPriceFrom().getAmount());
        assertEquals("VND", vn.getPriceFrom().getCurrency());
    }

    @Test
    @DisplayName("Chưa có giá thì bỏ hẳn trường priceFrom, không trả 0")
    void priceFromOmittedWhenNoPrice() {
        ProductSummary p3 = page(callListing("dk", "da", Map.of("q", "Hoi An"))).getItems().get(0);
        assertNull(p3.getPriceFrom(), "Trả 0 là nói với khách rằng tour này miễn phí");
    }

    @Test
    @DisplayName("Sản phẩm xoá mềm biến mất khỏi listing")
    void softDeletedProductDisappearsFromListing() {
        // CAST tường minh: cột là UUID, tham số JDBC là chuỗi, và Postgres
        // không tự ép — "operator does not exist: uuid = character varying".
        jdbc.update("UPDATE product SET soft_delete = TRUE WHERE id = CAST(? AS uuid)",
                "c0000000-0000-4000-8000-000000000001");

        assertEquals(3, page(callListing("dk", "da", Map.of())).getTotalItems());
    }

    // ------------------------------------------------------------ chi tiết

    @Test
    @DisplayName("Chi tiết trả đúng bảng con của loại sản phẩm")
    void detailReturnsProductTypeBlock() {
        ResponseEntity<GroupTourDetail> response = callDetail(
                "dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GroupTourDetail body = response.getBody();
        assertEquals("GROUP_TOUR", body.getProductType());
        assertEquals(12, body.getMinPax());
        assertEquals(10, body.getGuaranteedThreshold());
        assertEquals("da", body.getTourLeaderLanguage());
        assertEquals(2, body.getLongDescription().size());
        assertEquals("da", response.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));
        assertTrue(response.getHeaders().getVary().contains(HttpHeaders.ACCEPT_LANGUAGE));
    }

    @Test
    @DisplayName("Bộ ảnh: đúng thứ tự sort_order, URL ghép từ đường dẫn tương đối")
    void imageSetOrderedBySortOrder() {
        GroupTourDetail body = callDetail(
                "dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class).getBody();

        List<GalleryImage> image = body.getGallery();
        assertEquals(3, image.size(), "tấm xoá mềm phải bị loại");

        // Dữ liệu chèn theo thứ tự 3, 1, 2 — ra phải theo sort_order.
        assertEquals(List.of("Rismarker", "Lanterner", "Kun dansk"),
                image.stream().map(GalleryImage::getAlt).toList());

        // CSDL lưu `tour/mot.jpg`; địa chỉ gốc nằm ở cấu hình, không ở dữ liệu —
        // ADR-011 mục 2. Đây là chỗ bắt được nếu ai đó lưu URL đầy đủ vào CSDL.
        GalleryImage first = image.getFirst();
        assertTrue(first.getUrl().endsWith("/tour/mot.jpg"), first.getUrl());
        assertTrue(first.getUrl().startsWith("http"), first.getUrl());
        assertEquals(1400, first.getWidth());
        assertEquals(933, first.getHeight());
    }

    /**
     * Luật không fallback cho nội dung bán hàng, áp vào ảnh.
     *
     * <p>Tấm thứ ba chỉ có {@code alt} tiếng Đan. Ở locale {@code vi} nó phải
     * biến mất hẳn, <b>không</b> hiện kèm câu tiếng Đan — trình đọc màn hình sẽ
     * đọc một câu sai ngôn ngữ giữa trang tiếng Việt (docs/24 mục 6).
     */
    @Test
    @DisplayName("Ảnh thiếu alt ở locale nào thì biến mất khỏi locale đó")
    void imageMissingAltHiddenInThatLocale() {
        GroupTourDetail da = callDetail(
                "dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class).getBody();
        GroupTourDetail vi = callDetail(
                "dk", "vi", "viet-nam-tu-bac-vao-nam", GroupTourDetail.class).getBody();

        assertEquals(3, da.getGallery().size());
        assertEquals(2, vi.getGallery().size());
        assertEquals(List.of("Ruộng bậc thang", "Đèn lồng"),
                vi.getGallery().stream().map(GalleryImage::getAlt).toList());
    }

    /**
     * Sản phẩm chưa có ảnh nào là trạng thái hợp lệ, và template phải dựng được
     * khi rỗng. Trường vắng hẳn khỏi JSON vì {@code default-property-inclusion}
     * là {@code non_null} và danh sách rỗng bị bỏ — điều frontend đã phải xử lý
     * cho mọi trường tuỳ chọn khác.
     */
    @Test
    @DisplayName("Sản phẩm không có ảnh nào vẫn trả 200")
    void productWithoutImagesStillReturns200() {
        ResponseEntity<Object> response =
                callDetail("dk", "da", "aalborg-krydstogt", Object.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * {@code layout} chưa ai chọn thì vắng khỏi JSON, và frontend rơi về template
     * mặc định của loại. Đặt một giá trị bất kỳ phải trả về nguyên văn — cột là
     * chuỗi tự do có chủ ý, CSDL không cưỡng chế danh mục (docs/12 mục 4.1).
     */
    @Test
    @DisplayName("layout: vắng khi chưa chọn, trả nguyên văn khi đã chọn")
    void layoutAbsentUntilChosenThenVerbatim() {
        assertNull(callDetail("dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class)
                .getBody().getLayout());

        jdbc.update("UPDATE product SET layout = ? WHERE id = CAST(? AS uuid)",
                "tap-chi-image-lon", "c0000000-0000-4000-8000-000000000001");

        assertEquals("tap-chi-image-lon",
                callDetail("dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class)
                        .getBody().getLayout());
    }

    @Test
    @DisplayName("Slug phụ thuộc locale: slug tiếng Đan không mở được ở locale vi")
    void slugDependsOnLocale() {
        assertEquals(HttpStatus.NOT_FOUND,
                callDetail("dk", "vi", "vietnam-fra-nord-til-syd", ErrorResponse.class).getStatusCode());
        assertEquals(HttpStatus.OK,
                callDetail("dk", "vi", "viet-nam-tu-bac-vao-nam", GroupTourDetail.class).getStatusCode());
    }

    @Test
    @DisplayName("Sản phẩm chưa dịch trả 404 ở locale đó, không trả bản da")
    void untranslatedReturns404() {
        assertEquals(HttpStatus.OK,
                callDetail("dk", "da", "aalborg-krydstogt", Object.class).getStatusCode());

        ResponseEntity<ErrorResponse> vi = callDetail("dk", "vi", "aalborg-krydstogt", ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, vi.getStatusCode());
        assertEquals("NOT_FOUND", vi.getBody().getCode());
    }

    @Test
    @DisplayName("Sản phẩm chưa gán thị trường trả 404 ở thị trường đó")
    void productNotAssignedToMarketReturns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                callDetail("dk", "da", "skraeddersyet-rejse", ErrorResponse.class).getStatusCode());
        assertEquals(HttpStatus.OK,
                callDetail("vn", "da", "skraeddersyet-rejse", Object.class).getStatusCode());
    }

    // ------------------------------------------------------------ lỗi

    @Test
    @DisplayName("Ngôn ngữ không hỗ trợ trả 400 kèm mã, không lặng lẽ lùi về da")
    void unsupportedLanguageReturns400() {
        ResponseEntity<ErrorResponse> response = call("/api/v1/dk/products", "de", ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("UNSUPPORTED_LOCALE", response.getBody().getCode());
    }

    @Test
    @DisplayName("Thị trường không tồn tại hoặc đang tắt trả 404, không phải 400")
    void unknownOrDisabledMarketReturns404() {
        assertEquals(HttpStatus.NOT_FOUND,
                call("/api/v1/xx/products", "da", ErrorResponse.class).getStatusCode());

        jdbc.update("UPDATE market SET is_active = FALSE WHERE code = 'VN'");

        ResponseEntity<ErrorResponse> response = call("/api/v1/vn/products", "vi", ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getCode());
    }

    @Test
    @DisplayName("Tham số sai ràng buộc của spec trả 400 VALIDATION_FAILED")
    void invalidParamReturns400ValidationFailed() {
        ResponseEntity<ErrorResponse> qua = call("/api/v1/dk/products?size=999", "da", ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, qua.getStatusCode());
        assertEquals("VALIDATION_FAILED", qua.getBody().getCode());

        ResponseEntity<ErrorResponse> type = call(
                "/api/v1/dk/products?productType=KHONG_CO_LOAI_NAY", "da", ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, type.getStatusCode());
    }

    @Test
    @DisplayName("API không trả câu tiếng người ở bất kỳ mã lỗi nào")
    void apiNeverReturnsHumanSentences() {
        ResponseEntity<String> response = call("/api/v1/dk/products/khong-co-slug-nay", "da", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body.contains("NOT_FOUND"));
        assertFalse(body.toLowerCase().contains("not found for"),
                "Thân lỗi chỉ được chứa mã và tham số — câu chữ là việc của frontend");
    }

    // ------------------------------------------------------------ slug cũ

    @Test
    @DisplayName("Đổi slug thì slug cũ chuyển hướng sang slug mới")
    void oldSlugRedirectsToNewSlug() {
        // Trigger trg_luu_slug_cu ghi slug_history, không phải mã ứng dụng —
        // nên bài test này đổi slug bằng UPDATE thật, đúng đường mà trang quản
        // trị đi qua.
        jdbc.update("""
                UPDATE product_translation SET slug = 'vietnam-nord-syd-2027'
                WHERE product_id = 'c0000000-0000-4000-8000-000000000001' AND locale = 'da'
                """);

        ResponseEntity<SlugRedirect> response = call(
                "/api/v1/dk/redirects/PRODUCT/vietnam-fra-nord-til-syd", "da", SlugRedirect.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("vietnam-nord-syd-2027", response.getBody().getSlug());
    }

    @Test
    @DisplayName("KHÔNG chuyển hướng khi đích không xem được ở thị trường này")
    void noRedirectWhenTargetNotVisibleInMarket() {
        jdbc.update("""
                UPDATE product_translation SET slug = 'vietnam-nord-syd-2027'
                WHERE product_id = 'c0000000-0000-4000-8000-000000000001' AND locale = 'da'
                """);
        // Gỡ khỏi thị trường DK: slug cũ vẫn còn trong slug_history, nhưng slug
        // mới ở DK cũng trả 404. Chuyển hướng tới một trang 404 tệ hơn hẳn một
        // trang 404 thẳng — khách đi một vòng rồi vẫn không thấy gì.
        jdbc.update("""
                UPDATE product_market SET is_published = FALSE
                WHERE product_id = 'c0000000-0000-4000-8000-000000000001' AND market = 'DK'
                """);

        assertEquals(HttpStatus.NOT_FOUND, call(
                "/api/v1/dk/redirects/PRODUCT/vietnam-fra-nord-til-syd",
                "da", ErrorResponse.class).getStatusCode());
    }

    @Test
    @DisplayName("Slug hiện tại không phải slug cũ — trả 404, không tự trỏ về chính nó")
    void currentSlugIsNotAnOldSlug() {
        // Chuyển hướng một URL về chính nó là một vòng lặp; trình duyệt dừng lại
        // và báo lỗi, còn công cụ tìm kiếm bỏ trang đó.
        assertEquals(HttpStatus.NOT_FOUND, call(
                "/api/v1/dk/redirects/PRODUCT/vietnam-fra-nord-til-syd",
                "da", ErrorResponse.class).getStatusCode());
    }

    @Test
    @DisplayName("Slug cũ của locale này không dùng được ở locale kia")
    void oldSlugIsLocaleScoped() {
        jdbc.update("""
                UPDATE product_translation SET slug = 'viet-nam-2027'
                WHERE product_id = 'c0000000-0000-4000-8000-000000000001' AND locale = 'vi'
                """);

        // Slug cũ vừa sinh ra thuộc locale `vi`; hỏi bằng `da` thì không thấy.
        assertEquals(HttpStatus.NOT_FOUND, call(
                "/api/v1/dk/redirects/PRODUCT/viet-nam-tu-bac-vao-nam",
                "da", ErrorResponse.class).getStatusCode());

        assertEquals("viet-nam-2027", call(
                "/api/v1/vn/redirects/PRODUCT/viet-nam-tu-bac-vao-nam",
                "vi", SlugRedirect.class).getBody().getSlug());
    }

    // ------------------------------------------------------------ tiện ích

    /**
     * Tham số truy vấn đi qua {@code UriBuilder} chứ không nối chuỗi: nối tay
     * {@code "?q=hoi%20an"} rồi đưa cho {@code RestClient} thì phần trăm bị mã
     * hoá lần thứ hai và máy chủ nhận đúng chuỗi {@code "hoi%20an"} — tìm kiếm
     * ra 0 kết quả mà nhìn URL thì thấy đúng.
     */
    private ResponseEntity<ProductPage> callListing(String market, String locale, Map<String, String> params) {
        return client().get()
                .uri(b -> {
                    b.path("/api/v1/" + market + "/products");
                    params.forEach((name, value) -> b.queryParam(name, value));
                    return b.build();
                })
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(ProductPage.class);
    }

    private ProductPage page(ResponseEntity<ProductPage> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private <T> ResponseEntity<T> callDetail(String market, String locale, String slug, Class<T> type) {
        return call("/api/v1/" + market + "/products/" + slug, locale, type);
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
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build();
    }
}
