package vn.travel.booking.common.exception;

/**
 * Bốn tình huống nghiệp vụ của đường đặt tour, mỗi cái một mã lỗi ở
 * docs/13 mục 5.1.
 *
 * <p>Lớp riêng cho từng cái chứ không một lớp mang mã: mã lỗi là thứ frontend
 * dịch, còn kiểu ngoại lệ là thứ trình biên dịch kiểm — bắt nhầm một loại là lỗi
 * biên dịch, không phải một chuỗi gõ sai.
 */
public final class BookingErrors {

    private BookingErrors() {
    }

    /** Không đủ chỗ khả dụng. HTTP 409. */
    public static class DepartureSoldOut extends RuntimeException {
        public DepartureSoldOut(String detail) {
            super(detail);
        }
    }

    /** Giữ chỗ đã hết hạn hoặc đã dùng. HTTP 409. */
    public static class SeatHoldExpired extends RuntimeException {
        public SeatHoldExpired(String detail) {
            super(detail);
        }
    }

    /** Loại sản phẩm không đặt trực tiếp được — {@code PRIVATE_TOUR}. HTTP 422. */
    public static class ProductNotBookable extends RuntimeException {
        public ProductNotBookable(String detail) {
            super(detail);
        }
    }

    /** Ngày khởi hành đã đóng bán. HTTP 409. */
    public static class DepartureClosed extends RuntimeException {
        public DepartureClosed(String detail) {
            super(detail);
        }
    }
}
