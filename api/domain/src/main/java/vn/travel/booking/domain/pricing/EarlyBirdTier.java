package vn.travel.booking.domain.pricing;

import vn.travel.booking.domain.shared.Money;

/**
 * Một bậc giảm đặt sớm: đặt trước ít nhất {@code monthsBefore} tháng thì mỗi
 * người được giảm {@code discountPerPerson}.
 *
 * <p>Hai bậc của thị trường {@code DK} ở docs/14 mục 2.4 là <b>đề xuất</b>, chưa
 * chốt — nên chúng là dữ liệu truyền vào, không phải hằng số trong lõi.
 */
public record EarlyBirdTier(int monthsBefore, Money discountPerPerson) {
}
