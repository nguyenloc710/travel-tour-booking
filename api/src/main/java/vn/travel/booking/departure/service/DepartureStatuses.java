package vn.travel.booking.departure.service;

import vn.travel.booking.departure.dto.BaseDepartureStatus;
import vn.travel.booking.departure.dto.DepartureStatus;

/**
 * Giải trạng thái ngày khởi hành — docs/14 mục 5.
 *
 * <p>Hàm thuần: không đọc cơ sở dữ liệu, không đọc đồng hồ hệ thống, không biết
 * Spring tồn tại. Mọi thứ nó cần đều là tham số, kể cả ngưỡng "còn ít chỗ" —
 * hằng số là dữ liệu cấu hình, không phải kiến thức của lõi nghiệp vụ.
 */
public final class DepartureStatuses {

    private DepartureStatuses() {
    }

    /**
     * Thứ tự xét dừng ở điều kiện đầu tiên đúng. Hai điều dễ làm sai:
     *
     * <ul>
     *   <li><b>{@code GUARANTEED} xét TRƯỚC {@code FEW_SEATS}.</b> Một chuyến vừa
     *       đảm bảo khởi hành vừa còn ít chỗ thì hiện "đảm bảo khởi hành" — thông
     *       tin có giá trị bán hàng cao hơn. Đảo hai bước này không làm gãy gì,
     *       chỉ làm mất doanh thu một cách âm thầm.
     *   <li><b>Nhân viên ghi đè được về phía nghiêm ngặt hơn, nhưng không ghi đè
     *       được {@code GUARANTEED}.</b> Không có nhánh nào cho phép, và đó là
     *       chủ ý: đã đủ khách để chắc chắn đi thì không ai "đóng nhẹ" nó lại.
     * </ul>
     *
     * @param guaranteedThreshold ngưỡng đảm bảo khởi hành; {@code null} với loại
     *                            sản phẩm không có khái niệm đó, ví dụ du thuyền
     * @param seatsAvailable      đã <b>trừ chỗ đang giữ</b> — docs/14 mục 6.1
     */
    public static DepartureStatus resolve(
            BaseDepartureStatus baseStatus,
            int seatsBooked,
            int seatsAvailable,
            Integer guaranteedThreshold,
            int fewSeatsThreshold) {

        if (baseStatus == BaseDepartureStatus.PENDING) {
            return DepartureStatus.PENDING;
        }
        if (baseStatus == BaseDepartureStatus.SOLD_OUT) {
            return DepartureStatus.SOLD_OUT;
        }
        if (seatsAvailable <= 0) {
            return DepartureStatus.SOLD_OUT;
        }
        if (guaranteedThreshold != null && seatsBooked >= guaranteedThreshold) {
            return DepartureStatus.GUARANTEED;
        }
        if (seatsAvailable <= fewSeatsThreshold) {
            return DepartureStatus.FEW_SEATS;
        }
        if (baseStatus == BaseDepartureStatus.FEW_SEATS) {
            return DepartureStatus.FEW_SEATS;
        }
        return DepartureStatus.OPEN;
    }
}
