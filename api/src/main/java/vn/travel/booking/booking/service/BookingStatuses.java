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

    private static final Map<BookingStatus, Set<BookingStatus>> DUOC_PHEP =
            new EnumMap<>(BookingStatus.class);

    static {
        DUOC_PHEP.put(BookingStatus.DRAFT, EnumSet.of(
                BookingStatus.PENDING_PAYMENT, BookingStatus.CANCELLED, BookingStatus.EXPIRED));

        DUOC_PHEP.put(BookingStatus.PENDING_PAYMENT, EnumSet.of(
                BookingStatus.PENDING_CONFIRMATION, BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED, BookingStatus.EXPIRED));

        DUOC_PHEP.put(BookingStatus.PENDING_CONFIRMATION, EnumSet.of(
                BookingStatus.CONFIRMED, BookingStatus.CANCELLED));

        DUOC_PHEP.put(BookingStatus.CONFIRMED, EnumSet.of(
                BookingStatus.COMPLETED, BookingStatus.CANCELLED));

        // Ba trạng thái cuối: đi vào rồi thì chỉ còn một đường ra, hoặc không còn.
        DUOC_PHEP.put(BookingStatus.CANCELLED, EnumSet.of(BookingStatus.REFUNDED));
        DUOC_PHEP.put(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class));
        DUOC_PHEP.put(BookingStatus.REFUNDED, EnumSet.noneOf(BookingStatus.class));
        DUOC_PHEP.put(BookingStatus.EXPIRED, EnumSet.noneOf(BookingStatus.class));
    }

    private BookingStatuses() {
    }

    public static boolean diDuoc(BookingStatus tu, BookingStatus sang) {
        return DUOC_PHEP.getOrDefault(tu, EnumSet.noneOf(BookingStatus.class)).contains(sang);
    }

    public static void phaiDiDuoc(BookingStatus tu, BookingStatus sang) {
        if (!diDuoc(tu, sang)) {
            throw new IllegalBookingTransitionException(tu, sang);
        }
    }

    /**
     * Trạng thái đã chốt: không còn đường đi tiếp.
     *
     * <p>Dùng để biết đơn nào còn phải theo dõi trên bảng điều khiển quản trị.
     */
    public static boolean daChot(BookingStatus trangThai) {
        return DUOC_PHEP.getOrDefault(trangThai, EnumSet.noneOf(BookingStatus.class)).isEmpty();
    }

    /**
     * Trạng thái nào <b>đang giữ chỗ trong kho</b>.
     *
     * <p>Huỷ trả chỗ về kho ngay, không chờ hoàn tiền xong (docs/14 mục 6.5):
     * hoàn tiền và trả chỗ là hai việc độc lập, và giữ chỗ trống trong lúc chờ
     * ngân hàng là mất doanh thu vô ích.
     */
    public static boolean dangChiemCho(BookingStatus trangThai) {
        return EnumSet.of(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.PENDING_CONFIRMATION,
                BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED).contains(trangThai);
    }
}
