package vn.travel.booking.post.dto;

import vn.travel.booking.product.dto.NamedRef;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * {@code body} là danh sách đoạn văn, không phải một khối HTML — cùng lý do với
 * {@code longDescription} của sản phẩm: HTML tự do trong cơ sở dữ liệu là một lỗ
 * chèn mã chờ sẵn.
 */
public record PostDetail(
        String slug,
        String title,
        String excerpt,
        List<String> body,
        String heroImage,
        OffsetDateTime publishedAt,
        List<NamedRef> tags) {
}
