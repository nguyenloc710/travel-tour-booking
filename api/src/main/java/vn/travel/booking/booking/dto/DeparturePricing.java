package vn.travel.booking.booking.dto;

import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Mọi thứ cần để tính giá một ngày khởi hành, đọc một lần từ cơ sở dữ liệu.
 *
 * <p>Đọc gộp chứ không đọc rải rác: engine giá là hàm thuần và nhận toàn bộ đầu
 * vào cùng lúc, nên tầng này phải gom đủ trước khi gọi. Gom đủ cũng có nghĩa là
 * mọi con số của một lần tính đến từ <b>cùng một thời điểm</b> — đọc rải rác thì
 * giá cơ bản và phụ thu có thể đến từ hai trạng thái khác nhau của dữ liệu.
 */
public record DeparturePricing(
        UUID departureId,
        UUID productId,
        String productType,
        String productTitle,
        LocalDate departDate,
        String currency,
        int fractionDigits,
        BigDecimal depositRate,
        Money processingFee,
        /** Đơn giá phòng đôi theo mã loại khách. */
        Map<String, Money> doubleOccupancy,
        /** Đơn giá phòng đơn theo mã loại khách; thiếu thì không có phụ thu. */
        Map<String, Money> singleOccupancy,
        Money originSurcharge) {
}
