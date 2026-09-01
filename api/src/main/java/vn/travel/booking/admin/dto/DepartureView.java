package vn.travel.booking.admin.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @param prices rỗng nghĩa là <b>chưa có giá</b>: ngày khởi hành tồn tại nhưng
 *               chưa bán được. Trang quản trị phải hiện rõ điều đó
 */
public record DepartureView(
        UUID id,
        String market,
        LocalDate departDate,
        LocalDate returnDate,
        Short days,
        String cabinCategory,
        String baseStatus,
        Short capacity,
        Short seatsBooked,
        UUID departureOriginId,
        List<DeparturePriceView> prices) {
}
