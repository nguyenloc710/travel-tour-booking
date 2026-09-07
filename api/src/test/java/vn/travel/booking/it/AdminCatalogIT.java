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
import vn.travel.booking.web.generated.model.AdminProductPage;
import vn.travel.booking.web.generated.model.AdminDestination;
import vn.travel.booking.web.generated.model.AdminProductSummary;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.TranslationCoverageRow;
import vn.travel.booking.web.generated.model.TranslationEntityType;
import vn.travel.booking.web.generated.model.TranslationGap;
import vn.travel.booking.web.generated.model.TranslationQueueItem;
import vn.travel.booking.web.generated.model.TranslationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Đợt 5a — ba màn hình đọc của trang quản trị: danh sách sản phẩm (M2), hàng đợi
 * dịch (M10), bảng độ phủ (M12).
 *
 * <p>Bài test có giá trị nhất trong lớp này là
 * {@link #danhSachThayCaThuBeMatCongKhaiGiauDi()}: nó kiểm đúng chỗ mà bề mặt
 * quản trị <b>phải</b> khác bề mặt khách. Nếu ai đó "dọn dẹp" bằng cách cho hai
 * bề mặt dùng chung một truy vấn thì bài này đỏ, còn mọi bài khác vẫn xanh.
 *
 * <h2>Bộ dữ liệu</h2>
 *
 * <pre>
 * P1 Việt Nam từ bắc vào nam  da PUBLISHED · vi dịch mới   · DK đang bán
 * P2 Halong krydstogt         da PUBLISHED · KHÔNG có vi   · DK đang bán, VN chưa bán
 * P3 Mekong flodtur           da PUBLISHED · vi ĐÃ CŨ      · chưa gán thị trường
 * P4 Kladde                   da DRAFT     · KHÔNG có vi   · chưa gán thị trường
 * B1 bài viết                 da PUBLISHED · KHÔNG có vi
 * </pre>
 *
 * <p>Bốn sản phẩm đủ để phân biệt bốn tình huống khác nhau, và P4 là cái quan
 * trọng nhất: bản nguồn còn nháp thì <b>chưa đến lượt dịch</b>, nên nó phải có
 * trong danh sách mà không có trong hàng đợi.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdminCatalogIT {

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

    private static final String MAT_KHAU = "mat-khau-rat-dai";
    private static final String P1 = "bb000000-0000-4000-8000-000000000001";
    private static final String P2 = "bb000000-0000-4000-8000-000000000002";
    private static final String P3 = "bb000000-0000-4000-8000-000000000003";
    private static final String P4 = "bb000000-0000-4000-8000-000000000004";

    @LocalServerPort
    int cong;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void chuanBiDuLieu() {
        // MỘT câu nhiều lệnh, không phải nhiều lần gọi: ràng buộc
        // ct_product_source_translation là DEFERRABLE INITIALLY DEFERRED, nên
        // product và bản dịch nguồn của nó phải nằm cùng một transaction.
        jdbc.execute("""
                DELETE FROM staff_user_role;
                DELETE FROM post_tag;
                DELETE FROM post_translation;
                DELETE FROM post;
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product_cruise;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM staff_user;

                INSERT INTO region (id, code, sort_order) VALUES
                  ('bb000000-0000-4000-8000-0000000000f1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('bb000000-0000-4000-8000-0000000000f1','da','nordvietnam','Nordvietnam');
                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('bb000000-0000-4000-8000-0000000000f2',
                   'bb000000-0000-4000-8000-0000000000f1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('bb000000-0000-4000-8000-0000000000f2','da','hanoi','Hanoi');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES
                  ('bb000000-0000-4000-8000-000000000001','GROUP_TOUR','bb000000-0000-4000-8000-0000000000f2',14,'/img/1.jpg'),
                  ('bb000000-0000-4000-8000-000000000002','CRUISE','bb000000-0000-4000-8000-0000000000f2',3,'/img/2.jpg'),
                  ('bb000000-0000-4000-8000-000000000003','GROUP_TOUR','bb000000-0000-4000-8000-0000000000f2',9,'/img/3.jpg'),
                  ('bb000000-0000-4000-8000-000000000004','GROUP_TOUR','bb000000-0000-4000-8000-0000000000f2',7,'/img/4.jpg');

                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES
                  ('bb000000-0000-4000-8000-000000000001',12,20,12,'da',2),
                  ('bb000000-0000-4000-8000-000000000003',10,18,10,'da',2),
                  ('bb000000-0000-4000-8000-000000000004',10,16,10,'da',1);
                INSERT INTO product_cruise (product_id, ship_name, port_count)
                VALUES ('bb000000-0000-4000-8000-000000000002','Bhaya',4);

                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status, translated_at)
                VALUES
                  ('bb000000-0000-4000-8000-000000000001','da','nord-til-syd','Nord til syd',
                   'Hele landet.', ARRAY['Et.','To.'], ARRAY['A','B','C'],'Rismarker','PUBLISHED', NULL),
                  ('bb000000-0000-4000-8000-000000000001','vi','viet-nam-tu-bac-vao-nam','Việt Nam từ bắc vào nam',
                   'Cả nước.', ARRAY['Một.','Hai.'], ARRAY['A','B','C'],'Ruộng bậc thang','PUBLISHED',
                   now() + interval '1 hour'),

                  ('bb000000-0000-4000-8000-000000000002','da','halong-krydstogt','Halong krydstogt',
                   'To naetter.', ARRAY['Et.','To.'], ARRAY['A','B','C'],'Skib','PUBLISHED', NULL),

                  ('bb000000-0000-4000-8000-000000000003','da','mekong-flodtur','Mekong flodtur',
                   'Deltaet.', ARRAY['Et.','To.'], ARRAY['A','B','C'],'Flod','PUBLISHED', NULL),
                  ('bb000000-0000-4000-8000-000000000003','vi','song-mekong','Sông Mekong',
                   'Đồng bằng.', ARRAY['Một.','Hai.'], ARRAY['A','B','C'],'Sông','TRANSLATED',
                   now() - interval '1 day'),

                  ('bb000000-0000-4000-8000-000000000004','da','kladde','Kladde',
                   'Ikke faerdig.', ARRAY['Et.','To.'], ARRAY['A','B','C'],'Ingen','DRAFT', NULL);

                INSERT INTO product_market (product_id, market, is_published) VALUES
                  ('bb000000-0000-4000-8000-000000000001','DK',TRUE),
                  ('bb000000-0000-4000-8000-000000000002','DK',TRUE),
                  ('bb000000-0000-4000-8000-000000000002','VN',FALSE);

                INSERT INTO post (id, hero_image) VALUES
                  ('bb000000-0000-4000-8000-0000000000b1','/img/b1.jpg');
                INSERT INTO post_translation (post_id, locale, slug, title, excerpt, body, status, translated_at)
                VALUES ('bb000000-0000-4000-8000-0000000000b1','da','rejseraad','Rejseraad',
                        'Gode raad.', ARRAY['Et afsnit.'],'PUBLISHED', NULL);
                """);

        String bam = new BCryptPasswordEncoder().encode(MAT_KHAU);
        themNhanVien("bb100000-0000-4000-8000-000000000001", "editor@travel.test", "Biên tập", bam, "EDITOR");
        themNhanVien("bb100000-0000-4000-8000-000000000002", "tuvan@travel.test", "Tư vấn", bam, "CONSULTANT");
    }

    // ------------------------------------------------------------ M2

    @Test
    @DisplayName("Danh sách quản trị thấy cả thứ mà bề mặt công khai giấu đi")
    void danhSachThayCaThuBeMatCongKhaiGiauDi() {
        AdminProductPage trang = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/products", AdminProductPage.class).getBody();

        assertNotNull(trang);
        assertEquals(4L, trang.getTotalItems());

        // P4: bản nguồn còn DRAFT. Bề mặt công khai đòi status = 'PUBLISHED' nên
        // nó không tồn tại ở đó; ở đây nó phải thấy, vì đó chính là việc đang dở.
        AdminProductSummary p4 = tim(trang, "Kladde");
        assertEquals(TranslationStatus.DRAFT, p4.getSourceStatus());

        // P3: chưa gán thị trường nào. Bề mặt công khai đòi pm.is_published nên
        // nó cũng không tồn tại ở đó — mà chưa thấy thì không ai gán được cho nó.
        AdminProductSummary p3 = tim(trang, "Mekong flodtur");
        assertTrue(p3.getMarkets().isEmpty());

        // Mọi bản dịch, không phải một: màn hình dịch song song cần cả hai.
        assertEquals(2, p3.getTranslations().size());
        assertTrue(p3.getTranslations().get(0).getIsSource(), "bản nguồn phải đứng trước");
        assertTrue(p3.getTranslations().get(1).getOutdated(), "bản vi dịch trước khi nguồn sửa");
        assertFalse(p3.getTranslations().get(0).getOutdated(), "bản nguồn không dịch từ đâu cả");
    }

    @Test
    @DisplayName("Lọc khoảng trống: MISSING ra sản phẩm chưa có bản dịch nào")
    void locKhoangTrongThieuBanDich() {
        AdminProductPage trang = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/products?gap=MISSING", AdminProductPage.class).getBody();

        assertNotNull(trang);
        assertEquals(2L, trang.getTotalItems());
        List<String> ten = trang.getItems().stream().map(AdminProductSummary::getSourceTitle).toList();
        assertTrue(ten.contains("Halong krydstogt"));
        assertTrue(ten.contains("Kladde"));
    }

    @Test
    @DisplayName("Lọc khoảng trống: OUTDATED ra sản phẩm có bản dịch nhưng nguồn sửa sau")
    void locKhoangTrongQuaHan() {
        AdminProductPage trang = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/products?gap=OUTDATED", AdminProductPage.class).getBody();

        assertNotNull(trang);
        assertEquals(1L, trang.getTotalItems());
        assertEquals("Mekong flodtur", trang.getItems().get(0).getSourceTitle());
    }

    @Test
    @DisplayName("Tìm kiếm khử dấu, và tìm trong tiêu đề của MỌI locale")
    void timKiemKhuDauTrenMoiLocale() {
        // "bac" không dấu, mà chuỗi khớp lại nằm ở bản `vi` ("từ bắc vào nam")
        // chứ không phải bản nguồn. Nhân viên không nhớ tour này nhập bằng tiếng nào.
        //
        // Dấu cách viết THẲNG, không viết %20: RestClient coi chuỗi truyền vào
        // uri() là mẫu URI và mã hoá lại lần nữa, nên "%20" tới máy chủ thành
        // "%2520" và không khớp gì cả.
        AdminProductPage trang = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/products?q=bac vao", AdminProductPage.class).getBody();

        assertNotNull(trang);
        assertEquals(1L, trang.getTotalItems());
        assertEquals("Nord til syd", trang.getItems().get(0).getSourceTitle());
    }

    @Test
    @DisplayName("Lọc theo loại sản phẩm và theo thị trường đã gán")
    void locTheoLoaiVaThiTruong() {
        Phien phien = dangNhap("editor@travel.test");

        AdminProductPage theoLoai = phien
                .lay("/api/v1/admin/products?productType=CRUISE", AdminProductPage.class).getBody();
        assertNotNull(theoLoai);
        assertEquals(1L, theoLoai.getTotalItems());

        // VN chỉ có P2, và nó CHƯA bán ở VN. Bộ lọc hỏi "đã gán chưa", không hỏi
        // "đang bán chưa" — nhân viên phải tìm được đúng thứ đang chờ bật công tắc.
        AdminProductPage theoThiTruong = phien
                .lay("/api/v1/admin/products?market=VN", AdminProductPage.class).getBody();
        assertNotNull(theoThiTruong);
        assertEquals(1L, theoThiTruong.getTotalItems());
        assertEquals("Halong krydstogt", theoThiTruong.getItems().get(0).getSourceTitle());
        assertFalse(theoThiTruong.getItems().get(0).getMarkets().stream()
                .filter(m -> m.getMarket().getValue().equals("VN"))
                .findFirst().orElseThrow().getPublished());
    }

    @Test
    @DisplayName("Phân trang phía máy chủ")
    void phanTrangPhiaMayChu() {
        AdminProductPage trang = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/products?size=2", AdminProductPage.class).getBody();

        assertNotNull(trang);
        assertEquals(2, trang.getItems().size());
        assertEquals(4L, trang.getTotalItems());
        assertEquals(2, trang.getTotalPages());
    }

    @Test
    @DisplayName("Điểm đến trả id chứ không trả slug — bề mặt quản trị dùng id")
    void diemDenTraId() {
        AdminDestination[] ds = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/destinations", AdminDestination[].class).getBody();

        assertNotNull(ds);
        assertEquals(1, ds.length);
        // Bề mặt công khai trả slug vì URL của khách dùng slug; tạo sản phẩm thì
        // cần id, và slug đổi được còn id thì không.
        assertNotNull(ds[0].getId());
        assertEquals("HANOI", ds[0].getCode());
        // Tên ở NGÔN NGỮ NGUỒN, không phải ngôn ngữ giao diện quản trị.
        assertEquals("Hanoi", ds[0].getName());
        assertEquals("Nordvietnam", ds[0].getRegionName());
    }

    // ------------------------------------------------------------ M10

    @Test
    @DisplayName("Hàng đợi xếp tour đang bán trước tour chưa gán, bài viết sau cùng")
    void hangDoiXepTheoTacDongDoanhThu() {
        TranslationQueueItem[] hangDoi = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/translations/queue", TranslationQueueItem[].class).getBody();

        assertNotNull(hangDoi);
        assertEquals(3, hangDoi.length);

        assertEquals("Halong krydstogt", hangDoi[0].getSourceTitle());
        assertEquals(1, hangDoi[0].getPriority());
        assertEquals(TranslationGap.MISSING, hangDoi[0].getGap());
        assertNull(hangDoi[0].getTranslatedAt(), "chưa dịch lần nào thì không có mốc");

        assertEquals("Mekong flodtur", hangDoi[1].getSourceTitle());
        assertEquals(2, hangDoi[1].getPriority());
        assertEquals(TranslationGap.OUTDATED, hangDoi[1].getGap());
        assertNotNull(hangDoi[1].getTranslatedAt());

        assertEquals(TranslationEntityType.POST, hangDoi[2].getEntityType());
        assertEquals(4, hangDoi[2].getPriority());

        // P1 đã dịch xong và còn hạn; P4 bản nguồn còn DRAFT nên chưa đến lượt.
        List<String> ten = List.of(hangDoi).stream()
                .map(TranslationQueueItem::getSourceTitle).toList();
        assertFalse(ten.contains("Nord til syd"));
        assertFalse(ten.contains("Kladde"));

        assertEquals("vi", hangDoi[0].getLocale(), "locale ĐÍCH, không phải locale nguồn");
    }

    @Test
    @DisplayName("Hàng đợi lọc được theo loại thực thể")
    void hangDoiLocTheoLoaiThucThe() {
        TranslationQueueItem[] hangDoi = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/translations/queue?entityType=POST", TranslationQueueItem[].class)
                .getBody();

        assertNotNull(hangDoi);
        assertEquals(1, hangDoi.length);
        assertEquals(TranslationEntityType.POST, hangDoi[0].getEntityType());
    }

    // ------------------------------------------------------------ M12

    @Test
    @DisplayName("Độ phủ tách hai con số: đã dịch và còn hạn")
    void doPhuTachHaiConSo() {
        TranslationCoverageRow[] bang = dangNhap("editor@travel.test")
                .lay("/api/v1/admin/translations/coverage", TranslationCoverageRow[].class).getBody();

        assertNotNull(bang);

        TranslationCoverageRow sanPham = List.of(bang).stream()
                .filter(r -> r.getEntityType() == TranslationEntityType.PRODUCT)
                .findFirst().orElseThrow();

        // Mẫu số là 3 chứ không phải 4: P4 còn DRAFT ở bản nguồn nên chưa đến
        // lượt dịch. Đưa nó vào mẫu số là làm độ phủ tụt vì một việc chưa ai
        // được phép làm.
        assertEquals(3, sanPham.getTotal());
        assertEquals(2, sanPham.getTranslated(), "P1 và P3 đều có translated_at");
        assertEquals(1, sanPham.getUpToDate(), "chỉ P1 còn hạn — P3 đã trôi");
        assertEquals("vi", sanPham.getLocale());

        TranslationCoverageRow baiViet = List.of(bang).stream()
                .filter(r -> r.getEntityType() == TranslationEntityType.POST)
                .findFirst().orElseThrow();
        assertEquals(1, baiViet.getTotal());
        assertEquals(0, baiViet.getTranslated());
    }

    // ------------------------------------------------------------ quyền và cache

    @Test
    @DisplayName("Chưa đăng nhập thì không vào được bề mặt quản trị")
    void chuaDangNhapThiKhongVao() {
        ResponseEntity<ErrorResponse> phanHoi = new Phien()
                .lay("/api/v1/admin/products", ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, phanHoi.getStatusCode());
    }

    @Test
    @DisplayName("Tư vấn viên đọc được danh sách và hàng đợi — ma trận quyền docs/22 mục 2.1")
    void tuVanVienDocDuoc() {
        Phien phien = dangNhap("tuvan@travel.test");

        assertEquals(HttpStatus.OK,
                phien.lay("/api/v1/admin/products", AdminProductPage.class).getStatusCode());
        assertEquals(HttpStatus.OK,
                phien.lay("/api/v1/admin/translations/queue", TranslationQueueItem[].class).getStatusCode());
    }

    @Test
    @DisplayName("Mọi phản hồi quản trị là no-store")
    void moiPhanHoiQuanTriLaNoStore() {
        Phien phien = dangNhap("editor@travel.test");

        for (String duongDan : List.of("/api/v1/admin/products",
                "/api/v1/admin/translations/queue",
                "/api/v1/admin/translations/coverage")) {
            String cache = phien.lay(duongDan, String.class)
                    .getHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
            assertTrue(cache != null && cache.contains("no-store"),
                    duongDan + " phải là no-store — nội dung chưa xuất bản không được nằm trong cache nào");
        }
    }

    // ------------------------------------------------------------ tiện ích

    private static AdminProductSummary tim(AdminProductPage trang, String tieuDeNguon) {
        return trang.getItems().stream()
                .filter(x -> tieuDeNguon.equals(x.getSourceTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("không thấy " + tieuDeNguon));
    }

    private void themNhanVien(String id, String email, String ten, String bam, String vaiTro) {
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash)
                VALUES (CAST(? AS uuid), ?, ?, ?)
                """, id, email, ten, bam);
        jdbc.update("""
                INSERT INTO staff_user_role (id, staff_user_id, role_code)
                VALUES (gen_random_uuid(), CAST(? AS uuid), ?)
                """, id, vaiTro);
    }

    private Phien dangNhap(String email) {
        Phien phien = new Phien();
        assertEquals(HttpStatus.NO_CONTENT, phien.dangNhap(email, MAT_KHAU).getStatusCode());
        return phien;
    }

    /** Giữ cookie phiên giữa các lời gọi — đúng như trình duyệt làm. */
    private final class Phien {

        private final List<String> cookies = new ArrayList<>();

        ResponseEntity<String> dangNhap(String email, String matKhau) {
            return goi(HttpMethod.POST, "/api/v1/admin/session",
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, matKhau),
                    String.class);
        }

        <T> ResponseEntity<T> lay(String duongDan, Class<T> kieu) {
            return goi(HttpMethod.GET, duongDan, null, kieu);
        }

        <T> ResponseEntity<T> goi(HttpMethod phuongThuc, String duongDan, String than, Class<T> kieu) {
            RestClient.RequestBodySpec yeuCau = RestClient.builder()
                    .baseUrl("http://localhost:" + cong)
                    .defaultStatusHandler(status -> true, (req, res) -> { })
                    .build()
                    .method(phuongThuc)
                    .uri(duongDan);

            for (String c : cookies) {
                yeuCau.header(HttpHeaders.COOKIE, c);
            }
            thecCsrf().ifPresent(t -> yeuCau.header("X-XSRF-TOKEN", t));

            if (than != null) {
                yeuCau.contentType(MediaType.APPLICATION_JSON).body(than);
            }

            ResponseEntity<T> phanHoi = yeuCau.retrieve().toEntity(kieu);
            nhoCookie(phanHoi);
            return phanHoi;
        }

        private void nhoCookie(ResponseEntity<?> phanHoi) {
            List<String> moi = phanHoi.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (moi == null) {
                return;
            }
            for (String c : moi) {
                String rutGon = c.split(";", 2)[0];
                String ten = rutGon.split("=", 2)[0];
                cookies.removeIf(cu -> cu.startsWith(ten + "="));
                cookies.add(rutGon);
            }
        }

        private Optional<String> thecCsrf() {
            return cookies.stream()
                    .filter(c -> c.startsWith("XSRF-TOKEN="))
                    .map(c -> c.substring("XSRF-TOKEN=".length()))
                    .findFirst();
        }
    }
}
