package vn.travel.booking.common.exception;

import java.util.Map;

/**
 * Ngày khởi hành có lưu trú qua đêm mà không có giá phòng đơn
 * ({@code occupancy = 'SINGLE'}). Mã {@code SINGLE_PRICE_MISSING}, HTTP 409.
 *
 * <p><b>Vì sao thiếu một dòng giá lại đáng chặn cả một lần mở bán:</b> thiếu nó
 * thì không có gì hỏng và không có lỗi nào ghi ra. Máy tính giá lấy
 * {@code giá phòng đơn − giá phòng đôi}; không tìm thấy dòng nào thì phụ thu
 * bằng 0, và khách đi <b>một mình</b> đặt được nguyên chuyến ở giá chia đôi
 * phòng. Chênh lệch chỉ lộ ra khi kế toán đối soát với khách sạn — sau khi
 * khách đã đi, và với đúng nhóm khách trả nhiều tiền nhất mỗi đầu người.
 *
 * <p><b>Nằm ở {@code common} vì hai đường cùng ném nó</b>, và đó là chủ ý:
 *
 * <ul>
 *   <li>Đường quản trị chặn <b>trước</b>, ở công tắc mở bán — chỗ duy nhất sửa
 *       được ngay, vì người đang đứng đó là người nhập bảng giá.</li>
 *   <li>Đường khách chặn <b>sau</b>, lúc tính giá cho khách đi một mình. Đáng lẽ
 *       không bao giờ chạm tới, vì cửa trước đã khoá; nó là lưới thứ hai cho dữ
 *       liệu vào bằng lối khác — nhập tay bằng SQL, hoặc một sản phẩm đã bật bán
 *       từ trước khi có luật này.</li>
 * </ul>
 *
 * <p>Máy chủ <b>không đoán</b> giữa "chuyến này không bán phòng đơn" và "quên
 * nhập giá": hai thứ ấy để lại đúng cùng một dấu vết trong CSDL, và đoán sai
 * theo hướng dễ chịu là bán nguyên chuyến ở giá chia đôi phòng. Muốn nói "không
 * bán phòng đơn" thì phải nói bằng một trường riêng, không phải bằng một dòng
 * vắng mặt.
 *
 * <p>Quy tắc kiểm 23 của {@code docs/12} mục 9 là cùng một luật, đo trên toàn bộ
 * cơ sở dữ liệu thay vì trên một sản phẩm.
 */
public class SinglePriceMissingException extends RuntimeException {

    private final transient Map<String, Object> params;

    public SinglePriceMissingException(String chiTiet, Map<String, Object> params) {
        super(chiTiet);
        this.params = params;
    }

    /** Đường quản trị: cả một sản phẩm, nên kèm số ngày thiếu và ngày sớm nhất. */
    public static SinglePriceMissingException cuaSanPham(
            String market, int soNgay, String ngayDauTien) {
        return new SinglePriceMissingException(
                "thiếu giá phòng đơn ở " + soNgay + " ngày khởi hành của " + market,
                Map.of("market", market,
                        "departureCount", soNgay,
                        "firstDepartureDate", ngayDauTien));
    }

    /** Đường khách: đúng một ngày khởi hành, cái mà khách đang xem. */
    public static SinglePriceMissingException cuaNgayKhoiHanh(String departureId) {
        return new SinglePriceMissingException(
                "ngày khởi hành " + departureId + " không có giá phòng đơn",
                Map.of("departureId", departureId));
    }

    public Map<String, Object> params() {
        return params;
    }
}
