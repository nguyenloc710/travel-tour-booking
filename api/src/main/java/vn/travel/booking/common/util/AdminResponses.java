package vn.travel.booking.common.util;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Hai thứ mọi controller quản trị đều cần.
 *
 * <p>Trước đây chúng là phương thức {@code private static} nằm trong
 * {@code AdminController}. Khi controller đó tách thành mười một controller theo
 * feature, chép chúng sang từng nơi là tạo mười một bản của cùng một quy tắc —
 * và quy tắc "mọi phản hồi quản trị là no-store" chỉ đúng khi nó có đúng một
 * bản.
 */
public final class AdminResponses {

    private AdminResponses() {
    }

    /**
     * Mọi phản hồi của bề mặt quản trị là {@code no-store} — {@code docs/22}
     * mục 7. Không phải {@code no-cache}: {@code no-cache} vẫn cho lưu rồi hỏi
     * lại, còn dữ liệu quản trị thì không được nằm lại ở đâu cả.
     */
    public static ResponseEntity.BodyBuilder noCache() {
        return noCache(HttpStatus.OK);
    }

    public static ResponseEntity.BodyBuilder noCache(HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }

    /**
     * Ngữ cảnh servlet của request đang chạy.
     *
     * <p>Chỉ luồng đăng nhập cần tới nó: nó phải chạm thẳng vào {@code HttpSession}
     * để đổi id phiên. Controller khác không nên gọi — cần gì từ request thì khai
     * làm tham số của phương thức, để nhìn chữ ký là biết.
     */
    public static ServletRequestAttributes servlet() {
        return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    }
}
