package vn.travel.booking.domain.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.travel.booking.domain.shared.Money;

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
    private static final int SO_LE_DKK = 2;
    private static final int SO_LE_VND = 0;
    private static final BigDecimal COC_DK = new BigDecimal("0.2500");

    private static Money kr(String so) {
        return Money.of(so, DKK);
    }

    private static Money dong(String so) {
        return Money.of(so, VND);
    }

    // ------------------------------------------------------------ cơ bản

    @Test
    @DisplayName("Hai người phòng đôi, không tuỳ chọn nào")
    void haiNguoiPhongDoi() {
        PriceBreakdown kq = PricingEngine.tinh(
                PricingInput.cua(List.of(PaxLine.of("ADULT", 2, kr("24990.00"))), SO_LE_DKK, COC_DK));

        assertEquals(1, kq.lines().size(), "Không tuỳ chọn nào thì chỉ có một dòng giá cơ bản");
        assertEquals(kr("49980.00"), kq.total());
    }

    @Test
    @DisplayName("Một người ở phòng đơn thì có dòng phụ thu")
    void motNguoiPhongDon() {
        PriceBreakdown kq = PricingEngine.tinh(
                PricingInput.cua(List.of(PaxLine.of("ADULT", 1, kr("24990.00"))), SO_LE_DKK, COC_DK)
                        .phongDon(1, kr("4500.00")));

        assertEquals(kr("29490.00"), kq.total());
        assertTrue(coDong(kq, PriceLineKind.SINGLE_SUPPLEMENT));
    }

    @Test
    @DisplayName("Ví dụ đủ một đơn của docs/14 mục 4 ra đúng 52.760 kr")
    void viDuDayDu() {
        PriceBreakdown kq = PricingEngine.tinh(donDayDu());

        assertEquals(kr("52760.00"), kq.total());
        assertEquals(kr("13190.00"), kq.deposit());
        assertEquals(kr("39570.00"), kq.balance());
    }

    // ------------------------------------------------------------ thứ tự

    @Test
    @DisplayName("Giảm đặt sớm trừ TRƯỚC phí xử lý — thứ tự nằm trong kết quả, không chỉ trong tài liệu")
    void thuTuGiamTruocPhi() {
        List<PriceLineKind> thuTu = PricingEngine.tinh(donDayDu()).lines().stream()
                .map(PriceLine::kind)
                .toList();

        int viTriGiam = thuTu.indexOf(PriceLineKind.EARLY_BIRD_DISCOUNT);
        int viTriPhi = thuTu.indexOf(PriceLineKind.PROCESSING_FEE);

        assertTrue(viTriGiam >= 0 && viTriPhi > viTriGiam,
                "Đảo hai dòng cuối ra con số khác — docs/14 mục 2.1");
    }

    @Test
    @DisplayName("Tám dòng giữ đúng thứ tự cộng dồn của docs/14 mục 2.1")
    void thuTuTamDong() {
        List<PriceLineKind> thuTu = PricingEngine.tinh(donDayDu()).lines().stream()
                .map(PriceLine::kind)
                .toList();

        assertEquals(List.of(
                PriceLineKind.BASE,
                PriceLineKind.DEPARTURE_ORIGIN,
                PriceLineKind.INSURANCE,
                PriceLineKind.PRE_TOUR_HOTEL,
                PriceLineKind.EARLY_BIRD_DISCOUNT,
                PriceLineKind.PROCESSING_FEE), thuTu);
    }

    // ------------------------------------------------------------ dòng rỗng

    @Test
    @DisplayName("Dòng bằng 0 KHÔNG xuất hiện trong bảng phân rã")
    void dongBangKhongBienMat() {
        PriceBreakdown kq = PricingEngine.tinh(
                PricingInput.cua(List.of(PaxLine.of("ADULT", 2, kr("24990.00"))), SO_LE_DKK, COC_DK)
                        // Không ai ở phòng đơn: dòng phụ thu ra 0.
                        .phongDon(0, kr("4500.00"))
                        .baoHiem(kr("0.00")));

        assertFalse(coDong(kq, PriceLineKind.SINGLE_SUPPLEMENT), "Không hiện dòng 0 kr.");
        assertFalse(coDong(kq, PriceLineKind.INSURANCE));
        assertEquals(1, kq.lines().size());
    }

    @Test
    @DisplayName("Dòng không áp dụng cho loại sản phẩm thì không có mặt")
    void dongKhongApDung() {
        // PRIVATE_TOUR: không phụ thu phòng đơn, không nâng cabin, không giảm đặt sớm.
        PriceBreakdown kq = PricingEngine.tinh(
                PricingInput.cua(List.of(PaxLine.of("ADULT", 4, kr("31000.00"))), SO_LE_DKK, COC_DK)
                        .phiXuLy(kr("295.00")));

        assertEquals(2, kq.lines().size());
        assertFalse(coDong(kq, PriceLineKind.CABIN_UPGRADE));
        assertFalse(coDong(kq, PriceLineKind.EARLY_BIRD_DISCOUNT));
    }

    // ------------------------------------------------------------ làm tròn

    @Nested
    @DisplayName("Làm tròn")
    class LamTron {

        @Test
        @DisplayName("Làm tròn TỪNG DÒNG: các dòng cộng lại bằng đúng tổng")
        void congTungDongBangTong() {
            // Đơn giá lẻ để phép nhân sinh phần thập phân thứ ba.
            PriceBreakdown kq = PricingEngine.tinh(
                    PricingInput.cua(List.of(
                                    new PaxLine("ADULT", 3, kr("1333.333"), BigDecimal.ZERO),
                                    new PaxLine("CHILD", 2, kr("999.999"), BigDecimal.ZERO)),
                            SO_LE_DKK, COC_DK)
                            .baoHiem(kr("333.333"))
                            .phiXuLy(kr("295.00")));

            assertEquals(kq.total(), kq.sumOfLines(),
                    "Cộng số chưa tròn rồi tròn một lần thì bảng phân rã cộng lại không bằng tổng");
        }

        @Test
        @DisplayName("VND có 0 chữ số thập phân: không dòng nào có phần lẻ")
        void vndKhongCoPhanLe() {
            PriceBreakdown kq = PricingEngine.tinh(
                    PricingInput.cua(List.of(
                                    new PaxLine("ADULT", 2, dong("18900000"), BigDecimal.ZERO),
                                    new PaxLine("CHILD", 1, dong("4290000"), new BigDecimal("0.25"))),
                            SO_LE_VND, BigDecimal.ZERO));

            for (PriceLine d : kq.lines()) {
                assertEquals(0, d.amount().amount().scale(),
                        "fraction_digits = 0 nghĩa là làm tròn tới ĐỒNG");
            }
            assertEquals(0, kq.total().amount().scale());
        }

        @Test
        @DisplayName("Giảm 25% trên 4.290.000 ra 3.217.500 — không làm tròn tới nghìn")
        void khongTuLamTronToiNghin() {
            PriceBreakdown kq = PricingEngine.tinh(
                    PricingInput.cua(List.of(
                                    new PaxLine("CHILD", 1, dong("4290000"), new BigDecimal("0.25"))),
                            SO_LE_VND, BigDecimal.ZERO));

            assertEquals(dong("3217500"), kq.total(),
                    "Làm tròn tới nghìn đồng là một quy tắc KHÁC, chưa ai chốt — docs/14 mục 3");
        }

        @Test
        @DisplayName("Số chữ số lấy từ tham số, không hardcode: cùng đầu vào, hai thị trường hai kết quả")
        void soLeLayTuThamSo() {
            List<PaxLine> pax = List.of(new PaxLine("ADULT", 3, kr("1000.335"), BigDecimal.ZERO));

            assertEquals(new BigDecimal("3001.01"),
                    PricingEngine.tinh(PricingInput.cua(pax, 2, COC_DK)).total().amount());
            assertEquals(new BigDecimal("3001"),
                    PricingEngine.tinh(PricingInput.cua(pax, 0, COC_DK)).total().amount());
        }
    }

    // ------------------------------------------------------------ đặt cọc

    @Nested
    @DisplayName("Đặt cọc và phần còn lại")
    class DatCoc {

        @Test
        @DisplayName("deposit + balance = total, tiền DKK")
        void batBienDkk() {
            PriceBreakdown kq = PricingEngine.tinh(donDayDu());
            assertEquals(kq.total(), kq.deposit().plus(kq.balance()));
        }

        @Test
        @DisplayName("deposit + balance = total, tiền VND")
        void batBienVnd() {
            PriceBreakdown kq = PricingEngine.tinh(
                    PricingInput.cua(List.of(PaxLine.of("ADULT", 3, dong("18900001"))),
                            SO_LE_VND, new BigDecimal("0.3333")));

            assertEquals(kq.total(), kq.deposit().plus(kq.balance()));
        }

        @Test
        @DisplayName("Đặt cọc làm tròn XUỐNG, phần còn lại lấy bằng hiệu")
        void datCocLamTronXuong() {
            PriceBreakdown kq = PricingEngine.tinh(
                    PricingInput.cua(List.of(PaxLine.of("ADULT", 1, kr("999.99"))),
                            SO_LE_DKK, new BigDecimal("0.3333")));

            // 999.99 × 0.3333 = 333.296667 → FLOOR ở 2 chữ số = 333.29
            assertEquals(kr("333.29"), kq.deposit());
            assertEquals(kr("666.70"), kq.balance());
            assertEquals(kq.total(), kq.deposit().plus(kq.balance()));
        }

        @Test
        @DisplayName("Tỷ lệ đặt cọc 0 — thị trường chưa chốt con số vẫn tính được đơn")
        void tyLeKhong() {
            PriceBreakdown kq = PricingEngine.tinh(
                    PricingInput.cua(List.of(PaxLine.of("ADULT", 2, dong("18900000"))),
                            SO_LE_VND, BigDecimal.ZERO));

            assertEquals(dong("0"), kq.deposit());
            assertEquals(kq.total(), kq.balance(),
                    "Engine nhận hằng số làm tham số nên thiếu con số nghiệp vụ không chặn được nó");
        }
    }

    // ------------------------------------------------------------ tiện ích

    /** Đúng ví dụ ở docs/14 mục 4. */
    private static PricingInput donDayDu() {
        return PricingInput.cua(List.of(PaxLine.of("ADULT", 2, kr("24990.00"))), SO_LE_DKK, COC_DK)
                .phongDon(0, kr("4500.00"))
                .phuThuDiemKhoiHanh(kr("800.00"))
                .baoHiem(kr("895.00"))
                .demKhachSanTruocBay(1, 1, kr("1095.00"))
                .giamDatSom(kr("1000.00"))
                .phiXuLy(kr("295.00"));
    }

    private static boolean coDong(PriceBreakdown kq, PriceLineKind loai) {
        return kq.lines().stream().anyMatch(d -> d.kind() == loai);
    }
}
