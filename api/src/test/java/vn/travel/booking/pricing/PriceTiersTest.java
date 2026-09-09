package vn.travel.booking.pricing;

import vn.travel.booking.common.exception.PartySizeOutOfRangeException;
import vn.travel.booking.pricing.dto.PaxLine;
import vn.travel.booking.pricing.dto.PriceBreakdown;
import vn.travel.booking.pricing.dto.PriceTier;
import vn.travel.booking.pricing.dto.PricingInput;
import vn.travel.booking.pricing.service.PriceTiers;
import vn.travel.booking.pricing.service.PricingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bảng giá theo bậc của {@code PRIVATE_TOUR} — docs/14 mục 2.3.
 */
class PriceTiersTest {

    private static final String DKK = "DKK";

    /** Bốn bậc, bậc cuối không có trần. Giá giảm dần theo số khách. */
    private static final List<PriceTier> TIERS = List.of(
            new PriceTier(2, 3, Money.of("38000.00", DKK)),
            new PriceTier(4, 7, Money.of("31000.00", DKK)),
            new PriceTier(8, 14, Money.of("27000.00", DKK)),
            new PriceTier(15, null, Money.of("24000.00", DKK)));

    @Test
    @DisplayName("Đúng bậc cho 2, 4, 8, 15 và 20 khách")
    void picksCorrectTier() {
        assertEquals(Money.of("38000.00", DKK), PriceTiers.unitPricePerPerson(TIERS, 2));
        assertEquals(Money.of("31000.00", DKK), PriceTiers.unitPricePerPerson(TIERS, 4));
        assertEquals(Money.of("27000.00", DKK), PriceTiers.unitPricePerPerson(TIERS, 8));
        assertEquals(Money.of("24000.00", DKK), PriceTiers.unitPricePerPerson(TIERS, 15));
        assertEquals(Money.of("24000.00", DKK), PriceTiers.unitPricePerPerson(TIERS, 20),
                "Bậc cuối không có trần: nhóm đông hơn vẫn trả đơn giá đó");
    }

    @Test
    @DisplayName("Số khách dưới bậc thấp nhất: ném lỗi, KHÔNG lấy bậc gần nhất")
    void belowLowestTierThrows() {
        PartySizeOutOfRangeException ex =
                assertThrows(PartySizeOutOfRangeException.class, () -> PriceTiers.unitPricePerPerson(TIERS, 1));

        assertEquals(1, ex.partySize(),
                "Đoán giùm nghĩa là bán một mức giá không ai từng duyệt");
    }

    @Test
    @DisplayName("Số khách rơi vào khoảng trống giữa hai bậc: cũng ném lỗi")
    void gapBetweenTiersThrows() {
        List<PriceTier> gap = List.of(
                new PriceTier(2, 3, Money.of("38000.00", DKK)),
                // Thiếu hẳn khoảng 4–7: lỗi dữ liệu do nhân viên nhập.
                new PriceTier(8, null, Money.of("27000.00", DKK)));

        assertThrows(PartySizeOutOfRangeException.class, () -> PriceTiers.unitPricePerPerson(gap, 5));
    }

    @Test
    @DisplayName("Trẻ em TÍNH VÀO số khách để chọn bậc, nhưng trả theo tỷ lệ của mình")
    void childrenCountTowardTierButPayRatio() {
        int adult = 2;
        int child = 2;
        int totalPax = adult + child;

        // Bốn người → bậc 4–7, đơn giá 31.000 cho MỌI người, kể cả trẻ em.
        Money unitPrice = PriceTiers.unitPricePerPerson(TIERS, totalPax);
        assertEquals(Money.of("31000.00", DKK), unitPrice);

        PriceBreakdown breakdown = PricingEngine.calculate(PricingInput.of(List.of(
                        new PaxLine("ADULT", adult, unitPrice, BigDecimal.ZERO),
                        new PaxLine("CHILD", child, unitPrice, new BigDecimal("0.25"))),
                2, new BigDecimal("0.2500")));

        // 2 × 31.000 + 2 × 31.000 × 0,75 = 62.000 + 46.500
        assertEquals(Money.of("108500.00", DKK), breakdown.total());
    }

    @Test
    @DisplayName("Bỏ trẻ em ra khỏi số khách khi chọn bậc là chọn SAI bậc, và đắt hơn")
    void excludingChildrenPicksWrongTier() {
        Money picksCorrectTier = PriceTiers.unitPricePerPerson(TIERS, 4);   // 2 người lớn + 2 trẻ em
        Money wrongTier = PriceTiers.unitPricePerPerson(TIERS, 2);    // chỉ đếm người lớn

        assertEquals(Money.of("31000.00", DKK), picksCorrectTier);
        assertEquals(Money.of("38000.00", DKK), wrongTier);
        assertEquals(1, wrongTier.amount().compareTo(picksCorrectTier.amount()),
                "Chọn sai bậc làm khách trả đắt hơn, và không ai hiểu vì sao");
    }
}
