package vn.travel.booking.departure.dto;


/**
 * Trạng thái ngày khởi hành <b>hiển thị cho khách</b> — kết quả của
 * {@link DepartureStatuses#resolve}.
 */
public enum DepartureStatus {
    OPEN,
    FEW_SEATS,
    GUARANTEED,
    SOLD_OUT,
    PENDING
}
