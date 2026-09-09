package vn.travel.booking.quote.service;

import vn.travel.booking.common.exception.QuoteErrors;
import vn.travel.booking.quote.dto.QuoteStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máy trạng thái báo giá — docs/14 mục 7.
 *
 * <p>Hàm thuần, không Spring, không CSDL: luật đi được hay không phải kiểm được
 * bằng JUnit trong vài mili giây. Cùng khuôn với {@code BookingStatuses}, và
 * cùng một luật gốc — <b>không có bước lùi</b>.
 *
 * <pre>
 * DRAFT ──► SENT ──► ACCEPTED
 *             ├──► REJECTED
 *             └──► EXPIRED
 * </pre>
 *
 * <p><b>DRAFT không đi thẳng tới ACCEPTED được.</b> Nghe thì tiện — khách gọi
 * điện đồng ý ngay — nhưng một báo giá chưa gửi thì chưa có giá nào để đồng ý,
 * và {@code valid_until} chỉ sinh ra ở bước gửi. Bỏ qua {@code SENT} là tạo ra
 * một cam kết giá không có hạn.
 */
public final class QuoteStatuses {

    private static final Map<QuoteStatus, Set<QuoteStatus>> DUOC_PHEP =
            new EnumMap<>(QuoteStatus.class);

    static {
        DUOC_PHEP.put(QuoteStatus.DRAFT, EnumSet.of(QuoteStatus.SENT));

        DUOC_PHEP.put(QuoteStatus.SENT, EnumSet.of(
                QuoteStatus.ACCEPTED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED));

        // Ba trạng thái cuối. Khách đổi ý sau khi từ chối thì gửi yêu cầu MỚI —
        // quy tắc 4 nói thẳng điều đó cho trường hợp hết hạn, và cùng lý do áp
        // cho hai trường hợp kia: một báo giá đã kết thúc là một tài liệu đã
        // chốt, không phải một bản nháp sống lại được.
        DUOC_PHEP.put(QuoteStatus.ACCEPTED, EnumSet.noneOf(QuoteStatus.class));
        DUOC_PHEP.put(QuoteStatus.REJECTED, EnumSet.noneOf(QuoteStatus.class));
        DUOC_PHEP.put(QuoteStatus.EXPIRED, EnumSet.noneOf(QuoteStatus.class));
    }

    private QuoteStatuses() {
    }

    public static boolean canTransitionTo(QuoteStatus tu, QuoteStatus sang) {
        return DUOC_PHEP.getOrDefault(tu, EnumSet.noneOf(QuoteStatus.class)).contains(sang);
    }

    public static void requireTransition(QuoteStatus tu, QuoteStatus sang) {
        if (!canTransitionTo(tu, sang)) {
            throw new QuoteErrors.NotAcceptable(tu, sang);
        }
    }

    /** Đã chốt: không còn đường đi tiếp. Dùng để biết báo giá nào còn phải theo dõi. */
    public static boolean isClosed(QuoteStatus status) {
        return DUOC_PHEP.getOrDefault(status, EnumSet.noneOf(QuoteStatus.class)).isEmpty();
    }

    /**
     * Trạng thái nào <b>còn sửa bảng giá được</b>.
     *
     * <p>Chỉ {@code DRAFT}. Đã gửi rồi thì bảng giá là thứ khách đang cầm trong
     * tay, và sửa nó sau lưng khách là thứ không có cách nào giải thích khi hai
     * bên mang hai bản ra đối chiếu.
     */
    public static boolean canEditPriceTiers(QuoteStatus status) {
        return status == QuoteStatus.DRAFT;
    }
}
