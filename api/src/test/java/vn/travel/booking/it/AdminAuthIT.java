package vn.travel.booking.it;

import vn.travel.booking.common.entity.BaseEntity;
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
import vn.travel.booking.web.generated.model.AdminProductTranslation;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.StaffProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Đợt 2 — phiên đăng nhập, ma trận quyền, và đường ghi đầu tiên có kiểm toán.
 *
 * <p>Tiêu chí ra của đợt này (docs/15 mục 4): một tài khoản {@code TRANSLATOR}
 * sửa được bản {@code vi} và <b>bị từ chối</b> khi sửa bản {@code da}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdminAuthIT {

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

    private static final String SAN_PHAM = "aa000000-0000-4000-8000-000000000001";
    private static final String MAT_KHAU = "mat-khau-rat-dai";

    @LocalServerPort
    int cong;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void chuanBiDuLieu() {
        String bam = new BCryptPasswordEncoder().encode(MAT_KHAU);

        jdbc.execute("""
                DELETE FROM staff_user_role;
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM staff_user;

                INSERT INTO region (id, code, sort_order) VALUES
                  ('aa000000-0000-4000-8000-0000000000f1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('aa000000-0000-4000-8000-0000000000f1','da','nordvietnam','Nordvietnam');
                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('aa000000-0000-4000-8000-0000000000f2',
                   'aa000000-0000-4000-8000-0000000000f1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('aa000000-0000-4000-8000-0000000000f2','da','hanoi','Hanoi');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('aa000000-0000-4000-8000-000000000001','GROUP_TOUR',
                        'aa000000-0000-4000-8000-0000000000f2',14,'/img/p.jpg');
                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES ('aa000000-0000-4000-8000-000000000001',12,20,12,'da',2);
                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status)
                VALUES ('aa000000-0000-4000-8000-000000000001','da','nord-til-syd','Nord til syd',
                        'Hele landet.', ARRAY['Et.','To.'], ARRAY['A','B','C'],
                        'Rismarker','PUBLISHED');
                """);

        themNhanVien("aa100000-0000-4000-8000-000000000001", "editor@travel.test", "Biên tập", bam, "EDITOR");
        themNhanVien("aa100000-0000-4000-8000-000000000002", "dich@travel.test", "Biên dịch", bam, "TRANSLATOR");
        themNhanVien("aa100000-0000-4000-8000-000000000003", "admin@travel.test", "Quản trị", bam, "ADMIN");
        themNhanVien("aa100000-0000-4000-8000-000000000004", "danghi@travel.test", "Đã nghỉ", bam, "ADMIN");
        jdbc.update("UPDATE staff_user SET is_active = FALSE WHERE email = 'danghi@travel.test'");
    }

    // ------------------------------------------------------------ phiên

    @Test
    @DisplayName("Đăng nhập đúng: 204, không có token trong thân phản hồi, cookie là HttpOnly")
    void dangNhapDung() {
        Phien phien = new Phien();
        ResponseEntity<String> phanHoi = phien.dangNhap("editor@travel.test", MAT_KHAU);

        assertEquals(HttpStatus.NO_CONTENT, phanHoi.getStatusCode());
        assertNull(phanHoi.getBody(), "Phiên nằm trong cookie, không nằm trong thân phản hồi");

        String cookie = String.join(" ", phanHoi.getHeaders()
                .getOrDefault(HttpHeaders.SET_COOKIE, List.of()));
        assertTrue(cookie.contains("HttpOnly"),
                "Thiếu HttpOnly là mã chèn vào trang đọc được phiên — docs/22 mục 9");
    }

    @Test
    @DisplayName("Sai mật khẩu và không có tài khoản trả CÙNG một câu trả lời")
    void saiThongTinDangNhap() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                new Phien().dangNhap("editor@travel.test", "sai-mat-khau").getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                new Phien().dangNhap("khong-ton-tai@travel.test", MAT_KHAU).getStatusCode(),
                "Phân biệt hai câu này là cho phép dò email nào có trong hệ thống");
    }

    @Test
    @DisplayName("Tài khoản đã tắt không đăng nhập được")
    void taiKhoanDaTat() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                new Phien().dangNhap("danghi@travel.test", MAT_KHAU).getStatusCode());
    }

    @Test
    @DisplayName("Chưa đăng nhập thì 401 kèm mã, không chuyển hướng tới trang đăng nhập")
    void chuaDangNhap() {
        ResponseEntity<ErrorResponse> phanHoi = new Phien()
                .goi(HttpMethod.GET, "/api/v1/admin/me", null, ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, phanHoi.getStatusCode());
    }

    @Test
    @DisplayName("Hồ sơ trả về vai trò; đăng xuất rồi thì không vào được nữa")
    void hoSoVaDangXuat() {
        Phien phien = dangNhap("admin@travel.test");

        StaffProfile ho_so = phien.goi(HttpMethod.GET, "/api/v1/admin/me", null, StaffProfile.class).getBody();
        assertEquals("admin@travel.test", ho_so.getEmail());
        assertEquals(List.of(StaffProfile.RolesEnum.ADMIN), ho_so.getRoles());

        assertEquals(HttpStatus.NO_CONTENT,
                phien.goi(HttpMethod.DELETE, "/api/v1/admin/session", null, Void.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                phien.goi(HttpMethod.GET, "/api/v1/admin/me", null, ErrorResponse.class).getStatusCode());
    }

    // ------------------------------------------------------------ ma trận quyền

    @Test
    @DisplayName("TIÊU CHÍ RA: người dịch sửa được bản vi, bị TỪ CHỐI ở bản da")
    void nguoiDichKhongSuaDuocBanNguon() {
        Phien phien = dangNhap("dich@travel.test");

        assertEquals(HttpStatus.OK, phien.luu("vi", than("viet-nam-tu-bac-vao-nam")).getStatusCode());

        ResponseEntity<ErrorResponse> banNguon = phien.luuLoi("da", than("nord-til-syd-2"));
        assertEquals(HttpStatus.FORBIDDEN, banNguon.getStatusCode());
        assertEquals("FORBIDDEN", banNguon.getBody().getCode());
    }

    @Test
    @DisplayName("Người viết thì ngược lại: sửa được bản da, bị từ chối ở bản vi")
    void nguoiVietKhongSuaDuocBanDich() {
        Phien phien = dangNhap("editor@travel.test");

        assertEquals(HttpStatus.OK, phien.luu("da", than("nord-til-syd")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, phien.luuLoi("vi", than("bac-vao-nam")).getStatusCode(),
                "Người viết sửa bản dịch là làm hai bản lệch nhau mà không ai biết bản nào đúng");
    }

    @Test
    @DisplayName("ADMIN sửa được cả hai bản")
    void quanTriSuaDuocCaHai() {
        Phien phien = dangNhap("admin@travel.test");

        assertEquals(HttpStatus.OK, phien.luu("da", than("nord-til-syd")).getStatusCode());
        assertEquals(HttpStatus.OK, phien.luu("vi", than("bac-vao-nam")).getStatusCode());
    }

    // ------------------------------------------------------------ ghi và kiểm toán

    @Test
    @DisplayName("Lưu bản dịch điền created_by và last_modified_by từ người đăng nhập")
    void cotKiemToanDuocDien() {
        dangNhap("dich@travel.test").luu("vi", than("bac-vao-nam"));

        Map<String, Object> dong = jdbc.queryForMap("""
                SELECT created_by, last_modified_by, created_at, last_modified_at
                FROM product_translation
                WHERE product_id = CAST(? AS uuid) AND locale = 'vi'
                """, SAN_PHAM);

        assertEquals(UUID.fromString("aa100000-0000-4000-8000-000000000002"), dong.get("created_by"),
                "Không service nào tự gán cột này — AuditorAware lo");
        assertEquals(UUID.fromString("aa100000-0000-4000-8000-000000000002"), dong.get("last_modified_by"));
        assertNotNull(dong.get("last_modified_at"));
    }

    @Test
    @DisplayName("last_modified_at do TRIGGER đặt, không do ứng dụng")
    void triggerDatThoiDiemSua() {
        Phien phien = dangNhap("admin@travel.test");
        phien.luu("da", than("nord-til-syd"));

        // Đẩy lùi mốc thời gian bằng SQL trần rồi lưu lại: nếu ứng dụng tự ghi
        // cột này thì giá trị mới do Java quyết. Trigger thì luôn đặt now().
        jdbc.update("""
                UPDATE product_translation SET last_modified_at = now() - interval '1 day'
                WHERE product_id = CAST(? AS uuid) AND locale = 'da'
                """, SAN_PHAM);

        phien.luu("da", than("nord-til-syd-moi"));

        Boolean moi = jdbc.queryForObject("""
                SELECT last_modified_at > now() - interval '1 minute'
                FROM product_translation WHERE product_id = CAST(? AS uuid) AND locale = 'da'
                """, Boolean.class, SAN_PHAM);

        assertEquals(Boolean.TRUE, moi, "BaseEntity cố tình không có @LastModifiedDate");
    }

    @Test
    @DisplayName("Danh sách bản dịch trả MỌI locale, ngôn ngữ nguồn trước, kèm cờ quá hạn")
    void danhSachBanDich() {
        dangNhap("admin@travel.test").luu("vi", than("bac-vao-nam"));

        AdminProductTranslation[] ds = dangNhap("editor@travel.test")
                .goi(HttpMethod.GET, "/api/v1/admin/products/" + SAN_PHAM + "/translations", null,
                        AdminProductTranslation[].class)
                .getBody();

        assertEquals(2, ds.length, "Bề mặt quản trị trả TẤT CẢ bản dịch, không một bản");
        assertEquals("da", ds[0].getLocale(), "Ngôn ngữ nguồn xếp trước — màn hình dịch song song");
        assertEquals(Boolean.TRUE, ds[0].getIsSource());
        assertNull(ds[0].getOutdated(), "Chính bản nguồn thì không có khái niệm quá hạn");
        assertEquals(Boolean.TRUE, ds[1].getOutdated(),
                "Chưa từng đánh dấu dịch xong thì bản dịch luôn quá hạn");
    }

    @Test
    @DisplayName("Mọi phản hồi quản trị là no-store")
    void quanTriKhongCache() {
        String cache = dangNhap("admin@travel.test")
                .goi(HttpMethod.GET, "/api/v1/admin/me", null, StaffProfile.class)
                .getHeaders().getCacheControl();

        assertNotNull(cache);
        assertTrue(cache.contains("no-store"),
                "Nội dung chưa xuất bản không được nằm trong bất kỳ cache nào");
    }

    // ------------------------------------------------------------ tiện ích

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

    private static String than(String slug) {
        return ("{\"slug\":\"%s\",\"title\":\"Tiêu đề\",\"shortDescription\":\"Mô tả ngắn.\","
                + "\"longDescription\":[\"Đoạn một.\",\"Đoạn hai.\"],"
                + "\"whyChooseThis\":[\"A\",\"B\",\"C\"],"
                + "\"heroImageAlt\":\"Chữ thay ảnh\",\"status\":\"DRAFT\"}").formatted(slug);
    }

    /**
     * Giữ cookie phiên và thẻ CSRF giữa các lời gọi — đúng như trình duyệt làm.
     *
     * <p>Bề mặt quản trị bật CSRF: nó ghi, và nó xác thực bằng cookie. Test phải
     * đi qua đúng cơ chế đó, nếu không nó kiểm một hệ thống khác với hệ thống
     * chạy thật.
     */
    private final class Phien {

        private final List<String> cookies = new ArrayList<>();

        ResponseEntity<String> dangNhap(String email, String matKhau) {
            return goi(HttpMethod.POST, "/api/v1/admin/session",
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, matKhau),
                    String.class);
        }

        ResponseEntity<AdminProductTranslation> luu(String locale, String than) {
            return goi(HttpMethod.PUT, duongDanLuu(locale), than, AdminProductTranslation.class);
        }

        ResponseEntity<ErrorResponse> luuLoi(String locale, String than) {
            return goi(HttpMethod.PUT, duongDanLuu(locale), than, ErrorResponse.class);
        }

        private String duongDanLuu(String locale) {
            return "/api/v1/admin/products/" + SAN_PHAM + "/translations/" + locale;
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
