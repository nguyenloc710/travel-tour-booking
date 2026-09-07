package vn.travel.booking.quote.dto;

import java.time.OffsetDateTime;

/**
 * Thứ khách nhận lại sau khi gửi yêu cầu — <b>chỉ ba trường</b>.
 *
 * <p>Không trả về cả bản ghi báo giá: lúc này nó chưa có giá nào, và trả một
 * đối tượng rỗng ruột là mời frontend hiển thị "Tổng: —" cho khách xem.
 */
public record QuoteReceiptView(String reference, QuoteStatus status, OffsetDateTime createdAt) {
}
