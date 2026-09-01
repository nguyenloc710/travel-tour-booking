package vn.travel.booking.web;

/**
 * Locale không hợp lệ hoặc không được bật.
 *
 * <p>Trả <b>400</b>, <b>không</b> lặng lẽ lùi về {@code da}. Lùi lặng lẽ ở tầng
 * API che mất lỗi cấu hình của frontend: trang tiếng Việt gửi sai header vẫn
 * hiện ra nội dung tiếng Đan và không ai biết cho tới khi khách phàn nàn
 * (docs/13 mục 3).
 *
 * <p>Đừng nhầm với fallback <b>chuỗi giao diện</b> — cái đó có lùi về {@code da}
 * và ghi log, nhưng nó nằm ở frontend, không nằm ở đây.
 */
public class UnsupportedLocaleException extends RuntimeException {

    private final String header;

    public UnsupportedLocaleException(String header) {
        super("Accept-Language không hỗ trợ: " + header);
        this.header = header;
    }

    public String header() {
        return header;
    }
}
