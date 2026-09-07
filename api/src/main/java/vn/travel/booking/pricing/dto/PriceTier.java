package vn.travel.booking.pricing.dto;

import vn.travel.booking.common.money.Money;

/**
 * Một bậc giá theo số khách — chỉ {@code PRIVATE_TOUR}.
 *
 * <p>{@code maxPax} là {@code null} ở bậc cuối: nhóm đông hơn nữa vẫn trả theo
 * đơn giá đó, không có trần.
 */
public record PriceTier(int minPax, Integer maxPax, Money pricePerPerson) {

    public boolean chua(int soKhach) {
        return soKhach >= minPax && (maxPax == null || soKhach <= maxPax);
    }
}
