package vn.travel.booking.quote.dto;

import java.math.BigDecimal;

/**
 * Một dòng bảng giá tư vấn viên vừa gõ.
 *
 * <p>{@code labelKey} là <b>khoá chuỗi</b>, không phải câu tiếng người — cùng
 * luật với {@code booking_line}. Nhờ đó một báo giá in ra được ở cả hai ngôn
 * ngữ mà không phải dựng lại.
 *
 * @param amount âm = giảm trừ
 */
public record QuoteLineDraft(
        String labelKey,
        BigDecimal quantity,
        BigDecimal unitAmount,
        BigDecimal amount) {
}
