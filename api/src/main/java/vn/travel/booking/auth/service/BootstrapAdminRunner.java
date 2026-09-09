package vn.travel.booking.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Tạo tài khoản {@code ADMIN} đầu tiên lúc ứng dụng khởi động, để một cơ sở dữ
 * liệu trống vẫn có lối vào trang quản trị.
 *
 * <p><b>Vì sao cần.</b> Hợp đồng API không có đường tạo người dùng:
 * {@code openapi.yaml} chỉ có {@code GET /admin/users},
 * {@code PATCH /admin/users/&#123;id&#125;} và {@code PUT .../roles}. Không có
 * bước này thì sau khi Flyway chạy xong trên một CSDL mới, {@code staff_user}
 * rỗng và <b>không ai đăng nhập được</b> — kể cả để tạo người khác. Ở dev chỗ
 * này được che bởi {@code seed-dev.sql}, nên nó chỉ lộ ra lúc triển khai thật.
 *
 * <p><b>Chỉ chạy khi chưa TỪNG có {@code ADMIN} nào</b> — không phải "không có
 * {@code ADMIN} nào đang bật". Một quản trị viên bị tắt bằng tay là một quyết
 * định; nếu điều kiện tính theo {@code is_active} thì chỉ cần khởi động lại
 * container là tài khoản đó sống dậy, và biến môi trường thành cửa hậu.
 *
 * <p>Hệ quả: <b>không đổi được mật khẩu bằng cách sửa biến môi trường rồi khởi
 * động lại.</b> Đó là chủ ý. Hiện chưa có endpoint đổi mật khẩu nào trong hợp
 * đồng, nên xoay mật khẩu vẫn là việc làm bằng SQL — xem {@code docs/22}.
 *
 * <p>Chạy sau Flyway: {@link ApplicationRunner} gọi sau khi context đã nạp
 * xong, mà {@code flywayInitializer} là điều kiện của {@code entityManagerFactory}.
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    /**
     * Mật khẩu ngắn hơn thì bỏ qua kèm cảnh báo, <b>không</b> làm ứng dụng chết.
     * Cấu hình sai một tính năng khởi tạo không đáng để cả site sập; dòng
     * {@code WARN} nói đủ rõ vì sao không có tài khoản nào được tạo.
     */
    private static final int DAI_TOI_THIEU = 12;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder maHoa;
    private final String email;
    private final String tenHienThi;
    private final String matKhau;

    public BootstrapAdminRunner(
            JdbcTemplate jdbc,
            PasswordEncoder maHoa,
            @Value("${travel.bootstrap-admin.email:}") String email,
            @Value("${travel.bootstrap-admin.display-name:Quản trị viên}") String tenHienThi,
            @Value("${travel.bootstrap-admin.password:}") String matKhau) {
        this.jdbc = jdbc;
        this.maHoa = maHoa;
        // Hạ chữ thường ngay tại đây: StaffUserDetailsService tra cứu bằng email
        // đã hạ chữ thường, nên một địa chỉ có chữ hoa trong `.env` sẽ tạo được
        // tài khoản mà không đăng nhập được vào.
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.tenHienThi = tenHienThi;
        this.matKhau = matKhau == null ? "" : matKhau;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty() && matKhau.isEmpty()) {
            return;   // không cấu hình — trạng thái bình thường của dev và test
        }
        if (email.isEmpty() || matKhau.isEmpty()) {
            log.warn("Bỏ qua tạo ADMIN khởi tạo: phải đặt CẢ HAI biến "
                    + "BOOTSTRAP_ADMIN_EMAIL và BOOTSTRAP_ADMIN_PASSWORD.");
            return;
        }
        if (matKhau.length() < DAI_TOI_THIEU) {
            log.warn("Bỏ qua tạo ADMIN khởi tạo: mật khẩu ngắn hơn {} ký tự.", DAI_TOI_THIEU);
            return;
        }

        // Đếm MỌI dòng ADMIN, kể cả của người đã tắt hoặc đã xoá mềm.
        Integer daCoAdmin = jdbc.queryForObject(
                "SELECT count(*) FROM staff_user_role WHERE role_code = 'ADMIN'",
                Integer.class);
        if (daCoAdmin != null && daCoAdmin > 0) {
            log.info("Đã có ADMIN trong hệ thống — bỏ qua tạo tài khoản khởi tạo.");
            return;
        }

        // Địa chỉ đã có chủ nhưng chưa mang vai trò ADMIN: dừng lại thay vì cấp
        // thêm quyền cho một tài khoản mình không tạo ra.
        Integer trungEmail = jdbc.queryForObject(
                "SELECT count(*) FROM staff_user WHERE email = ? AND NOT soft_delete",
                Integer.class, email);
        if (trungEmail != null && trungEmail > 0) {
            log.warn("Bỏ qua tạo ADMIN khởi tạo: đã có tài khoản mang địa chỉ này.");
            return;
        }

        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash, is_active)
                VALUES (?, ?, ?, ?, TRUE)
                """, id, email, tenHienThi, maHoa.encode(matKhau));
        jdbc.update("""
                INSERT INTO staff_user_role (id, staff_user_id, role_code)
                VALUES (?, ?, 'ADMIN')
                """, UUID.randomUUID(), id);

        // Ghi địa chỉ để người vận hành đối chiếu. KHÔNG bao giờ ghi mật khẩu,
        // kể cả ở mức DEBUG: log rời khỏi máy chủ, mật khẩu thì không.
        log.info("Đã tạo ADMIN khởi tạo cho {}.", email);
    }
}
