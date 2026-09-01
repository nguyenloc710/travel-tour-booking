package vn.travel.booking.domain.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.travel.booking.domain.shared.Money;

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
    private static final List<PriceTier> BAC = List.of(
            new PriceTier(2, 3, Money.of("38000.00", DKK)),
            new PriceTier(4, 7, Money.of("31000.00", DKK)),
            new PriceTier(8, 14, Money.of("27000.00", DKK)),
            new PriceTier(15, null, Money.of("24000.00", DKK)));

    @Test
    @DisplayName("Đúng bậc cho 2, 4, 8, 15 và 20 khách")
    void dungBac() {
        assertEquals(Money.of("38000.00", DKK), PriceTiers.donGiaMoiNguoi(BAC, 2));
        assertEquals(Money.of("31000.00", DKK), PriceTiers.donGiaMoiNguoi(BAC, 4));
        assertEquals(Money.of("27000.00", DKK), PriceTiers.donGiaMoiNguoi(BAC, 8));
        assertEquals(Money.of("24000.00", DKK), PriceTiers.donGiaMoiNguoi(BAC, 15));
        assertEquals(Money.of("24000.00", DKK), PriceTiers.donGiaMoiNguoi(BAC, 20),
                "Bậc cuối không có trần: nhóm đông hơn vẫn trả đơn giá đó");
    }

    @Test
    @DisplayName("Số khách dưới bậc thấp nhất: ném lỗi, KHÔNG lấy bậc gần nhất")
    void duoiBacThapNhat() {
        PartySizeOutOfRangeException loi =
                assertThrows(PartySizeOutOfRangeException.class, () -> PriceTiers.donGiaMoiNguoi(BAC, 1));

        assertEquals(1, loi.partySize(),
                "Đoán giùm nghĩa là bán một mức giá không ai từng duyệt");
    }

    @Test
    @DisplayName("Số khách rơi vào khoảng trống giữa hai bậc: cũng ném lỗi")
    void khoangTrongGiuaHaiBac() {
        List<PriceTier> thung = List.of(
                new PriceTier(2, 3, Money.of("38000.00", DKK)),
                // Thiếu hẳn khoảng 4–7: lỗi dữ liệu do nhân viên nhập.
                new PriceTier(8, null, Money.of("27000.00", DKK)));

        assertThrows(PartySizeOutOfRangeException.class, () -> PriceTiers.donGiaMoiNguoi(thung, 5));
    }

    @Test
    @DisplayName("Trẻ em TÍNH VÀO số khách để chọn bậc, nhưng trả theo tỷ lệ của mình")
    void treEmTinhVaoSoKhachNhungTraTheoTyLe() {
        int nguoiLon = 2;
        int treEm = 2;
        int tongSoKhach = nguoiLon + treEm;

        // Bốn người → bậc 4–7, đơn giá 31.000 cho MỌI người, kể cả trẻ em.
        Money donGia = PriceTiers.donGiaMoiNguoi(BAC, tongSoKhach);
        assertEquals(Money.of("31000.00", DKK), donGia);

        PriceBreakdown kq = PricingEngine.tinh(PricingInput.cua(List.of(
                        new PaxLine("ADULT", nguoiLon, donGia, BigDecimal.ZERO),
                        new PaxLine("CHILD", treEm, donGia, new BigDecimal("0.25"))),
                2, new BigDecimal("0.2500")));

        // 2 × 31.000 + 2 × 31.000 × 0,75 = 62.000 + 46.500
        assertEquals(Money.of("108500.00", DKK), kq.total());
    }

    @Test
    @DisplayName("Bỏ trẻ em ra khỏi số khách khi chọn bậc là chọn SAI bậc, và đắt hơn")
    void boTreEmRaLaChonSaiBac() {
        Money dungBac = PriceTiers.donGiaMoiNguoi(BAC, 4);   // 2 người lớn + 2 trẻ em
        Money saiBac = PriceTiers.donGiaMoiNguoi(BAC, 2);    // chỉ đếm người lớn

        assertEquals(Money.of("31000.00", DKK), dungBac);
        assertEquals(Money.of("38000.00", DKK), saiBac);
        assertEquals(1, saiBac.amount().compareTo(dungBac.amount()),
                "Chọn sai bậc làm khách trả đắt hơn, và không ai hiểu vì sao");
    }
}
