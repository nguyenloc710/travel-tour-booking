package vn.travel.booking.booking.service;

import vn.travel.booking.booking.dto.BookingStatus;
import vn.travel.booking.common.exception.IllegalBookingTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máy trạng thái đơn đặt — docs/23 mục 4.
 *
 * <p><b>Không có bước lùi.</b> Huỷ nhầm thì tạo đơn mới, không đưa
 * {@code CANCELLED} trở lại {@code CONFIRMED}: nhật ký phải đọc được như một câu
 * chuyện có thật. Bảng dưới đây không có nhánh nào đi ngược, và đó là điều kiện
 * duy nhất khiến {@code booking_event} có giá trị khi có tranh chấp.
 */
public final class BookingStatuses {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(BookingStatus.DRAFT, EnumSet.of(
                BookingStatus.PENDING_PAYMENT, BookingStatus.CANCELLED, BookingStatus.EXPIRED));

        ALLOWED_TRANSITIONS.put(BookingStatus.PENDING_PAYMENT, EnumSet.of(
                BookingStatus.PENDING_CONFIRMATION, BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED, BookingStatus.EXPIRED));

        ALLOWED_TRANSITIONS.put(BookingStatus.PENDING_CONFIRMATION, EnumSet.of(
                BookingStatus.CONFIRMED, BookingStatus.CANCELLED));

        ALLOWED_TRANSITIONS.put(BookingStatus.CONFIRMED, EnumSet.of(
                BookingStatus.COMPLETED, BookingStatus.CANCELLED));

        // Ba trạng thái cuối: đi vào rồi thì chỉ còn một đường ra, hoặc không còn.
        ALLOWED_TRANSITIONS.put(BookingStatus.CANCELLED, EnumSet.of(BookingStatus.REFUNDED));
        ALLOWED_TRANSITIONS.put(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITIONS.put(BookingStatus.REFUNDED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITIONS.put(BookingStatus.EXPIRED, EnumSet.noneOf(BookingStatus.class));
    }

    private BookingStatuses() {
    }

    public static boolean canTransitionTo(BookingStatus from, BookingStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(BookingStatus.class)).contains(to);
    }

    public static void requireTransition(BookingStatus from, BookingStatus to) {
        if (!canTransitionTo(from, to)) {
            throw new IllegalBookingTransitionException(from, to);
        }
    }

    /**
     * Trạng thái đã chốt: không còn đường đi tiếp.
     *
     * <p>Dùng để biết đơn nào còn phải theo dõi trên bảng điều khiển quản trị.
     */
    public static boolean isClosed(BookingStatus status) {
        return ALLOWED_TRANSITIONS.getOrDefault(status, EnumSet.noneOf(BookingStatus.class)).isEmpty();
    }

    /**
     * Trạng thái nào <b>đang giữ chỗ trong kho</b>.
     *
     * <p>Huỷ trả chỗ về kho ngay, không chờ hoàn tiền xong (docs/14 mục 6.5):
     * hoàn tiền và trả chỗ là hai việc độc lập, và giữ chỗ trống trong lúc chờ
     * ngân hàng là mất doanh thu vô ích.
     */
    public static boolean occupiesSeat(BookingStatus status) {
        return EnumSet.of(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.PENDING_CONFIRMATION,
                BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED).contains(status);
    }
}
