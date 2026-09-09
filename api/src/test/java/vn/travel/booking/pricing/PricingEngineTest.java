package vn.travel.booking.pricing;

import vn.travel.booking.pricing.dto.PaxLine;
import vn.travel.booking.pricing.dto.PriceBreakdown;
import vn.travel.booking.pricing.dto.PriceLine;
import vn.travel.booking.pricing.dto.PriceLineKind;
import vn.travel.booking.pricing.dto.PricingInput;
import vn.travel.booking.pricing.service.PricingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine giá — danh sách test bắt buộc ở docs/14 mục 9.1.
 *
 * <p>JUnit thuần: không context Spring, không cơ sở dữ liệu, không đồng hồ hệ
 * thống. Chạy trong vài mili giây, và đỏ vì đúng một lý do.
 *
 * <p>Con số của thị trường {@code DK} lấy từ docs/14 mục 2.4 và ví dụ ở mục 4.
 * Thị trường {@code VN} <b>không có test giá trị thật</b> — sáu con số của nó
 * chưa ai quyết (Q-2). Ở đây chỉ kiểm những gì không phụ thuộc con số: quy tắc
 * làm tròn 0 chữ số thập phân và bất biến đặt cọc.
 */
class PricingEngineTest {

    private static final String DKK = "DKK";
    private static final String VND = "VND";
    private static final int DKK_FRACTION_DIGITS = 2;
    private static final int VND_FRACTION_DIGITS = 0;
    private static final BigDecimal DK_DEPOSIT_RATE = new BigDecimal("0.2500");

    private static Money kr(String amount) {
        return Money.of(amount, DKK);
    }

    private static Money vnd(String amount) {
        return Money.of(amount, VND);
    }

    // ------------------------------------------------------------ cơ bản

    @Test
    @DisplayName("Hai người phòng đôi, không tuỳ chọn nào")
    void twoPaxDoubleRoom() {
        PriceBreakdown breakdown = PricingEngine.calculate(
                PricingInput.of(List.of(PaxLine.of("ADULT", 2, kr("24990.00"))), DKK_FRACTION_DIGITS, DK_DEPOSIT_RATE));

        assertEquals(1, breakdown.lines().size(), "Không tuỳ chọn nào thì chỉ có một dòng giá cơ bản");
        assertEquals(kr("49980.00"), breakdown.total());
    }

    @Test
    @DisplayName("Một người ở phòng đơn thì có dòng phụ thu")
    void soloPaxAddsSingleSupplement() {
        PriceBreakdown breakdown = PricingEngine.calculate(
                PricingInput.of(List.of(PaxLine.of("ADULT", 1, kr("24990.00"))), DKK_FRACTION_DIGITS, DK_DEPOSIT_RATE)
                        .singleSupplement(1, kr("4500.00")));

        assertEquals(kr("29490.00"), breakdown.total());
        assertTrue(hasLine(breakdown, PriceLineKind.SINGLE_SUPPLEMENT));
    }

    @Test
    @DisplayName("Ví dụ đủ một đơn của docs/14 mục 4 ra đúng 52.760 kr")
    void fullWorkedExample() {
        PriceBreakdown breakdown = PricingEngine.calculate(fullBooking());

        assertEquals(kr("52760.00"), breakdown.total());
        assertEquals(kr("13190.00"), breakdown.deposit());
        assertEquals(kr("39570.00"), breakdown.balance());
    }

    // ------------------------------------------------------------ thứ tự

    @Test
    @DisplayName("Giảm đặt sớm trừ TRƯỚC phí xử lý — thứ tự nằm trong kết quả, không chỉ trong tài liệu")
    void discountAppliedBeforeHandlingFee() {
        List<PriceLineKind> order = PricingEngine.calculate(fullBooking()).lines().stream()
                .map(PriceLine::kind)
                .toList();

        int discountIndex = order.indexOf(PriceLineKind.EARLY_BIRD_DISCOUNT);
        int feeIndex = order.indexOf(PriceLineKind.PROCESSING_FEE);

        assertTrue(discountIndex >= 0 && feeIndex > discountIndex,
                "Đảo hai dòng cuối ra con số khác — docs/14 mục 2.1");
    }

