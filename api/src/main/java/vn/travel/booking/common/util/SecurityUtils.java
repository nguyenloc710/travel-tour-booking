package vn.travel.booking.common.util;

import vn.travel.booking.auth.dto.StaffPrincipal;
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
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static StaffPrincipal currentStaff() {
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc != null && xacThuc.getPrincipal() instanceof StaffPrincipal staff) {
            return staff;
        }
        // Không xảy ra được với đường dẫn /admin/** vì chuỗi lọc đã chặn trước,
        // nhưng ném rõ ràng còn hơn trả null cho chỗ gọi tự đoán.
        throw new IllegalStateException("không có nhân viên nào trong ngữ cảnh bảo mật");
    }

    public static Set<String> roles() {
        return Set.copyOf(currentStaff().roleList());
    }
}
