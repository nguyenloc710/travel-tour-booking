package vn.travel.booking.pricing.dto;

import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;

/**
 * Một loại khách trong đơn: bao nhiêu người, đơn giá bao nhiêu.
 *
 * <p>{@code discountRate} chỉ dùng cho bảng giá theo bậc của
 * {@code PRIVATE_TOUR}, nơi bậc cho ra một đơn giá chung rồi mới áp tỷ lệ của
 * từng loại khách (docs/14 mục 2.3). Với giá theo ngày khởi hành thì bảng
 * {@code departure_price} đã có đơn giá riêng cho từng loại, nên tỷ lệ ở đây là 0
 * — áp thêm lần nữa là giảm giá hai lần.
 */
public record PaxLine(String paxTypeCode, int count, Money unitPrice, BigDecimal discountRate) {

    public PaxLine {
        if (count < 0) {
    throw new IllegalArgumentException("Số khách không âm: " + count);
        }
    }

    public static PaxLine of(String paxTypeCode, int count, Money unitPrice) {
    return new PaxLine(paxTypeCode, count, unitPrice, BigDecimal.ZERO);
    }

    public Money thanhTien() {
        BigDecimal heSo = BigDecimal.ONE.subtract(discountRate == null ? BigDecimal.ZERO : discountRate);
        return unitPrice.times(heSo).times(BigDecimal.valueOf(count));
    }
}
