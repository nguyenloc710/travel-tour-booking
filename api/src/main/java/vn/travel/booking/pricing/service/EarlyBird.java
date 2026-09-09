package vn.travel.booking.pricing.service;

import vn.travel.booking.pricing.dto.EarlyBirdTier;
import vn.travel.booking.common.money.Money;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Chọn bậc giảm đặt sớm.
 *
 * <p><b>Nhận ngày làm tham số, không đọc đồng hồ hệ thống</b> — api/CLAUDE.md
 * mục 1. Test đọc đồng hồ thật sẽ đỏ vào một ngày nào đó trong tương lai mà
 * không ai hiểu vì sao.
 */
public final class EarlyBird {

    private EarlyBird() {
    }

    /**
     * Bậc cao nhất mà đơn đạt được; không đạt bậc nào thì trả 0.
     *
     * <p>Ngưỡng tính bằng <b>không sớm hơn</b>: đặt đúng ngày tròn 6 tháng thì
     * được giảm, đặt sau đó <b>một ngày</b> thì không. Ranh giới này có test
     * riêng vì nó là chỗ dễ lệch một ngày nhất (docs/14 mục 9.1).
     */
    public static Money discountPerPerson(LocalDate bookedAt, LocalDate departureDate,
                                        List<EarlyBirdTier> bac, String currency) {
        return bac.stream()
                .sorted(Comparator.comparingInt(EarlyBirdTier::monthsBefore).reversed())
                .filter(b -> !departureDate.isBefore(bookedAt.plusMonths(b.monthsBefore())))
                .findFirst()
                .map(EarlyBirdTier::discountPerPerson)
                .orElse(Money.of("0", currency));
    }
}
