package vn.travel.booking.admin.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Sửa buổi thuyết trình. {@code market} không có ở đây — đổi thị trường là tạo buổi khác. */
public record LecturePatchInput(
        LocalDate eventDate,
        LocalTime startTime,
        String city,
        String venue,
        Integer seats) {
}
