package vn.travel.booking.quote.dto;

/**
 * Năm trạng thái của một báo giá — docs/14 mục 7.
 *
 * <p>Enum riêng chứ không dùng thẳng enum sinh từ spec: đây là khái niệm nghiệp
 * vụ, và nó phải test được bằng JUnit thuần không cần một dòng nào của
 * OpenAPI. Cùng lý do với {@code BookingStatus}.
 */
public enum QuoteStatus {

    /** Khách vừa gửi yêu cầu; chưa ai dựng giá. */
    DRAFT,

    /** Đã gửi cho khách, đang trong hạn {@code valid_until}. */
    SENT,

    /** Khách đồng ý. Chỉ trạng thái này mới sinh được {@code Booking} (quy tắc 3). */
    ACCEPTED,

    /** Khách từ chối. */
    REJECTED,

    /** Quá hạn. Do job sinh ra, không phải quyết định của người (quy tắc 4). */
    EXPIRED
}
