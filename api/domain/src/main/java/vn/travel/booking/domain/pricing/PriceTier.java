package vn.travel.booking.domain.pricing;

import vn.travel.booking.domain.shared.Money;

/**
 * Một bậc giá theo số khách — chỉ {@code PRIVATE_TOUR}.
 *
 * <p>{@code maxPax} là {@code null} ở bậc cuối: nhóm đông hơn nữa vẫn trả theo
 * đơn giá đó, không có trần.
 */
public record PriceTier(int minPax, Integer maxPax, Money pricePerPerson) {

    boolean chua(int soKhach) {
        return soKhach >= minPax && (maxPax == null || soKhach <= maxPax);
    }
}
