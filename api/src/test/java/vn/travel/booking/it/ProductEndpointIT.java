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
    int cong;

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
    void chuanBiDuLieu() {
        jdbc.execute("""
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
                  ('c0000000-0000-4000-8000-000000000003','vi','hoi-an-ve-dem',
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
    void listing_dk_da() {
        ProductPage trang = trang(goiListing("dk", "da", Map.of()));

        assertEquals(4, trang.getTotalItems(),
                "P4 chỉ xuất bản ở VN nên không được lọt vào thị trường DK");
        assertTrue(trang.getItems().stream().noneMatch(p -> "Skræddersyet rejse".equals(p.getTitle())));
    }

    @Test
    @DisplayName("Sản phẩm thiếu bản dịch vi BIẾN MẤT khỏi listing vi — không hiện bản da")
    void khongFallbackNoiDungBanHang() {
        ProductPage da = trang(goiListing("dk", "da", Map.of()));
        ProductPage vi = trang(goiListing("dk", "vi", Map.of()));

        assertEquals(4, da.getTotalItems());
        assertEquals(2, vi.getTotalItems(),
                "Chỉ P1 và P3 có bản vi; P2 và P5 chỉ có bản da nên phải biến mất");

        List<String> tenVi = vi.getItems().stream().map(ProductSummary::getTitle).toList();
        assertTrue(tenVi.stream().noneMatch(t -> t.contains("Ålborg")),
                "Hiện bản tiếng Đan thay thế là phá chính sách của docs/02 mục 4");

        // Đây chính là lý do "Xem tất cả N tour" không được hardcode: cùng một
        // thị trường, hai locale ra hai con số khác nhau.
        assertTrue(da.getTotalItems() != vi.getTotalItems());
    }

    @Test
    @DisplayName("Sắp theo tiêu đề dùng collation Đan Mạch: Aa và Å xếp SAU z")
    void sapXepTheoCollationDanMach() {
        List<String> ten = trang(goiListing("dk", "da", Map.of("sort", "title,asc")))
                .getItems().stream().map(ProductSummary::getTitle).toList();

        assertEquals(List.of(
                        "Hoi An efter mørkets frembrud",
                        "Vietnam fra nord til syd",
                        "Ålborg-gruppens krydstogt",
                        "Aarhus kulturtur"),
                ten,
                "String.compareTo() của Java xếp Aarhus lên đầu — sai với người Đan Mạch");
    }

    @Test
    @DisplayName("Tìm không dấu: gõ hoi an ra Hội An")
    void timKhongDau() {
        ProductPage kq = trang(goiListing("dk", "vi", Map.of("q", "hoi an")));

        assertEquals(1, kq.getTotalItems());
        assertEquals("Hội An về đêm", kq.getItems().get(0).getTitle());
    }

    @Test
    @DisplayName("Lọc theo miền dùng slug CỦA LOCALE ĐANG XEM, không dùng mã miền")
    void locTheoMien() {
        assertEquals(1, trang(goiListing("dk", "da", Map.of("region", "nordvietnam"))).getTotalItems());
        assertEquals(1, trang(goiListing("dk", "vi", Map.of("region", "mien-bac"))).getTotalItems());
        assertEquals(0, trang(goiListing("dk", "vi", Map.of("region", "nordvietnam"))).getTotalItems(),
                "Slug tiếng Đan không được dùng ở locale vi");
    }

    @Test
    @DisplayName("Lọc theo loại sản phẩm")
    void locTheoLoai() {
        ProductPage kq = trang(goiListing("dk", "da", Map.of("productType", "CRUISE")));
        assertEquals(1, kq.getTotalItems());
        assertEquals("Ålborg-gruppens krydstogt", kq.getItems().get(0).getTitle());
    }

    @Test
    @DisplayName("Phân trang theo offset, tổng số đếm từ dữ liệu")
    void phanTrang() {
        ProductPage t0 = trang(goiListing("dk", "da", Map.of("size", "2", "page", "0")));
        ProductPage t1 = trang(goiListing("dk", "da", Map.of("size", "2", "page", "1")));
        ProductPage t2 = trang(goiListing("dk", "da", Map.of("size", "2", "page", "2")));

        assertEquals(4, t0.getTotalItems());
        assertEquals(2, t0.getTotalPages());
        assertEquals(2, t0.getItems().size());
        assertEquals(2, t1.getItems().size());
        assertTrue(t2.getItems().isEmpty(), "Trang vượt quá tổng số trả rỗng, không lỗi");

        assertTrue(t0.getItems().stream().noneMatch(a ->
                        t1.getItems().stream().anyMatch(b -> b.getSlug().equals(a.getSlug()))),
                "Thiếu tiêu chí sắp xếp phụ thì phân trang offset lặp bản ghi");
    }

    @Test
    @DisplayName("Giá làm tròn theo số chữ số thập phân của THỊ TRƯỜNG: DKK 2, VND 0")
    void giaTheoThiTruong() {
        ProductSummary dk = trang(goiListing("dk", "da", Map.of("q", "Vietnam"))).getItems().get(0);
        ProductSummary vn = trang(goiListing("vn", "vi", Map.of("q", "Viet"))).getItems().get(0);

        assertEquals("24990.00", dk.getPriceFrom().getAmount());
        assertEquals("DKK", dk.getPriceFrom().getCurrency());

        // Cùng một tour, giá của hai thị trường là hai con số do người nhập —
        // không phải kết quả nhân tỷ giá. Hệ thống này không có tỷ giá.
        assertEquals("18900000", vn.getPriceFrom().getAmount());
        assertEquals("VND", vn.getPriceFrom().getCurrency());
    }

    @Test
    @DisplayName("Chưa có giá thì bỏ hẳn trường priceFrom, không trả 0")
    void chuaCoGia() {
        ProductSummary p3 = trang(goiListing("dk", "da", Map.of("q", "Hoi An"))).getItems().get(0);
        assertNull(p3.getPriceFrom(), "Trả 0 là nói với khách rằng tour này miễn phí");
    }

    @Test
    @DisplayName("Sản phẩm xoá mềm biến mất khỏi listing")
    void xoaMem() {
        // CAST tường minh: cột là UUID, tham số JDBC là chuỗi, và Postgres
        // không tự ép — "operator does not exist: uuid = character varying".
        jdbc.update("UPDATE product SET soft_delete = TRUE WHERE id = CAST(? AS uuid)",
                "c0000000-0000-4000-8000-000000000001");

        assertEquals(3, trang(goiListing("dk", "da", Map.of())).getTotalItems());
    }

    // ------------------------------------------------------------ chi tiết

    @Test
    @DisplayName("Chi tiết trả đúng bảng con của loại sản phẩm")
    void chiTietGroupTour() {
        ResponseEntity<GroupTourDetail> phanHoi = goiChiTiet(
                "dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class);

        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
        GroupTourDetail than = phanHoi.getBody();
        assertEquals("GROUP_TOUR", than.getProductType());
        assertEquals(12, than.getMinPax());
        assertEquals(10, than.getGuaranteedThreshold());
        assertEquals("da", than.getTourLeaderLanguage());
        assertEquals(2, than.getLongDescription().size());
        assertEquals("da", phanHoi.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));
        assertTrue(phanHoi.getHeaders().getVary().contains(HttpHeaders.ACCEPT_LANGUAGE));
    }

    @Test
    @DisplayName("Bộ ảnh: đúng thứ tự sort_order, URL ghép từ đường dẫn tương đối")
    void boAnhTheoThuTu() {
        GroupTourDetail than = goiChiTiet(
                "dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class).getBody();

        List<GalleryImage> anh = than.getGallery();
        assertEquals(3, anh.size(), "tấm xoá mềm phải bị loại");

        // Dữ liệu chèn theo thứ tự 3, 1, 2 — ra phải theo sort_order.
        assertEquals(List.of("Rismarker", "Lanterner", "Kun dansk"),
                anh.stream().map(GalleryImage::getAlt).toList());

        // CSDL lưu `tour/mot.jpg`; địa chỉ gốc nằm ở cấu hình, không ở dữ liệu —
        // ADR-011 mục 2. Đây là chỗ bắt được nếu ai đó lưu URL đầy đủ vào CSDL.
        GalleryImage dau = anh.getFirst();
        assertTrue(dau.getUrl().endsWith("/tour/mot.jpg"), dau.getUrl());
        assertTrue(dau.getUrl().startsWith("http"), dau.getUrl());
        assertEquals(1400, dau.getWidth());
        assertEquals(933, dau.getHeight());
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
    void anhThieuAltThiAn() {
        GroupTourDetail da = goiChiTiet(
                "dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class).getBody();
        GroupTourDetail vi = goiChiTiet(
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
    void khongCoAnhVanTra200() {
        ResponseEntity<Object> phanHoi =
                goiChiTiet("dk", "da", "aalborg-krydstogt", Object.class);

        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
    }

    /**
     * {@code layout} chưa ai chọn thì vắng khỏi JSON, và frontend rơi về template
     * mặc định của loại. Đặt một giá trị bất kỳ phải trả về nguyên văn — cột là
     * chuỗi tự do có chủ ý, CSDL không cưỡng chế danh mục (docs/12 mục 4.1).
     */
    @Test
    @DisplayName("layout: vắng khi chưa chọn, trả nguyên văn khi đã chọn")
    void layoutTraNguyenVan() {
        assertNull(goiChiTiet("dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class)
                .getBody().getLayout());

        jdbc.update("UPDATE product SET layout = ? WHERE id = CAST(? AS uuid)",
                "tap-chi-anh-lon", "c0000000-0000-4000-8000-000000000001");

        assertEquals("tap-chi-anh-lon",
                goiChiTiet("dk", "da", "vietnam-fra-nord-til-syd", GroupTourDetail.class)
                        .getBody().getLayout());
    }

    @Test
    @DisplayName("Slug phụ thuộc locale: slug tiếng Đan không mở được ở locale vi")
    void slugPhuThuocLocale() {
        assertEquals(HttpStatus.NOT_FOUND,
                goiChiTiet("dk", "vi", "vietnam-fra-nord-til-syd", ErrorResponse.class).getStatusCode());
        assertEquals(HttpStatus.OK,
                goiChiTiet("dk", "vi", "viet-nam-tu-bac-vao-nam", GroupTourDetail.class).getStatusCode());
    }

    @Test
    @DisplayName("Sản phẩm chưa dịch trả 404 ở locale đó, không trả bản da")
    void chuaDichTra404() {
        assertEquals(HttpStatus.OK,
                goiChiTiet("dk", "da", "aalborg-krydstogt", Object.class).getStatusCode());

        ResponseEntity<ErrorResponse> vi = goiChiTiet("dk", "vi", "aalborg-krydstogt", ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, vi.getStatusCode());
        assertEquals("NOT_FOUND", vi.getBody().getCode());
    }

    @Test
    @DisplayName("Sản phẩm chưa gán thị trường trả 404 ở thị trường đó")
    void chuaGanThiTruongTra404() {
        assertEquals(HttpStatus.NOT_FOUND,
                goiChiTiet("dk", "da", "skraeddersyet-rejse", ErrorResponse.class).getStatusCode());
        assertEquals(HttpStatus.OK,
                goiChiTiet("vn", "da", "skraeddersyet-rejse", Object.class).getStatusCode());
    }

    // ------------------------------------------------------------ lỗi

    @Test
    @DisplayName("Ngôn ngữ không hỗ trợ trả 400 kèm mã, không lặng lẽ lùi về da")
    void ngonNguKhongHoTro() {
        ResponseEntity<ErrorResponse> phanHoi = goi("/api/v1/dk/products", "de", ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, phanHoi.getStatusCode());
        assertEquals("UNSUPPORTED_LOCALE", phanHoi.getBody().getCode());
    }

    @Test
    @DisplayName("Thị trường không tồn tại hoặc đang tắt trả 404, không phải 400")
    void thiTruongKhongBat() {
        assertEquals(HttpStatus.NOT_FOUND,
                goi("/api/v1/xx/products", "da", ErrorResponse.class).getStatusCode());

        jdbc.update("UPDATE market SET is_active = FALSE WHERE code = 'VN'");

        ResponseEntity<ErrorResponse> phanHoi = goi("/api/v1/vn/products", "vi", ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, phanHoi.getStatusCode());
        assertEquals("NOT_FOUND", phanHoi.getBody().getCode());
    }

    @Test
    @DisplayName("Tham số sai ràng buộc của spec trả 400 VALIDATION_FAILED")
    void thamSoSai() {
        ResponseEntity<ErrorResponse> qua = goi("/api/v1/dk/products?size=999", "da", ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, qua.getStatusCode());
        assertEquals("VALIDATION_FAILED", qua.getBody().getCode());

        ResponseEntity<ErrorResponse> loai = goi(
                "/api/v1/dk/products?productType=KHONG_CO_LOAI_NAY", "da", ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, loai.getStatusCode());
    }

    @Test
    @DisplayName("API không trả câu tiếng người ở bất kỳ mã lỗi nào")
    void khongTraCauTiengNguoi() {
        ResponseEntity<String> phanHoi = goi("/api/v1/dk/products/khong-co-slug-nay", "da", String.class);

        assertEquals(HttpStatus.NOT_FOUND, phanHoi.getStatusCode());
        String than = phanHoi.getBody();
        assertTrue(than.contains("NOT_FOUND"));
        assertFalse(than.toLowerCase().contains("not found for"),
                "Thân lỗi chỉ được chứa mã và tham số — câu chữ là việc của frontend");
    }

    // ------------------------------------------------------------ slug cũ

    @Test
    @DisplayName("Đổi slug thì slug cũ chuyển hướng sang slug mới")
    void slugCuTroSangSlugMoi() {
        // Trigger trg_luu_slug_cu ghi slug_history, không phải mã ứng dụng —
        // nên bài test này đổi slug bằng UPDATE thật, đúng đường mà trang quản
        // trị đi qua.
        jdbc.update("""
                UPDATE product_translation SET slug = 'vietnam-nord-syd-2027'
                WHERE product_id = 'c0000000-0000-4000-8000-000000000001' AND locale = 'da'
                """);

        ResponseEntity<SlugRedirect> phanHoi = goi(
                "/api/v1/dk/redirects/PRODUCT/vietnam-fra-nord-til-syd", "da", SlugRedirect.class);

        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
        assertEquals("vietnam-nord-syd-2027", phanHoi.getBody().getSlug());
    }

    @Test
    @DisplayName("KHÔNG chuyển hướng khi đích không xem được ở thị trường này")
    void khongChuyenHuongToiNgoCut() {
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

        assertEquals(HttpStatus.NOT_FOUND, goi(
                "/api/v1/dk/redirects/PRODUCT/vietnam-fra-nord-til-syd",
                "da", ErrorResponse.class).getStatusCode());
    }

    @Test
    @DisplayName("Slug hiện tại không phải slug cũ — trả 404, không tự trỏ về chính nó")
    void slugHienTaiKhongPhaiSlugCu() {
        // Chuyển hướng một URL về chính nó là một vòng lặp; trình duyệt dừng lại
        // và báo lỗi, còn công cụ tìm kiếm bỏ trang đó.
        assertEquals(HttpStatus.NOT_FOUND, goi(
                "/api/v1/dk/redirects/PRODUCT/vietnam-fra-nord-til-syd",
                "da", ErrorResponse.class).getStatusCode());
    }

    @Test
    @DisplayName("Slug cũ của locale này không dùng được ở locale kia")
    void slugCuPhuThuocLocale() {
        jdbc.update("""
                UPDATE product_translation SET slug = 'viet-nam-2027'
                WHERE product_id = 'c0000000-0000-4000-8000-000000000001' AND locale = 'vi'
                """);

        // Slug cũ vừa sinh ra thuộc locale `vi`; hỏi bằng `da` thì không thấy.
        assertEquals(HttpStatus.NOT_FOUND, goi(
                "/api/v1/dk/redirects/PRODUCT/viet-nam-tu-bac-vao-nam",
                "da", ErrorResponse.class).getStatusCode());

        assertEquals("viet-nam-2027", goi(
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
    private ResponseEntity<ProductPage> goiListing(String market, String locale, Map<String, String> thamSo) {
        return client().get()
                .uri(b -> {
                    b.path("/api/v1/" + market + "/products");
                    thamSo.forEach((ten, gia_tri) -> b.queryParam(ten, gia_tri));
                    return b.build();
                })
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(ProductPage.class);
    }

    private ProductPage trang(ResponseEntity<ProductPage> phanHoi) {
        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
        return phanHoi.getBody();
    }

    private <T> ResponseEntity<T> goiChiTiet(String market, String locale, String slug, Class<T> kieu) {
        return goi("/api/v1/" + market + "/products/" + slug, locale, kieu);
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
