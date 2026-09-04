package vn.travel.booking.quote.dto;

import java.util.UUID;

/**
 * Sản phẩm nhìn từ luồng báo giá — vừa đủ để nhận hay từ chối một yêu cầu.
 *
 * <p>Ba thứ và chỉ ba thứ: nó có phải {@code PRIVATE_TOUR} không, phải báo
 * trước bao nhiêu ngày, và báo giá gửi ra sống được bao lâu. Nạp cả
 * {@code ProductDetail} ở đây là kéo theo lịch trình, khách sạn và bộ ảnh cho
 * một quyết định không cần đến chúng.
 */
public record QuoteProduct(
        UUID id,
        String productType,
        String title,
        int leadTimeDays,
        int quoteValidDays) {
}
