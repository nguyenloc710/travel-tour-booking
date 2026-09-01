package vn.travel.booking.infrastructure.job;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Quét giữ chỗ quá hạn — docs/14 mục 6.4.
 *
 * <p><b>Job này chỉ dọn dẹp, không phải cơ chế trả chỗ.</b> Chỗ về kho ngay khi
 * hết hạn vì công thức chỗ khả dụng dùng {@code expires_at > now()}: một giữ chỗ
 * quá hạn không còn được tính dù cột {@code released_at} chưa cập nhật. Nếu job
 * chết, hệ thống vẫn bán đúng — chỉ có bảng {@code seat_hold} lớn dần.
 *
 * <p><b>ShedLock từ job đầu tiên</b>, dù lệnh {@code UPDATE} này bất biến khi
 * lặp. Cùng cơ chế sẽ dùng cho gửi email nhắc và cho hết hạn báo giá — hai việc
 * không bất biến, và lúc đó nhớ ra thì đã muộn.
 */
@Component
public class SeatHoldSweeper {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldSweeper.class);

    private final JdbcTemplate jdbc;

    public SeatHoldSweeper(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "seatHoldSweeper", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    public void quet() {
        int so = donDep();
        if (so > 0) {
            log.info("Đã trả {} giữ chỗ quá hạn về kho", so);
        }
    }

    /** Tách riêng để test gọi được mà không phải chờ lịch. */
    public int donDep() {
        return jdbc.update("""
                UPDATE seat_hold SET released_at = now()
                WHERE released_at IS NULL AND expires_at <= now()
                """);
    }
}
