package vn.travel.booking.common.config;

import vn.travel.booking.ApiApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Đồng hồ được tiêm, không đọc thẳng {@code LocalDate.now()} ở tầng dưới.
 *
 * <p>Quy tắc của api/CLAUDE.md mục 1: use case nhận "hôm nay" từ {@code Clock}
 * này và truyền xuống. Test cố định được ngày, nên test không đỏ vào một ngày
 * nào đó trong tương lai mà không ai hiểu vì sao.
 *
 * <p>UTC vì toàn bộ ứng dụng chạy ở UTC — xem khối {@code static} của
 * {@code ApiApplication}.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
