package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;

public record LectureTranslationView(
        String locale,
        String title,
        String description,
        boolean isSource,
        OffsetDateTime lastModifiedAt,
        String lastModifiedBy) {
}
