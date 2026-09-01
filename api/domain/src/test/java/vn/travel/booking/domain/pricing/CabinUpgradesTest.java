package vn.travel.booking.domain.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.travel.booking.domain.shared.Money;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Nâng hạng cabin — chỉ {@code CRUISE}, docs/14 mục 8.
 */
class CabinUpgradesTest {

    private static final String DKK = "DKK";

    /** Giá bốn hạng của MỘT ngày khởi hành cụ thể. */
    private static Map<String, Money> ngayThang3() {
        Map<String, Money> gia = new LinkedHashMap<>();
        gia.put("INSIDE", Money.of("8990.00", DKK));
        gia.put("OUTSIDE", Money.of("10490.00", DKK));
        gia.put("BALCONY", Money.of("12290.00", DKK));
        gia.put("AQUA", Money.of("13990.00", DKK));
        return gia;
    }

    @Test
    @DisplayName("Chênh tính so với hạng THẤP NHẤT của đúng ngày đó")
    void chenhSoVoiHangThapNhat() {
        assertEquals(Money.of("3300.00", DKK),
                CabinUpgrades.chenhSoVoiHangThapNhat(ngayThang3(), "BALCONY"));
    }

    @Test
    @DisplayName("Chọn chính hạng thấp nhất thì chênh bằng 0, và dòng đó biến mất khỏi bảng phân rã")
    void chonHangThapNhat() {
        Money chenh = CabinUpgrades.chenhSoVoiHangThapNhat(ngayThang3(), "INSIDE");
        assertEquals(0, chenh.amount().signum());

        PriceBreakdown kq = PricingEngine.tinh(
                PricingInput.cua(java.util.List.of(PaxLine.of("ADULT", 2, Money.of("8990.00", DKK))),
                                2, new java.math.BigDecimal("0.2500"))
                        .nangHangCabin(chenh));

        assertEquals(1, kq.lines().size(), "Không hiện dòng nâng hạng bằng 0");
    }

    @Test
    @DisplayName("Cùng một hạng, hai ngày khởi hành khác nhau cho hai mức chênh khác nhau")
    void chenhDaoDongTheoNgay() {
        Map<String, Money> mua_cao_diem = new LinkedHashMap<>();
        mua_cao_diem.put("INSIDE", Money.of("11990.00", DKK));
        mua_cao_diem.put("BALCONY", Money.of("17990.00", DKK));

        assertEquals(Money.of("3300.00", DKK),
                CabinUpgrades.chenhSoVoiHangThapNhat(ngayThang3(), "BALCONY"));
        assertEquals(Money.of("6000.00", DKK),
                CabinUpgrades.chenhSoVoiHangThapNhat(mua_cao_diem, "BALCONY"));

        // Đây là lý do không dùng một mức chênh cố định giữa hai hạng: mùa cao
        // điểm chênh gấp gần hai lần, và bán theo mức cũ là bán lỗ.
    }

    @Test
    @DisplayName("Hạng không có trong ngày đó thì báo lỗi, không im lặng trả 0")
    void hangKhongTonTai() {
        assertThrows(IllegalArgumentException.class,
                () -> CabinUpgrades.chenhSoVoiHangThapNhat(ngayThang3(), "SUITE"));
    }
}