    @Test
    @DisplayName("Tám dòng giữ đúng thứ tự cộng dồn của docs/14 mục 2.1")
    void eightLinesKeepAccumulationOrder() {
        List<PriceLineKind> order = PricingEngine.calculate(fullBooking()).lines().stream()
                .map(PriceLine::kind)
                .toList();

        assertEquals(List.of(
                PriceLineKind.BASE,
                PriceLineKind.DEPARTURE_ORIGIN,
                PriceLineKind.INSURANCE,
                PriceLineKind.PRE_TOUR_HOTEL,
                PriceLineKind.EARLY_BIRD_DISCOUNT,
                PriceLineKind.PROCESSING_FEE), order);
    }

    // ------------------------------------------------------------ dòng rỗng

    @Test
    @DisplayName("Dòng bằng 0 KHÔNG xuất hiện trong bảng phân rã")
    void zeroLinesAreOmitted() {
        PriceBreakdown breakdown = PricingEngine.calculate(
                PricingInput.of(List.of(PaxLine.of("ADULT", 2, kr("24990.00"))), DKK_FRACTION_DIGITS, DK_DEPOSIT_RATE)
                        // Không ai ở phòng đơn: dòng phụ thu ra 0.
                        .singleSupplement(0, kr("4500.00"))
                        .insurance(kr("0.00")));

        assertFalse(hasLine(breakdown, PriceLineKind.SINGLE_SUPPLEMENT), "Không hiện dòng 0 kr.");
        assertFalse(hasLine(breakdown, PriceLineKind.INSURANCE));
        assertEquals(1, breakdown.lines().size());
    }

    @Test
    @DisplayName("Dòng không áp dụng cho loại sản phẩm thì không có mặt")
    void inapplicableLinesAreAbsent() {
        // PRIVATE_TOUR: không phụ thu phòng đơn, không nâng cabin, không giảm đặt sớm.
        PriceBreakdown breakdown = PricingEngine.calculate(
                PricingInput.of(List.of(PaxLine.of("ADULT", 4, kr("31000.00"))), DKK_FRACTION_DIGITS, DK_DEPOSIT_RATE)
                        .processingFee(kr("295.00")));

        assertEquals(2, breakdown.lines().size());
        assertFalse(hasLine(breakdown, PriceLineKind.CABIN_UPGRADE));
        assertFalse(hasLine(breakdown, PriceLineKind.EARLY_BIRD_DISCOUNT));
    }

    // ------------------------------------------------------------ làm tròn

    @Nested
    @DisplayName("Làm tròn")
    class Rounding {

        @Test
        @DisplayName("Làm tròn TỪNG DÒNG: các dòng cộng lại bằng đúng tổng")
        void roundedLinesSumToTotal() {
            // Đơn giá lẻ để phép nhân sinh phần thập phân thứ ba.
            PriceBreakdown breakdown = PricingEngine.calculate(
                    PricingInput.of(List.of(
                                    new PaxLine("ADULT", 3, kr("1333.333"), BigDecimal.ZERO),
                                    new PaxLine("CHILD", 2, kr("999.999"), BigDecimal.ZERO)),
                            DKK_FRACTION_DIGITS, DK_DEPOSIT_RATE)
                            .insurance(kr("333.333"))
                            .processingFee(kr("295.00")));

            assertEquals(breakdown.total(), breakdown.sumOfLines(),
                    "Cộng số chưa tròn rồi tròn một lần thì bảng phân rã cộng lại không bằng tổng");
        }

        @Test
        @DisplayName("VND có 0 chữ số thập phân: không dòng nào có phần lẻ")
        void vndHasNoFractionalPart() {
            PriceBreakdown breakdown = PricingEngine.calculate(
                    PricingInput.of(List.of(
                                    new PaxLine("ADULT", 2, vnd("18900000"), BigDecimal.ZERO),
                                    new PaxLine("CHILD", 1, vnd("4290000"), new BigDecimal("0.25"))),
                            VND_FRACTION_DIGITS, BigDecimal.ZERO));

            for (PriceLine line : breakdown.lines()) {
                assertEquals(0, line.amount().amount().scale(),
                        "fraction_digits = 0 nghĩa là làm tròn tới ĐỒNG");
            }
            assertEquals(0, breakdown.total().amount().scale());
        }

