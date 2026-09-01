package vn.travel.booking.admin.dto;

import vn.travel.booking.product.dto.ProductTypeBlocks;

import java.util.UUID;

/**
 * Sửa sản phẩm. Trường {@code null} nghĩa là <b>không đụng tới</b>, không phải
 * "đặt về rỗng" — API này không có cách xoá một giá trị, và đó là chủ ý: xoá
 * nhầm {@code heroImage} bằng một trường vắng mặt là lỗi im lặng.
 *
 * <p>Không có {@code productType}: loại không đổi được sau khi tạo (docs/22
 * mục 7). Không có bản dịch: nó đi qua endpoint riêng, nơi quyền phụ thuộc
 * locale đang sửa.
 */
public record ProductPatchInput(
        UUID primaryDestinationId,
        Short durationDays,
        String heroImage,
        String mapImage,
        Boolean isNew,
        UUID consultantId,
        ProductTypeBlocks blocks) {
}
