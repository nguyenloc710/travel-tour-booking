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
        List<PriceLine> row = new ArrayList<>();
        int fractionDigits = dauVao.fractionDigits();
        String currency = dauVao.currency();
        int paxCount = dauVao.tongSoKhach();

        // Thứ tự dưới đây LÀ thứ tự của docs/14 mục 2.1. Đừng sắp xếp lại cho
        // "gọn": giảm đặt sớm trừ TRƯỚC phí xử lý, và đảo hai dòng cuối ra kết
        // quả khác.

        // 1. Giá cơ bản — một dòng cho mỗi loại khách.
        for (PaxLine p : dauVao.paxCopy()) {
            them(row, PriceLineKind.BASE, "price.base." + p.paxTypeCode(),
                    BigDecimal.valueOf(p.count()), p.unitPrice(), p.thanhTien(), fractionDigits);
        }

        // 2. Phụ thu phòng đơn.
        them(row, PriceLineKind.SINGLE_SUPPLEMENT, "price.singleSupplement",
                BigDecimal.valueOf(dauVao.singleTravellers()),
                dauVao.singleSupplementPerPerson(),
                nhan(dauVao.singleSupplementPerPerson(), dauVao.singleTravellers()), fractionDigits);

        // 3. Nâng hạng cabin — chỉ CRUISE.
        them(row, PriceLineKind.CABIN_UPGRADE, "price.cabinUpgrade",
                BigDecimal.valueOf(paxCount), dauVao.cabinUpgradePerPerson(),
                nhan(dauVao.cabinUpgradePerPerson(), paxCount), fractionDigits);

        // 4. Phụ thu điểm khởi hành.
        them(row, PriceLineKind.DEPARTURE_ORIGIN, "price.departureOrigin",
                BigDecimal.valueOf(paxCount), dauVao.departureOriginSurchargePerPerson(),
                nhan(dauVao.departureOriginSurchargePerPerson(), paxCount), fractionDigits);

        // 5. Bảo hiểm.
        them(row, PriceLineKind.INSURANCE, "price.insurance",
                BigDecimal.valueOf(paxCount), dauVao.insurancePerPerson(),
                nhan(dauVao.insurancePerPerson(), paxCount), fractionDigits);

        // 6. Đêm khách sạn trước bay — đơn giá phòng × số phòng × số đêm.
        int roomNights = dauVao.preTourHotelRooms() * dauVao.preTourHotelNights();
        them(row, PriceLineKind.PRE_TOUR_HOTEL, "price.preTourHotel",
                BigDecimal.valueOf(roomNights), dauVao.preTourHotelPricePerRoomNight(),
                nhan(dauVao.preTourHotelPricePerRoomNight(), roomNights), fractionDigits);

        // 7. Giảm đặt sớm — ÂM, và trừ TRƯỚC phí xử lý.
        Money giam = dauVao.earlyBirdPerPerson();
        if (giam != null && giam.amount().signum() != 0) {
            Money perPerson = new Money(giam.amount().negate(), giam.currency());
            them(row, PriceLineKind.EARLY_BIRD_DISCOUNT, "price.earlyBird",
                    BigDecimal.valueOf(paxCount), perPerson, nhan(perPerson, paxCount), fractionDigits);
        }

        // 8. Phí xử lý — một lần mỗi đơn, không nhân theo số khách.
        them(row, PriceLineKind.PROCESSING_FEE, "price.processingFee",
                BigDecimal.ONE, dauVao.processingFee(), dauVao.processingFee(), fractionDigits);

        // Tổng là tổng của các số ĐÃ TRÒN. Cộng số chưa tròn rồi tròn một lần
        // cho tổng đẹp hơn về mặt toán học, nhưng khi đó bảng phân rã cộng lại
        // không bằng tổng — và khách nhìn thấy ngay ở bước thanh toán.
        Money tong = row.stream()
                .map(PriceLine::amount)
                .reduce(Money::plus)
                .orElse(new Money(BigDecimal.ZERO, currency).round(fractionDigits));

        // Đặt cọc làm tròn XUỐNG, phần còn lại lấy bằng hiệu — nhờ vậy
        // deposit + balance = total đúng tuyệt đối, không phụ thuộc may rủi.
        Money deposit = tong.depositAt(dauVao.depositRate(), fractionDigits);
        Money conLai = tong.minus(deposit);

        return new PriceBreakdown(List.copyOf(row), tong, deposit, conLai);
    }

    /** Dòng bằng 0 hoặc không áp dụng thì <b>không xuất hiện</b> — docs/14 mục 2.2. */
    private static void them(List<PriceLine> row, PriceLineKind type, String khoa,
                             BigDecimal quantity, Money unitPrice, Money thanhTien, int fractionDigits) {
        if (thanhTien == null) {
            return;
        }
        Money daTron = thanhTien.round(fractionDigits);
        if (daTron.amount().signum() == 0) {
            return;
        }
        row.add(new PriceLine(type, khoa, quantity,
                unitPrice == null ? null : unitPrice.round(fractionDigits), daTron));
    }

    private static Money nhan(Money unitPrice, int times) {
        return unitPrice == null ? null : unitPrice.times(BigDecimal.valueOf(times));
    }
}
