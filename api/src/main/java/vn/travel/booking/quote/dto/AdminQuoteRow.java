package vn.travel.booking.quote.dto;

import vn.travel.booking.common.money.Money;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một dòng của danh sách báo giá (docs/22 M8).
 *
 * <p>{@code productTitle} đọc từ <b>bản dịch hiện tại</b> theo locale của báo
 * giá, khác hẳn {@code booking} vốn chụp lại tên lúc đặt. Lý do: đơn là chứng
 * từ nên tên phải đóng băng, còn báo giá chưa chốt gì — tên đúng của nó là tên
 * hôm nay, không phải tên hôm khách bấm nút.
 *
 * @param total      {@code null} khi chưa ai dựng bảng giá
 * @param validUntil {@code null} cho tới lúc gửi
 */
public record AdminQuoteRow(
        UUID id,
        String reference,
        QuoteStatus status,
        String market,
        String locale,
        UUID productId,
        String productTitle,
        int partySize,
        LocalDate requestedDate,
        String contactName,
        String contactEmail,
        Money total,
        LocalDate validUntil,
        OffsetDateTime createdAt) {
}
