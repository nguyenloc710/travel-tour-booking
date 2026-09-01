package vn.travel.booking.domain.booking;

/**
 * Bước chuyển trạng thái không hợp lệ.
 *
 * <p>Ném lỗi chứ không bỏ qua: một đơn đi sai đường trong máy trạng thái là một
 * đơn mà nhật ký {@code booking_event} đọc lên không còn thành câu chuyện có
 * thật, và đó chính là thứ duy nhất trả lời được khi khách khiếu nại.
 */
public class IllegalBookingTransitionException extends RuntimeException {

    public IllegalBookingTransitionException(BookingStatus tu, BookingStatus sang) {
        super("Không đi từ " + tu + " sang " + sang + " được");
    }
}
