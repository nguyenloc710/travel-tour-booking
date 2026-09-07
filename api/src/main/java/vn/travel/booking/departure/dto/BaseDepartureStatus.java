package vn.travel.booking.departure.dto;

/**
 * Trạng thái ngày khởi hành <b>lưu trong cơ sở dữ liệu</b>.
 *
 * <p>Cố tình <b>không có</b> {@code GUARANTEED}: đó là giá trị tính ra từ số
 * khách đã đặt, không phải thứ ai nhập tay được (docs/14 mục 7). Ràng buộc
 * {@code ck_dep_status} ở tầng CSDL nói đúng bốn giá trị này; enum ở đây nói lại
 * cùng điều đó ở tầng kiểu, nên nhập sai là lỗi biên dịch chứ không phải lỗi lúc
 * chạy.
 */
public enum BaseDepartureStatus {
    OPEN,
    FEW_SEATS,
    SOLD_OUT,
    PENDING
}
