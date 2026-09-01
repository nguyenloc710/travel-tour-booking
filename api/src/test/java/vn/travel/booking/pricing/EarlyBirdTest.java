package vn.travel.booking.pricing;

import vn.travel.booking.pricing.dto.EarlyBirdTier;
import vn.travel.booking.pricing.service.EarlyBird;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.travel.booking.common.money.Money;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Giảm đặt sớm — hai bậc <b>đề xuất</b> của thị trường {@code DK}
 * (docs/14 mục 2.4). Chúng là dữ liệu truyền vào, nên test này vẫn đúng khi
 * nghiệp vụ chốt con số khác.
 */
class EarlyBirdTest {

    private static final String DKK = "DKK";

    private static final List<EarlyBirdTier> BAC = List.of(
            new EarlyBirdTier(6, Money.of("1000.00", DKK)),
            new EarlyBirdTier(3, Money.of("500.00", DKK)));

    /** Ngày cố định, không lấy từ đồng hồ hệ thống. */
    private static final LocalDate NGAY_DAT = LocalDate.of(2026, 9, 1);

    @Test
    @DisplayName("Đúng ngày tròn 6 tháng: được bậc cao nhất")
    void dungNguong6Thang() {
        assertEquals(Money.of("1000.00", DKK), giam(LocalDate.of(2027, 3, 1)));
    }

    @Test
    @DisplayName("Sớm hơn ngưỡng 6 tháng ĐÚNG MỘT NGÀY: rơi xuống bậc 3 tháng")
    void thieuMotNgay() {
        assertEquals(Money.of("500.00", DKK), giam(LocalDate.of(2027, 2, 28)),
                "Đây là chỗ lệch một ngày dễ xảy ra nhất — docs/14 mục 9.1");
    }

    @Test
    @DisplayName("Đúng ngày tròn 3 tháng: bậc thứ hai")
    void dungNguong3Thang() {
        assertEquals(Money.of("500.00", DKK), giam(LocalDate.of(2026, 12, 1)));
    }

    @Test
    @DisplayName("Sớm hơn ngưỡng 3 tháng một ngày: không được giảm")
    void thieuMotNgayOBacHai() {
        assertEquals(Money.of("0", DKK), giam(LocalDate.of(2026, 11, 30)));
    }

    @Test
    @DisplayName("Đặt sát ngày đi: không được giảm")
    void datSatNgay() {
        assertEquals(Money.of("0", DKK), giam(LocalDate.of(2026, 9, 20)));
    }

    @Test
    @DisplayName("Đặt trước một năm: vẫn là bậc cao nhất, không cộng dồn hai bậc")
    void datRatSom() {
        assertEquals(Money.of("1000.00", DKK), giam(LocalDate.of(2027, 9, 1)),
                "Hai bậc là hai mức thay thế nhau, không phải hai khoản cộng lại");
    }

    @Test
    @DisplayName("Thị trường không áp dụng giảm đặt sớm: danh sách bậc rỗng")
    void khongApDung() {
        assertEquals(Money.of("0", "VND"),
                EarlyBird.mucGiamMoiNguoi(NGAY_DAT, LocalDate.of(2028, 1, 1), List.of(), "VND"),
                "Thị trường VN không áp dụng giảm đặt sớm — docs/14 mục 2.4");
    }

    private static Money giam(LocalDate ngayKhoiHanh) {
        return EarlyBird.mucGiamMoiNguoi(NGAY_DAT, ngayKhoiHanh, BAC, DKK);
    }
}
