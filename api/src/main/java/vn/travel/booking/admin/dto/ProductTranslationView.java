package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Một bản dịch như trang quản trị nhìn thấy — kể cả bản chưa xuất bản.
 *
 * <p>{@code outdated} là giá trị <b>tính ra</b>, không lưu: bản nguồn sửa sau
 * lần dịch gần nhất (docs/11 mục 8). Với chính bản nguồn thì nó là {@code null}.
 */
public record ProductTranslationView(
        String locale,
        String slug,
        String title,
        String shortDescription,
        List<String> longDescription,
        List<String> whyChooseThis,
        String heroImageAlt,
        String status,
        boolean isSource,
        Boolean outdated,
        OffsetDateTime lastModifiedAt,
        UUID lastModifiedBy) {
}
