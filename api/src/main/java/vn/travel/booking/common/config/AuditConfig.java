package vn.travel.booking.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.travel.booking.auth.dto.StaffPrincipal;

import java.util.Optional;
import java.util.UUID;

/**
 * Điền {@code created_by} và {@code last_modified_by} từ người đang đăng nhập.
 *
 * <p>Không service nào tự đi hỏi ngữ cảnh bảo mật để gán hai cột này — quên một
 * chỗ là một bản ghi không biết ai sửa, và không có gì báo.
 *
 * <p>Job nền và migration chạy ngoài mọi yêu cầu HTTP nên trả rỗng. Bịa một
 * "id hệ thống" là làm cột "ai sửa" nói dối.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {

    @Bean
    AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
            if (xacThuc != null
                    && xacThuc.isAuthenticated()
                    && xacThuc.getPrincipal() instanceof StaffPrincipal nhanVien) {
                return Optional.of(nhanVien.id());
            }
            return Optional.empty();
        };
    }
}
