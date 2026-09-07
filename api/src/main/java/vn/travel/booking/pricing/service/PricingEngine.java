package vn.travel.booking.pricing.service;

import vn.travel.booking.pricing.dto.PaxLine;
import vn.travel.booking.pricing.dto.PriceBreakdown;
import vn.travel.booking.pricing.dto.PriceLine;
import vn.travel.booking.pricing.dto.PriceLineKind;
import vn.travel.booking.pricing.dto.PricingInput;
import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Engine tính giá — docs/14 mục 2.
 *
 * <p><b>Một hàm với các dòng tuỳ chọn, không phải sáu hàm.</b> Sáu loại sản phẩm
 * khác nhau ở chỗ dòng nào có mặt, không ở chỗ tính thế nào. Tách thành sáu bản
 * sao là sáu chỗ để sửa khi thứ tự cộng dồn đổi, và sẽ có bản quên.
 *
 * <p>Hàm thuần: không đọc cơ sở dữ liệu, không đọc đồng hồ, không biết Spring
 * tồn tại. Test bằng JUnit thuần trong vài mili giây.
 */
public final class PricingEngine {

    private PricingEngine() {
    }

    public static PriceBreakdown tinh(PricingInput dauVao) {
        List<PriceLine> dong = new ArrayList<>();
        int soLe = dauVao.fractionDigits();
        String tienTe = dauVao.currency();
        int soKhach = dauVao.tongSoKhach();

        // Thứ tự dưới đây LÀ thứ tự của docs/14 mục 2.1. Đừng sắp xếp lại cho
        // "gọn": giảm đặt sớm trừ TRƯỚC phí xử lý, và đảo hai dòng cuối ra kết
        // quả khác.

        // 1. Giá cơ bản — một dòng cho mỗi loại khách.
        for (PaxLine p : dauVao.paxCopy()) {
            them(dong, PriceLineKind.BASE, "price.base." + p.paxTypeCode(),
                    BigDecimal.valueOf(p.count()), p.unitPrice(), p.thanhTien(), soLe);
        }

        // 2. Phụ thu phòng đơn.
        them(dong, PriceLineKind.SINGLE_SUPPLEMENT, "price.singleSupplement",
                BigDecimal.valueOf(dauVao.singleTravellers()),
                dauVao.singleSupplementPerPerson(),
                nhan(dauVao.singleSupplementPerPerson(), dauVao.singleTravellers()), soLe);

        // 3. Nâng hạng cabin — chỉ CRUISE.
        them(dong, PriceLineKind.CABIN_UPGRADE, "price.cabinUpgrade",
                BigDecimal.valueOf(soKhach), dauVao.cabinUpgradePerPerson(),
                nhan(dauVao.cabinUpgradePerPerson(), soKhach), soLe);

        // 4. Phụ thu điểm khởi hành.
        them(dong, PriceLineKind.DEPARTURE_ORIGIN, "price.departureOrigin",
                BigDecimal.valueOf(soKhach), dauVao.departureOriginSurchargePerPerson(),
                nhan(dauVao.departureOriginSurchargePerPerson(), soKhach), soLe);

        // 5. Bảo hiểm.
        them(dong, PriceLineKind.INSURANCE, "price.insurance",
                BigDecimal.valueOf(soKhach), dauVao.insurancePerPerson(),
                nhan(dauVao.insurancePerPerson(), soKhach), soLe);

        // 6. Đêm khách sạn trước bay — đơn giá phòng × số phòng × số đêm.
        int soDemPhong = dauVao.preTourHotelRooms() * dauVao.preTourHotelNights();
        them(dong, PriceLineKind.PRE_TOUR_HOTEL, "price.preTourHotel",
                BigDecimal.valueOf(soDemPhong), dauVao.preTourHotelPricePerRoomNight(),
                nhan(dauVao.preTourHotelPricePerRoomNight(), soDemPhong), soLe);

        // 7. Giảm đặt sớm — ÂM, và trừ TRƯỚC phí xử lý.
        Money giam = dauVao.earlyBirdPerPerson();
        if (giam != null && giam.amount().signum() != 0) {
            Money moiNguoi = new Money(giam.amount().negate(), giam.currency());
            them(dong, PriceLineKind.EARLY_BIRD_DISCOUNT, "price.earlyBird",
                    BigDecimal.valueOf(soKhach), moiNguoi, nhan(moiNguoi, soKhach), soLe);
        }

        // 8. Phí xử lý — một lần mỗi đơn, không nhân theo số khách.
        them(dong, PriceLineKind.PROCESSING_FEE, "price.processingFee",
                BigDecimal.ONE, dauVao.processingFee(), dauVao.processingFee(), soLe);

        // Tổng là tổng của các số ĐÃ TRÒN. Cộng số chưa tròn rồi tròn một lần
        // cho tổng đẹp hơn về mặt toán học, nhưng khi đó bảng phân rã cộng lại
        // không bằng tổng — và khách nhìn thấy ngay ở bước thanh toán.
        Money tong = dong.stream()
                .map(PriceLine::amount)
                .reduce(Money::plus)
                .orElse(new Money(BigDecimal.ZERO, tienTe).round(soLe));

        // Đặt cọc làm tròn XUỐNG, phần còn lại lấy bằng hiệu — nhờ vậy
        // deposit + balance = total đúng tuyệt đối, không phụ thuộc may rủi.
        Money datCoc = tong.depositAt(dauVao.depositRate(), soLe);
        Money conLai = tong.minus(datCoc);

        return new PriceBreakdown(List.copyOf(dong), tong, datCoc, conLai);
    }

    /** Dòng bằng 0 hoặc không áp dụng thì <b>không xuất hiện</b> — docs/14 mục 2.2. */
    private static void them(List<PriceLine> dong, PriceLineKind loai, String khoa,
                             BigDecimal soLuong, Money donGia, Money thanhTien, int soLe) {
        if (thanhTien == null) {
            return;
        }
        Money daTron = thanhTien.round(soLe);
        if (daTron.amount().signum() == 0) {
            return;
        }
        dong.add(new PriceLine(loai, khoa, soLuong,
                donGia == null ? null : donGia.round(soLe), daTron));
    }

    private static Money nhan(Money donGia, int soLan) {
        return donGia == null ? null : donGia.times(BigDecimal.valueOf(soLan));
    }
}
