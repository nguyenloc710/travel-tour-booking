package vn.travel.booking.admin.dto;

/**
 * Tình trạng một bản dịch, nhìn từ danh sách.
 *
 * <p>{@code outdated} <b>tính ra, không lưu</b>: bản nguồn sửa sau lần dịch gần
 * nhất. Thêm một cột cờ trong CSDL là tạo ra thứ có thể sai — và nó sẽ sai, vào
 * đúng lúc ai đó cập nhật bản nguồn bằng một câu SQL (docs/22 mục 4.1).
 */
public record TranslationState(String locale, String status, boolean isSource, boolean outdated) {
}
