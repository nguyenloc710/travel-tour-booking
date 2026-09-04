package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PostDetailView(
        UUID id,
        String heroImage,
        OffsetDateTime publishedAt,
        List<TagView> tags,
        List<PostTranslationView> translations,
        OffsetDateTime lastModifiedAt,
        String lastModifiedBy) {
}
