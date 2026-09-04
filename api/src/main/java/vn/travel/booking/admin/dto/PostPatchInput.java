package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;

/**
 * Sửa phần không phụ thuộc ngôn ngữ của một bài viết.
 *
 * <p><b>Không có thẻ ở đây.</b> Thẻ đi qua endpoint riêng, vì bộ sinh mã dựng
 * trường mảng vắng mặt thành danh sách rỗng: trong một {@code PATCH} thì "không
 * nhắc tới thẻ" và "gỡ hết thẻ" không phân biệt được, và hậu quả là đổi mỗi cái
 * ảnh bìa cũng gỡ sạch thẻ của bài.
 */
public record PostPatchInput(String heroImage, OffsetDateTime publishedAt) {
}
