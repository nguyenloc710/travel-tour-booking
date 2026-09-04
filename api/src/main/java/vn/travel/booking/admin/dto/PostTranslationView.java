package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PostTranslationView(
        String locale,
        String slug,
        String title,
        String excerpt,
        List<String> body,
        String status,
        boolean isSource,
        OffsetDateTime lastModifiedAt,
        String lastModifiedBy) {
}
