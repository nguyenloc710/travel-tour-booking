package vn.travel.booking.application.post;

import vn.travel.booking.application.product.NamedRef;

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
