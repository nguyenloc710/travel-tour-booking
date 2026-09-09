package vn.travel.booking.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.travel.booking.auth.service.BootstrapAdminRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tài khoản {@code ADMIN} đầu tiên — {@link BootstrapAdminRunner}.
 *
 * <p>Các test có thứ tự vì test đầu kiểm <b>tác dụng của lúc khởi động</b>, thứ
 * chỉ quan sát được khi chưa test nào kịp sửa bảng. Những test sau tự dựng lấy
 * trạng thái của mình.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
class BootstrapAdminIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("travel")
                    .withUsername("travel")
                    .withPassword("travel");

    /** Có chữ HOA và khoảng trắng thừa — cả hai phải bị chuẩn hoá. */
    private static final String EMAIL_NHAP = "  Quan.Tri@Travel.Test  ";
    private static final String EMAIL_CHUAN = "quan.tri@travel.test";
    private static final String MAT_KHAU = "mat-khau-du-dai";

    @DynamicPropertySource
    static void cauHinh(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("travel.bootstrap-admin.email", () -> EMAIL_NHAP);
        registry.add("travel.bootstrap-admin.password", () -> MAT_KHAU);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BootstrapAdminRunner runner;

    @Autowired
    PasswordEncoder maHoa;

    @Test
    @Order(1)
    @DisplayName("CSDL trống: khởi động tạo đúng một ADMIN, email đã hạ chữ thường")
    void tao_admin_tren_csdl_trong() {
        assertEquals(1, demNguoiDung(),
                "runner phải chạy lúc khởi động, sau Flyway");

        assertEquals(EMAIL_CHUAN, jdbc.queryForObject(
                "SELECT email FROM staff_user", String.class),
                "email phải hạ chữ thường và cắt khoảng trắng — "
                        + "StaffUserDetailsService tra cứu bằng dạng đã hạ");

        assertTrue(jdbc.queryForObject(
                "SELECT is_active FROM staff_user", Boolean.class));

        assertEquals(1, (int) jdbc.queryForObject("""
                SELECT count(*) FROM staff_user_role WHERE role_code = 'ADMIN'
                """, Integer.class));

        // Mật khẩu phải nằm trong CSDL dưới dạng băm BCrypt kiểm được, không
        // phải chữ thường.
        String bam = jdbc.queryForObject("SELECT password_hash FROM staff_user", String.class);
        assertTrue(maHoa.matches(MAT_KHAU, bam));
    }

    @Test
    @Order(2)
    @DisplayName("Chạy lại khi đã có ADMIN: không tạo thêm")
    void chay_lai_khong_tao_them() {
        runner.run(null);
        assertEquals(1, demNguoiDung());
    }

    @Test
    @Order(3)
    @DisplayName("ADMIN đã bị tắt bằng tay: khởi động lại KHÔNG hồi sinh nó")
    void khong_hoi_sinh_admin_da_tat() {
        jdbc.update("UPDATE staff_user SET is_active = FALSE");

        runner.run(null);

        assertEquals(1, demNguoiDung(), "không được tạo tài khoản thứ hai");
        assertFalse(jdbc.queryForObject(
                "SELECT is_active FROM staff_user", Boolean.class),
                "tắt tài khoản là một quyết định — biến môi trường không được lật lại nó");
    }

    @Test
    @Order(4)
    @DisplayName("Mật khẩu quá ngắn: bỏ qua, không làm ứng dụng chết")
    void bo_qua_mat_khau_ngan() {
        xoaSachNguoiDung();

        new BootstrapAdminRunner(jdbc, maHoa, "ngan@travel.test", "Ngắn", "ngan")
                .run(null);

        assertEquals(0, demNguoiDung());
    }

    @Test
    @Order(5)
    @DisplayName("Thiếu một trong hai biến: bỏ qua")
    void bo_qua_khi_thieu_mot_bien() {
        xoaSachNguoiDung();

        new BootstrapAdminRunner(jdbc, maHoa, "co-email@travel.test", "Tên", "")
                .run(null);
        assertEquals(0, demNguoiDung());

        new BootstrapAdminRunner(jdbc, maHoa, "", "Tên", MAT_KHAU).run(null);
        assertEquals(0, demNguoiDung());
    }

    @Test
    @Order(6)
    @DisplayName("Email đã có chủ nhưng chưa là ADMIN: không cấp thêm quyền cho nó")
    void khong_cap_quyen_cho_tai_khoan_co_san() {
        xoaSachNguoiDung();
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash, is_active)
                VALUES (gen_random_uuid(), ?, 'Biên tập', 'x', TRUE)
                """, EMAIL_CHUAN);

        new BootstrapAdminRunner(jdbc, maHoa, EMAIL_CHUAN, "Tên", MAT_KHAU).run(null);

        assertEquals(1, demNguoiDung(), "không được tạo thêm dòng trùng email");
        assertEquals(0, (int) jdbc.queryForObject("""
                SELECT count(*) FROM staff_user_role WHERE role_code = 'ADMIN'
                """, Integer.class),
                "không được nâng quyền một tài khoản mà runner không tạo ra");
    }

    private int demNguoiDung() {
        return jdbc.queryForObject("SELECT count(*) FROM staff_user", Integer.class);
    }

    private void xoaSachNguoiDung() {
        jdbc.execute("DELETE FROM staff_user_role; DELETE FROM staff_user;");
    }
}
