package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Một dòng của danh sách sản phẩm quản trị.
 *
 * @param sourceTitle tiêu đề bản ngôn ngữ nguồn — luôn có, vì CSDL từ chối một
 *                    sản phẩm không có bản dịch nguồn
 *                    ({@code ct_product_source_translation})
 * @param markets     rỗng nghĩa là <b>chưa gán thị trường nào</b>
 */
public record AdminProductRow(
        UUID id,
        String productType,
        String sourceTitle,
        String sourceStatus,
        List<MarketState> markets,
        List<TranslationState> translations,
        OffsetDateTime lastModifiedAt) {
}
