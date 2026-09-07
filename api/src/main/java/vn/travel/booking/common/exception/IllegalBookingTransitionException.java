package vn.travel.booking.common.exception;

import vn.travel.booking.booking.dto.BookingStatus;

import java.util.Map;

/**
 * Bước chuyển trạng thái không hợp lệ.
 *
 * <p>Ném lỗi chứ không bỏ qua: một đơn đi sai đường trong máy trạng thái là một
 * đơn mà nhật ký {@code booking_event} đọc lên không còn thành câu chuyện có
 * thật, và đó chính là thứ duy nhất trả lời được khi khách khiếu nại.
 *
 * <p>Mang theo {@code from} và {@code to} vì frontend cần chúng để dựng câu
 * tiếng người: "đơn đã huỷ thì không xác nhận lại được" hữu ích hơn hẳn "thao
 * tác không hợp lệ", và backend vẫn không trả câu tiếng người nào.
 */
public class IllegalBookingTransitionException extends RuntimeException {

    private final transient Map<String, Object> params;

    public IllegalBookingTransitionException(BookingStatus tu, BookingStatus sang) {
        super("Không đi từ " + tu + " sang " + sang + " được");
        this.params = Map.of("from", tu.name(), "to", sang.name());
    }

    public Map<String, Object> params() {
        return params;
    }
}
