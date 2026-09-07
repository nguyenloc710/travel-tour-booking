package vn.travel.booking.quote.dto;

/**
 * Bộ lọc của màn hình danh sách báo giá (docs/22 M8).
 *
 * <p>Khác {@code AdminBookingQuery} ở chỗ chỉ có <b>một</b> trục trạng thái:
 * danh sách đơn cần cả {@code scope} lẫn {@code status} vì "cần xử lý" gộp ba
 * trạng thái, còn ở đây phần việc đang nợ đúng bằng một trạng thái — {@code
 * DRAFT}. Thêm một khái niệm {@code scope} nữa chỉ để cho giống là thêm một thứ
 * phải giải thích mà không giải quyết gì.
 *
 * @param status một trạng thái, hoặc {@code "ALL"}
 */
public record AdminQuoteQuery(String status, String market, String q, int page, int size) {

    /** Giá trị {@code status} nghĩa là "đừng lọc" — không bao giờ nằm trong CSDL. */
    public static final String TAT_CA = "ALL";
}
