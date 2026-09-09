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

    public static PriceBreakdown calculate(PricingInput input) {
        List<PriceLine> row = new ArrayList<>();
        int fractionDigits = input.fractionDigits();
        String currency = input.currency();
        int paxCount = input.totalPaxCount();

        // Thứ tự dưới đây LÀ thứ tự của docs/14 mục 2.1. Đừng sắp xếp lại cho
        // "gọn": giảm đặt sớm trừ TRƯỚC phí xử lý, và đảo hai dòng cuối ra kết
        // quả khác.

        // 1. Giá cơ bản — một dòng cho mỗi loại khách.
        for (PaxLine p : input.paxCopy()) {
            addLine(row, PriceLineKind.BASE, "price.base." + p.paxTypeCode(),
                    BigDecimal.valueOf(p.count()), p.unitPrice(), p.total(), fractionDigits);
        }

        // 2. Phụ thu phòng đơn.
        addLine(row, PriceLineKind.SINGLE_SUPPLEMENT, "price.singleSupplement",
                BigDecimal.valueOf(input.singleTravellers()),
                input.singleSupplementPerPerson(),
                multiply(input.singleSupplementPerPerson(), input.singleTravellers()), fractionDigits);

        // 3. Nâng hạng cabin — chỉ CRUISE.
        addLine(row, PriceLineKind.CABIN_UPGRADE, "price.cabinUpgrade",
                BigDecimal.valueOf(paxCount), input.cabinUpgradePerPerson(),
                multiply(input.cabinUpgradePerPerson(), paxCount), fractionDigits);

        // 4. Phụ thu điểm khởi hành.
        addLine(row, PriceLineKind.DEPARTURE_ORIGIN, "price.departureOrigin",
                BigDecimal.valueOf(paxCount), input.departureOriginSurchargePerPerson(),
                multiply(input.departureOriginSurchargePerPerson(), paxCount), fractionDigits);

        // 5. Bảo hiểm.
        addLine(row, PriceLineKind.INSURANCE, "price.insurance",
                BigDecimal.valueOf(paxCount), input.insurancePerPerson(),
                multiply(input.insurancePerPerson(), paxCount), fractionDigits);

        // 6. Đêm khách sạn trước bay — đơn giá phòng × số phòng × số đêm.
        int roomNights = input.preTourHotelRooms() * input.preTourHotelNights();
        addLine(row, PriceLineKind.PRE_TOUR_HOTEL, "price.preTourHotel",
                BigDecimal.valueOf(roomNights), input.preTourHotelPricePerRoomNight(),
                multiply(input.preTourHotelPricePerRoomNight(), roomNights), fractionDigits);

        // 7. Giảm đặt sớm — ÂM, và trừ TRƯỚC phí xử lý.
        Money discount = input.earlyBirdPerPerson();
        if (discount != null && discount.amount().signum() != 0) {
            Money perPerson = new Money(discount.amount().negate(), discount.currency());
            addLine(row, PriceLineKind.EARLY_BIRD_DISCOUNT, "price.earlyBird",
                    BigDecimal.valueOf(paxCount), perPerson, multiply(perPerson, paxCount), fractionDigits);
        }

        // 8. Phí xử lý — một lần mỗi đơn, không nhân theo số khách.
        addLine(row, PriceLineKind.PROCESSING_FEE, "price.processingFee",
                BigDecimal.ONE, input.processingFee(), input.processingFee(), fractionDigits);

        // Tổng là tổng của các số ĐÃ TRÒN. Cộng số chưa tròn rồi tròn một lần
        // cho tổng đẹp hơn về mặt toán học, nhưng khi đó bảng phân rã cộng lại
        // không bằng tổng — và khách nhìn thấy ngay ở bước thanh toán.
        Money total = row.stream()
                .map(PriceLine::amount)
                .reduce(Money::plus)
                .orElse(new Money(BigDecimal.ZERO, currency).round(fractionDigits));

        // Đặt cọc làm tròn XUỐNG, phần còn lại lấy bằng hiệu — nhờ vậy
        // deposit + balance = total đúng tuyệt đối, không phụ thuộc may rủi.
        Money deposit = total.depositAt(input.depositRate(), fractionDigits);
        Money balance = total.minus(deposit);

        return new PriceBreakdown(List.copyOf(row), total, deposit, balance);
    }

    /** Dòng bằng 0 hoặc không áp dụng thì <b>không xuất hiện</b> — docs/14 mục 2.2. */
    private static void addLine(List<PriceLine> row, PriceLineKind type, String key,
                                BigDecimal quantity, Money unitPrice, Money lineTotal, int fractionDigits) {
        if (lineTotal == null) {
            return;
        }
        Money rounded = lineTotal.round(fractionDigits);
        if (rounded.amount().signum() == 0) {
            return;
        }
        row.add(new PriceLine(type, key, quantity,
                unitPrice == null ? null : unitPrice.round(fractionDigits), rounded));
    }

    private static Money multiply(Money unitPrice, int times) {
        return unitPrice == null ? null : unitPrice.times(BigDecimal.valueOf(times));
    }
}
