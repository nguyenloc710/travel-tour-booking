package vn.travel.booking.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Khoá job dùng chính cơ sở dữ liệu — không thêm Redis chỉ để giữ một cái khoá.
 *
 * <p>Bảng {@code shedlock} tạo ở {@code V4}; ShedLock không tự tạo bảng, và đó là
 * điều đúng: lược đồ do Flyway sở hữu.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class JobConfig {

    @Bean
    LockProvider lockProvider(JdbcTemplate jdbc) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbc)
                .usingDbTime()   // đồng hồ của CSDL, không của từng máy chủ
                .build());
    }
}
