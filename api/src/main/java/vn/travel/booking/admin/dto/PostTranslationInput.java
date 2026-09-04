package vn.travel.booking.admin.dto;

import java.util.List;

/**
 * Bản dịch một bài viết.
 *
 * <p>{@code body} là <b>mảng đoạn văn</b>, không phải một khối HTML: nội dung do
 * biên tập viên nhập, và HTML tự do đi thẳng từ CSDL ra trang là một lỗ chèn mã
 * chờ sẵn.
 */
public record PostTranslationInput(
        String slug,
        String title,
        String excerpt,
        List<String> body,
        String status) {
}
