package vn.travel.booking.application.shared;

/**
 * Cùng {@code Idempotency-Key} nhưng thân yêu cầu khác lần trước.
 *
 * <p>Trả 409 chứ không trả kết quả cũ: client đang dùng lại khoá cho một việc
 * khác, và trả kết quả cũ nghĩa là đơn thứ hai biến mất mà không ai biết.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String chiTiet) {
        super(chiTiet);
    }
}
