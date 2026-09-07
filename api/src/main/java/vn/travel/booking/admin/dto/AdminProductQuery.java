package vn.travel.booking.admin.dto;

/**
 * Bộ lọc của màn hình danh sách sản phẩm quản trị (docs/22 M2).
 *
 * <p><b>Không có market và không có locale</b> — khác hẳn {@code ProductQuery}
 * của bề mặt công khai. Nhân viên làm việc xuyên thị trường và nhìn mọi bản dịch
 * cùng lúc (docs/22 mục 1); nhét market vào đây là bắt họ chuyển qua chuyển lại.
 *
 * @param market chỉ là <b>bộ lọc</b> "đã gán thị trường này chưa", không phải
 *               phạm vi dữ liệu
 * @param gap    {@code MISSING} hoặc {@code OUTDATED} ở một locale bất kỳ
 */
public record AdminProductQuery(
        String productType,
        String market,
        String gap,
        String q,
        int page,
        int size) {
}
