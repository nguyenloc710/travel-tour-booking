package vn.travel.booking.application.booking;

import java.util.Optional;
import java.util.UUID;

public interface BookingPricingPort {

    /**
     * Rỗng khi ngày khởi hành không tồn tại, đã xoá mềm, hoặc không thuộc thị
     * trường này. Một ngày khởi hành thuộc về <b>đúng một</b> thị trường — ADR-006.
     */
    Optional<DeparturePricing> load(String market, UUID departureId, UUID departureOriginId);
}
