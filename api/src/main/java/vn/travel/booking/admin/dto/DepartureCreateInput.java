package vn.travel.booking.admin.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code returnDate} không có ở đây: nó bằng {@code departDate + days - 1} và
 * do entity tự suy. Nhận cả hai đầu là mời hai giá trị mâu thuẫn nhau.
 */
public record DepartureCreateInput(
        String market,
        LocalDate departDate,
        Short days,
        Short capacity,
        String cabinCategory,
        String baseStatus,
        UUID departureOriginId) {
}
