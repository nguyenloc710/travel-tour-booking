package vn.travel.booking.pricing.service;

import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

/**
 * Chênh giá nâng hạng cabin — chỉ {@code CRUISE}, docs/14 mục 8.
 */
public final class CabinUpgrades {

    private CabinUpgrades() {
    }

    /**
     * Chênh so với <b>hạng thấp nhất của đúng ngày khởi hành đó</b>, không phải
     * một mức chênh cố định giữa các hạng.
     *
     * <p>Giá cabin dao động theo mùa, và chênh lệch giữa hai hạng cũng dao động
     * theo. Dùng một mức chênh cố định nghĩa là bán sai giá vào mùa cao điểm.
     */
    public static Money diffAgainstLowestTier(Map<String, Money> priceByTier, String selectedTier) {
        Money selectedPrice = priceByTier.get(selectedTier);
        if (selectedPrice == null) {
            throw new IllegalArgumentException("Không có hạng cabin: " + selectedTier);
        }
        Money lowestPrice = priceByTier.values().stream()
                .min(Comparator.comparing(Money::amount))
                .orElseThrow();

        return new Money(selectedPrice.amount().subtract(lowestPrice.amount()).max(BigDecimal.ZERO),
                selectedPrice.currency());
    }
}
