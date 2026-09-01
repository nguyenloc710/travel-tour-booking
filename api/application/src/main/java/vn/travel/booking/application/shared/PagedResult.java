package vn.travel.booking.application.shared;

import java.util.List;

/**
 * Một trang kết quả, phân trang theo offset (docs/13 mục 6).
 *
 * <p>{@code totalItems} đếm <b>trong phạm vi (market, locale)</b> sau khi đã áp
 * bộ lọc, nên hai locale ra hai con số khác nhau. Đó là con số frontend phải
 * hiển thị — "Xem tất cả 92 tour" không được hardcode.
 */
public record PagedResult<T>(List<T> items, int page, int size, long totalItems) {

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((totalItems + size - 1) / size);
    }
}
