package vn.travel.booking.quote.dto;

import java.time.LocalDate;

/**
 * Yêu cầu báo giá khách vừa gửi — docs/23 mục 7.
 *
 * <p>{@code productSlug} chứ không {@code productId}: bề mặt công khai dùng
 * slug (docs/13 mục 4), và slug <b>phụ thuộc locale</b> — cùng một tour có slug
 * khác nhau ở {@code da} và {@code vi}, nên phải tra kèm locale chứ không tra
 * một mình.
 *
 * @param requestedDate có thể {@code null}: khách chưa chốt ngày vẫn hỏi giá được
 */
public record QuoteRequestCommand(
        String productSlug,
        int partySize,
        LocalDate requestedDate,
        String contactName,
        String contactEmail,
        String contactPhone,
        String message) {
}
