package vn.travel.booking.admin.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record LectureRow(
        UUID id,
        String market,
        LocalDate eventDate,
        LocalTime startTime,
        String city,
        String venue,
        int seats,
        int seatsTaken,
        String title,
        Map<String, ContentLocaleState> locales,
        OffsetDateTime lastModifiedAt,
        String lastModifiedBy) {
}
