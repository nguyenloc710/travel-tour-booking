package vn.travel.booking.product.dto;

import vn.travel.booking.departure.dto.DepartureStatus;
import vn.travel.booking.common.money.Money;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Một ngày khởi hành như khách nhìn thấy.
 *
 * <p>{@code status} và {@code seatsAvailable} đều là <b>giá trị tính ra</b>:
 * trạng thái từ {@code DepartureStatuses.resolve}, số chỗ từ sức chứa trừ đi
 * chỗ đã đặt và chỗ đang giữ. Không cột nào trong cơ sở dữ liệu mang hai giá trị
 * này, và đó là chủ ý (docs/14 mục 5 và 6.1).
 */
public record DepartureView(
        UUID id,
        LocalDate departDate,
        LocalDate returnDate,
        int days,
        DepartureStatus status,
        int seatsAvailable,
        Money priceFrom,
        String cabinCategory,
        String departureCity) {
}
