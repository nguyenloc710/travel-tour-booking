package vn.travel.booking.lecture.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * {@code seatsAvailable} là giá trị tính ra: {@code seats − seats_taken}. Không
 * có cờ "đã đầy" trong cơ sở dữ liệu (docs/14 mục 5.2).
 */
public record LectureSummary(
        UUID id,
        LocalDate eventDate,
        LocalTime startTime,
        String city,
        String venue,
        String title,
        String description,
        int seats,
        int seatsAvailable) {
}
