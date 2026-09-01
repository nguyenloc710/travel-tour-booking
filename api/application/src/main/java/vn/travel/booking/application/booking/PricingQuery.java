package vn.travel.booking.application.booking;

import java.util.Map;
import java.util.UUID;

/**
 * @param pax số khách theo mã loại khách
 * @param singleTravellers số khách ở một mình — mỗi người một phần phụ thu phòng đơn
 */
public record PricingQuery(
        UUID departureId,
        Map<String, Integer> pax,
        int singleTravellers,
        UUID departureOriginId) {

    public int tongSoKhach() {
        return pax.values().stream().mapToInt(Integer::intValue).sum();
    }
}
