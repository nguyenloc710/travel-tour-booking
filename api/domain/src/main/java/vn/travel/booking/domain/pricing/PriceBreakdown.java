package vn.travel.booking.domain.pricing;

import vn.travel.booking.domain.shared.Money;

import java.util.List;

/**
 * Kết quả tính giá.
 *
 * <p>{@code deposit + balance} <b>luôn</b> bằng {@code total}: đặt cọc làm tròn
 * xuống rồi phần còn lại lấy bằng hiệu, thay vì làm tròn cả hai rồi hy vọng
 * chúng khớp (docs/14 mục 3 quy tắc 4).
 */
public record PriceBreakdown(List<PriceLine> lines, Money total, Money deposit, Money balance) {

    /** Tổng các dòng — dùng cho test bất biến "các dòng cộng lại bằng tổng". */
    public Money sumOfLines() {
        return lines.stream()
                .map(PriceLine::amount)
                .reduce(Money::plus)
                .orElse(total);
    }
}
