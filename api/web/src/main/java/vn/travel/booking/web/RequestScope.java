package vn.travel.booking.web;

import java.util.Locale;

/**
 * Hai tham số phạm vi của mọi yêu cầu công khai: <b>market</b> và <b>locale</b>.
 *
 * <p>Gom vào một chỗ vì đây là chỗ dễ sai nhất của cả dự án, và vì mọi controller
 * đều cần đúng hai bước này. Nhắc lại điều quan trọng nhất: <b>không suy cái này
 * từ cái kia</b>. Khách Việt sống ở Đan Mạch mua ở {@code DK} nhưng đọc
 * {@code vi} — frontend gửi cả hai, backend không đoán.
 */
final class RequestScope {

    private RequestScope() {
    }

    /** Market chữ thường trong URL, chữ hoa trong code — api/CLAUDE.md mục 10. */
    static String market(String duongDan) {
        return duongDan.toUpperCase(Locale.ROOT);
    }

    static String locale(String acceptLanguage) {
        String locale = AcceptLanguages.resolve(acceptLanguage);
        if (locale == null) {
            throw new UnsupportedLocaleException(acceptLanguage);
        }
        return locale;
    }
}
