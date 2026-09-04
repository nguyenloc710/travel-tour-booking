package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tạo một bài viết, kèm <b>luôn</b> bản ngôn ngữ nguồn.
 *
 * <p>Cùng khuôn với tạo sản phẩm: một bài viết không có bản {@code da} là một
 * dòng không hiện ở đâu và không ai tìm lại được để sửa.
 */
public record PostCreateInput(
        String heroImage,
        OffsetDateTime publishedAt,
        List<UUID> tagIds,
        PostTranslationInput translation) {
}
