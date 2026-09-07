package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Một điểm đến nhìn từ trang quản trị, kèm <b>mọi</b> bản dịch.
 *
 * <p>{@code productCount} có mặt để màn hình nói trước hậu quả của nút Xoá thay
 * vì để người dùng bấm rồi nhận {@code 409} — docs/22 mục 7 đòi ô xác nhận nói
 * rõ hậu quả.
 */
public record DestinationDetailView(
        UUID id,
        String code,
        UUID regionId,
        String regionName,
        int sortOrder,
        int productCount,
        List<DestinationTranslationView> translations,
        OffsetDateTime lastModifiedAt,
        String lastModifiedBy) {
}
