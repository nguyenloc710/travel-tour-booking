package vn.travel.booking.application.booking;

import java.time.Duration;
import java.util.UUID;

public interface SeatHoldPort {

    /**
     * Giữ chỗ bằng <b>khoá bi quan</b> — docs/14 mục 6.2.
     *
     * <p>Khoá lạc quan không dùng được ở đây: khách thứ hai chỉ biết mình trượt
     * <b>sau khi</b> đã điền xong thông tin và bấm thanh toán. Khoá bi quan cho
     * câu trả lời ngay ở bước chọn ngày, và transaction chỉ dài vài mili giây.
     *
     * @throws BookingErrors.DepartureSoldOut khi chỗ khả dụng không đủ
     */
    SeatHoldView hold(UUID departureId, int seats, String sessionRef, Duration hanGiu);

    /** Trả chỗ về kho khi khách quay lại bước trước. Gọi lại nhiều lần không hại. */
    void release(UUID seatHoldId);
}
