package vn.travel.booking.admin.dto;

import java.math.BigDecimal;

/**
 * Một ô của ma trận loại khách × kiểu phòng.
 *
 * <p><b>Không có tiền tệ.</b> Nó lấy từ cấu hình thị trường của chính ngày khởi
 * hành này — cho client gửi tiền tệ là mở đường nhập giá DKK vào thị trường
 * {@code VN}, và không ràng buộc CSDL nào bắt được chuyện đó.
 */
public record DeparturePriceInput(String paxTypeCode, String occupancy, BigDecimal amount) {
}