        @Test
        @DisplayName("Giảm 25% trên 4.290.000 ra 3.217.500 — không làm tròn tới nghìn")
        void doesNotRoundToThousands() {
            PriceBreakdown breakdown = PricingEngine.calculate(
                    PricingInput.of(List.of(
                                    new PaxLine("CHILD", 1, vnd("4290000"), new BigDecimal("0.25"))),
                            VND_FRACTION_DIGITS, BigDecimal.ZERO));

            assertEquals(vnd("3217500"), breakdown.total(),
                    "Làm tròn tới nghìn đồng là một quy tắc KHÁC, chưa ai chốt — docs/14 mục 3");
        }

        @Test
        @DisplayName("Số chữ số lấy từ tham số, không hardcode: cùng đầu vào, hai thị trường hai kết quả")
        void fractionDigitsComeFromParams() {
            List<PaxLine> pax = List.of(new PaxLine("ADULT", 3, kr("1000.335"), BigDecimal.ZERO));

            assertEquals(new BigDecimal("3001.01"),
                    PricingEngine.calculate(PricingInput.of(pax, 2, DK_DEPOSIT_RATE)).total().amount());
            assertEquals(new BigDecimal("3001"),
                    PricingEngine.calculate(PricingInput.of(pax, 0, DK_DEPOSIT_RATE)).total().amount());
        }
    }

    // ------------------------------------------------------------ đặt cọc

    @Nested
    @DisplayName("Đặt cọc và phần còn lại")
    class Deposit {

        @Test
        @DisplayName("deposit + balance = total, tiền DKK")
        void depositPlusBalanceEqualsTotalDkk() {
            PriceBreakdown breakdown = PricingEngine.calculate(fullBooking());
            assertEquals(breakdown.total(), breakdown.deposit().plus(breakdown.balance()));
        }

        @Test
        @DisplayName("deposit + balance = total, tiền VND")
        void depositPlusBalanceEqualsTotalVnd() {
            PriceBreakdown breakdown = PricingEngine.calculate(
                    PricingInput.of(List.of(PaxLine.of("ADULT", 3, vnd("18900001"))),
                            VND_FRACTION_DIGITS, new BigDecimal("0.3333")));

            assertEquals(breakdown.total(), breakdown.deposit().plus(breakdown.balance()));
        }

        @Test
        @DisplayName("Đặt cọc làm tròn XUỐNG, phần còn lại lấy bằng hiệu")
        void depositRoundsDown() {
            PriceBreakdown breakdown = PricingEngine.calculate(
                    PricingInput.of(List.of(PaxLine.of("ADULT", 1, kr("999.99"))),
                            DKK_FRACTION_DIGITS, new BigDecimal("0.3333")));

            // 999.99 × 0.3333 = 333.296667 → FLOOR ở 2 chữ số = 333.29
            assertEquals(kr("333.29"), breakdown.deposit());
            assertEquals(kr("666.70"), breakdown.balance());
            assertEquals(breakdown.total(), breakdown.deposit().plus(breakdown.balance()));
        }

        @Test
        @DisplayName("Tỷ lệ đặt cọc 0 — thị trường chưa chốt con số vẫn tính được đơn")
        void zeroDepositRate() {
            PriceBreakdown breakdown = PricingEngine.calculate(
                    PricingInput.of(List.of(PaxLine.of("ADULT", 2, vnd("18900000"))),
                            VND_FRACTION_DIGITS, BigDecimal.ZERO));

            assertEquals(vnd("0"), breakdown.deposit());
            assertEquals(breakdown.total(), breakdown.balance(),
                    "Engine nhận hằng số làm tham số nên thiếu con số nghiệp vụ không chặn được nó");
        }
    }

    // ------------------------------------------------------------ tiện ích

    /** Đúng ví dụ ở docs/14 mục 4. */
    private static PricingInput fullBooking() {
        return PricingInput.of(List.of(PaxLine.of("ADULT", 2, kr("24990.00"))), DKK_FRACTION_DIGITS, DK_DEPOSIT_RATE)
                .singleSupplement(0, kr("4500.00"))
                .departureOriginSurcharge(kr("800.00"))
                .insurance(kr("895.00"))
                .preTourHotel(1, 1, kr("1095.00"))
                .earlyBirdDiscount(kr("1000.00"))
                .processingFee(kr("295.00"));
    }

    private static boolean hasLine(PriceBreakdown breakdown, PriceLineKind type) {
        return breakdown.lines().stream().anyMatch(d -> d.kind() == type);
    }
}
