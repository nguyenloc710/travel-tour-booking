package vn.travel.booking.destination.dto;

import vn.travel.booking.product.dto.GalleryImage;
import vn.travel.booking.product.dto.NamedRef;

/**
 * Một điểm đến, đã lọc theo cặp (market, locale), kèm miền chứa nó.
 *
 * <p>{@code summary} là {@code null} khi biên tập viên chưa viết mô tả — trường
 * rỗng bị bỏ hẳn khỏi JSON, không trả chuỗi rỗng (docs/13 mục 4).
 *
 * <p>{@code image} là {@code null} khi điểm đến chưa có ảnh, và cũng null khi
 * ảnh có nhưng thiếu {@code alt} ở locale đang đọc — luật không fallback cho nội
 * dung bán hàng, giống hệt bộ ảnh của sản phẩm. Thẻ điểm đến phải dựng được
 * trong cả hai trường hợp.
 *
 * <p>{@code productCount} đếm theo <b>điểm đến chính</b> của sản phẩm. Đếm theo
 * mọi điểm đến sản phẩm ghé qua sẽ làm tổng của các điểm đến lớn hơn số sản phẩm
 * thật, và khách phát hiện ngay khi bấm vào.
 */
public record DestinationSummary(
        String slug,
        String name,
        String summary,
        NamedRef region,
        GalleryImage image,
        int productCount) {
}
