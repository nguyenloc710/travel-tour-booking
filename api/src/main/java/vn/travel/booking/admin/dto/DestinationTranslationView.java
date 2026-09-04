package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;

public record DestinationTranslationView(
        String locale,
        String slug,
        String name,
        String summary,
        boolean isSource,
        OffsetDateTime lastModifiedAt,
        String lastModifiedBy) {
}
