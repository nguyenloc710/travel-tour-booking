package vn.travel.booking.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * Người đang đăng nhập, lấy ở <b>một chỗ duy nhất</b>.
 *
 * <p>Khuôn mượn của dự án trước: không service nào tự đọc {@code SecurityContext}.
 * Rải nó ra nhiều chỗ nghĩa là mỗi chỗ tự quyết xử lý thế nào khi chưa đăng nhập,
 * và sẽ có chỗ quyết sai.
 */
final class SecurityUtils {

    private SecurityUtils() {
    }

    static StaffPrincipal nhanVienHienTai() {
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc != null && xacThuc.getPrincipal() instanceof StaffPrincipal nhanVien) {
            return nhanVien;
        }
        // Không xảy ra được với đường dẫn /admin/** vì chuỗi lọc đã chặn trước,
        // nhưng ném rõ ràng còn hơn trả null cho chỗ gọi tự đoán.
        throw new IllegalStateException("không có nhân viên nào trong ngữ cảnh bảo mật");
    }

    static Set<String> vaiTro() {
        return Set.copyOf(nhanVienHienTai().roleList());
    }
}
