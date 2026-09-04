package vn.travel.booking.admin.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LectureDetailView(
        UUID id,
        String market,
        LocalDate eventDate,
        LocalTime startTime,
        String city,
        String venue,
        int seats,
        int seatsTaken,
        List<LectureTranslationView> translations,
        OffsetDateTime lastModifiedAt,
        String lastModifiedBy) {
}
