package vn.travel.booking.post.dto;

import vn.travel.booking.product.dto.NamedRef;

import java.time.OffsetDateTime;
import java.util.List;

public record PostSummary(
        String slug,
        String title,
        String excerpt,
        String heroImage,
        OffsetDateTime publishedAt,
        List<NamedRef> tags) {
}
