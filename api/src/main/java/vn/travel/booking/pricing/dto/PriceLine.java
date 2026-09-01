package vn.travel.booking.pricing.dto;

import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;

/**
 * Một dòng của bảng phân rã.
 *
 * <p>{@code labelKey} là <b>khoá chuỗi</b>, không phải câu tiếng người: cùng một
 * đơn in ra được ở cả hai ngôn ngữ, và sửa câu chữ không đụng tới đơn đã đặt
 * (docs/13 mục 5).
 *
 * <p>{@code amount} đã <b>làm tròn</b> theo số chữ số thập phân của thị trường.
 * Tổng là tổng của các số đã tròn, nên bảng phân rã cộng lại luôn bằng đúng tổng
 * — docs/14 mục 3 quy tắc 3.
 */
public record PriceLine(
        PriceLineKind kind,
        String labelKey,
        BigDecimal quantity,
        Money unitAmount,
        Money amount) {

    public boolean isZero() {
        return amount.amount().signum() == 0;
    }
}
