package vn.travel.booking.application.booking;

import java.util.Optional;

public interface BookingWritePort {

    /**
     * Tạo đơn trong <b>một</b> transaction — docs/14 mục 6.3.
     *
     * <p>Sáu việc phải cùng thành công hoặc cùng không: khoá giữ chỗ, kiểm lại
     * hạn, cộng {@code seats_booked}, đánh dấu giữ chỗ đã dùng, ghi đơn cùng các
     * dòng và hành khách, và ghi một dòng {@code booking_event}.
     *
     * <p>Bước kiểm lại hạn <b>phải làm lại</b> dù tầng trên đã kiểm: thời gian
     * trôi giữa hai lần gọi, và giữ chỗ có thể vừa hết hạn.
     *
     * @throws BookingErrors.SeatHoldExpired khi giữ chỗ đã hết hạn hoặc đã dùng
     */
    BookingView create(BookingDraft draft);

    /** Tra đơn bằng mã và email — không có đăng nhập cho khách ở v1. */
    Optional<BookingView> findByReferenceAndEmail(String market, String reference, String email);
}
