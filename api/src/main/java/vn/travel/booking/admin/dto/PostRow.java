package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Một dòng của danh sách bài viết.
 *
 * <p>{@code locales} là cột quan trọng nhất: {@code MISSING} ở {@code vi} nghĩa
 * là bài này <b>không tồn tại</b> với khách đọc tiếng Việt — không phải "hiện
 * bản tiếng Đan thay thế".
 */
public record PostRow(
        UUID id,
        String title,
        String heroImage,
        OffsetDateTime publishedAt,
        Map<String, ContentLocaleState> locales,
        List<TagView> tags,
        OffsetDateTime lastModifiedAt,
        String lastModifiedBy) {
}
