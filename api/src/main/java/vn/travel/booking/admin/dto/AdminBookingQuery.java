package vn.travel.booking.admin.dto;

import java.time.LocalDate;

/**
 * Bộ lọc của màn hình danh sách đơn (docs/22 M6).
 *
 * <p><b>{@code scope} và {@code status} không cùng cấp.</b> {@code scope} là
 * mặc định thông minh — "việc cần làm hôm nay" — còn {@code status} là bộ lọc
 * tường minh của người dùng. Có {@code status} thì {@code scope} bị bỏ qua: một
 * lựa chọn người dùng nhìn thấy luôn thắng một mặc định họ không nhìn thấy.
 *
 * @param scope  {@code NEEDS_ACTION} hoặc {@code ALL}
 * @param status một trạng thái cụ thể, hoặc {@code null}
 * @param from   ngày <b>tạo đơn</b>, không phải ngày khởi hành
 * @param to     ngày tạo đơn, tính tới hết ngày đó
 * @param q      khớp một phần mã tra cứu hoặc email liên hệ
 */
public record AdminBookingQuery(
        String scope,
        String status,
        String market,
        LocalDate from,
        LocalDate to,
        String q,
        int page,
        int size) {

    /** Ba trạng thái mà nhân viên còn nợ khách một hành động — docs/22 mục 6. */
    public static final java.util.List<String> NEEDS_ACTION_STATUSES =
            java.util.List.of("DRAFT", "PENDING_PAYMENT", "PENDING_CONFIRMATION");
}
