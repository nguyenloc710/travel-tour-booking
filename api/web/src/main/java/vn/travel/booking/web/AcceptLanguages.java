package vn.travel.booking.web;

import java.util.List;
import java.util.Locale;

/**
 * Giải header {@code Accept-Language} thành một locale được hỗ trợ.
 *
 * <p>Locale <b>không</b> suy ra market và market <b>không</b> suy ra locale.
 * Đây là hai tham số độc lập: khách Việt sống ở Đan Mạch mua ở thị trường
 * {@code DK} nhưng đọc {@code vi}.
 */
public final class AcceptLanguages {

    /** Ngôn ngữ nguồn — ADR-004. Cũng là locale dự phòng cho chuỗi giao diện. */
    public static final String SOURCE = "da";

    private static final List<String> SUPPORTED = List.of("da", "vi");

    private AcceptLanguages() {
    }

    /**
     * Lấy thẻ ngôn ngữ đầu tiên được hỗ trợ. Không có thẻ nào khớp thì trả về
     * {@code null} — controller quyết định trả lỗi, không tự đoán giùm khách.
     *
     * <p>Chú ý: fallback về {@code da} ở đây chỉ đúng với <b>chuỗi giao diện</b>.
     * Nội dung bán hàng thiếu bản dịch thì ẩn hoàn toàn, không hiện bản
     * {@code da} thay thế — docs/02 mục 4.
     */
    public static String resolve(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        for (String phan : header.split(",")) {
            String the = phan.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (the.contains("-")) {
                the = the.substring(0, the.indexOf('-'));
            }
            if (SUPPORTED.contains(the)) {
                return the;
            }
        }
        return null;
    }
}
