package vn.travel.booking.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.travel.booking.application.auth.CurrentStaffProvider;

import java.util.Optional;
import java.util.UUID;

/**
 * Hiện thực cổng "ai đang thao tác" bằng ngữ cảnh bảo mật của Spring.
 *
 * <p>Đây là <b>chỗ duy nhất</b> trong toàn hệ thống đọc {@code SecurityContext}
 * để lấy id nhân viên phục vụ cột kiểm toán. Job nền và migration chạy ngoài mọi
 * yêu cầu HTTP nên trả rỗng — bịa một "id hệ thống" là làm cột "ai sửa" nói dối.
 */
@Component
class CurrentStaffFromSecurityContext implements CurrentStaffProvider {

    @Override
    public Optional<UUID> currentStaffId() {
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc != null
                && xacThuc.isAuthenticated()
                && xacThuc.getPrincipal() instanceof StaffPrincipal nhanVien) {
            return Optional.of(nhanVien.id());
        }
        return Optional.empty();
    }
}
