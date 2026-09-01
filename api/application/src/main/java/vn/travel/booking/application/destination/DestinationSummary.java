package vn.travel.booking.application.destination;

import vn.travel.booking.application.product.NamedRef;

/**
 * Một điểm đến, đã lọc theo cặp (market, locale), kèm miền chứa nó.
 *
 * <p>{@code summary} là {@code null} khi biên tập viên chưa viết mô tả — trường
 * rỗng bị bỏ hẳn khỏi JSON, không trả chuỗi rỗng (docs/13 mục 4).
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
        int productCount) {
}
