package vn.travel.booking.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test JUnit thuần — không context Spring, không CSDL, chạy trong vài mili giây.
 * Đây là tính chất mà module {@code domain} phải giữ được.
 */
class MoneyTest {

    @Test
    @DisplayName("DKK làm tròn 2 chữ số, VND làm tròn 0 — số chữ số lấy từ thị trường")
    void lamTronTheoSoChuSoCuaThiTruong() {
        assertEquals(new BigDecimal("24990.13"), Money.of("24990.125", "DKK").round(2).amount());
        assertEquals(new BigDecimal("18900001"), Money.of("18900000.5", "VND").round(0).amount());
    }

    @Test
    @DisplayName("Làm tròn từng dòng rồi cộng, không cộng rồi mới tròn")
    void lamTronTungDongRoiCong() {
        Money dong1 = Money.of("100.004", "DKK").round(2);
        Money dong2 = Money.of("100.004", "DKK").round(2);

        // Cộng số đã tròn: 100.00 + 100.00 = 200.00
        assertEquals(new BigDecimal("200.00"), dong1.plus(dong2).amount());

        // Cộng trước rồi tròn sẽ ra 200.01 — bảng phân rã khi đó không bằng tổng.
        Money congTruoc = Money.of("100.004", "DKK").plus(Money.of("100.004", "DKK")).round(2);
        assertEquals(new BigDecimal("200.01"), congTruoc.amount());
    }

    @Test
    @DisplayName("Đặt cọc làm tròn xuống, và cọc cộng phần còn lại bằng đúng tổng")
    void datCocLamTronXuong() {
        Money tong = Money.of("52760.00", "DKK");
        Money coc = tong.depositAt(new BigDecimal("0.25"), 2);

        assertEquals(new BigDecimal("13190.00"), coc.amount());
        assertEquals(tong.amount(), coc.plus(tong.minus(coc)).amount());
    }

    @Test
    @DisplayName("Đặt cọc lẻ vẫn làm tròn xuống, không lên")
    void datCocLeLamTronXuong() {
        Money coc = Money.of("999.99", "DKK").depositAt(new BigDecimal("0.25"), 2);
        assertEquals(new BigDecimal("249.99"), coc.amount());
    }

    @Test
    @DisplayName("Không cộng được hai loại tiền khác nhau")
    void khongCongDuocHaiLoaiTien() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of("100.00", "DKK").plus(Money.of("100.00", "VND")));
    }
}
