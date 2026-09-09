package vn.travel.booking.quote.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.travel.booking.quote.repository.QuoteRepository;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Cho báo giá quá hạn sang {@code EXPIRED} — docs/14 mục 7 quy tắc 4.
 *
 * <p><b>Khác {@code SeatHoldSweeper} ở một điểm quan trọng.</b> Ở giữ chỗ, job
 * chỉ dọn dẹp: công thức chỗ khả dụng đọc {@code expires_at > now()} nên một
 * giữ chỗ quá hạn đã hết tác dụng dù cột chưa cập nhật. Ở đây thì <b>cột {@code
 * status} là sự thật</b> — màn hình M8 lọc theo nó, và luật "quá hạn thì không
 * ACCEPTED được" đọc nó. Job này chết thì báo giá quá hạn vẫn nằm ở {@code SENT}
 * trong danh sách.
 *
 * <p>Lưới an toàn cho tình huống đó nằm ở {@code AdminQuoteService.doiTrangThai}:
 * nó so {@code validUntil} với đồng hồ chứ không tin cột {@code status}, nên
 * một báo giá quá hạn mà job chưa quét tới vẫn không chấp nhận được. Hai lớp
 * chặn cho cùng một quy tắc là có chủ ý — quy tắc này liên quan tới tiền.
 *
 * <p>Chạy 05:00 UTC mỗi ngày chứ không mỗi phút: hạn báo giá tính theo
 * <b>ngày</b>, nên quét mỗi phút là 1440 lần {@code UPDATE} rỗng mỗi ngày để
 * biết sớm hơn vài giờ một thứ không ai đang chờ.
 */
@Component
public class QuoteSweeper {

    private static final Logger log = LoggerFactory.getLogger(QuoteSweeper.class);

    private final QuoteRepository quote;
    private final Clock dongHo;

    public QuoteSweeper(QuoteRepository quote, Clock dongHo) {
        this.quote = quote;
        this.dongHo = dongHo;
    }

    @Scheduled(cron = "0 0 5 * * *")
    @SchedulerLock(name = "quoteSweeper", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void quet() {
        int count = donDep();
        if (count > 0) {
            log.info("Đã cho {} báo giá quá hạn sang EXPIRED", count);
        }
    }

    /** Tách riêng để test gọi được mà không phải chờ lịch. */
    public int donDep() {
        return quote.hetHan(LocalDate.now(dongHo));
    }
}
