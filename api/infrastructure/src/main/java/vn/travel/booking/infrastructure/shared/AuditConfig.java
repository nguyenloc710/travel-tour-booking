package vn.travel.booking.infrastructure.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import vn.travel.booking.application.auth.CurrentStaffProvider;

import java.util.UUID;

/**
 * Điền {@code created_by} và {@code last_modified_by} từ người đang đăng nhập.
 *
 * <p>Không service nào tự đi hỏi ngữ cảnh bảo mật để gán hai cột này — quên một
 * chỗ là một bản ghi không biết ai sửa, và không có gì báo.
 *
 * <p>Câu hỏi "ai đang thao tác" đi qua cổng {@code CurrentStaffProvider} chứ
 * không đọc thẳng {@code SecurityContext}: tầng này không được biết tầng
 * {@code web} tồn tại.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {

    @Bean
    AuditorAware<UUID> auditorAware(CurrentStaffProvider nguoiDangThaoTac) {
        return nguoiDangThaoTac::currentStaffId;
    }
}
