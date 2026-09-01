package vn.travel.booking.domain.booking;

/** Tám trạng thái của đơn đặt — docs/23 mục 4. */
public enum BookingStatus {
    DRAFT,
    PENDING_PAYMENT,
    PENDING_CONFIRMATION,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    EXPIRED
}
