package vn.travel.booking.common.exception;

/**
 * Không tìm thấy tài nguyên — <b>gộp ba tình huống có chủ ý</b>: không tồn tại,
 * chưa dịch cho locale này, chưa gán vào thị trường này (docs/13 mục 5.1).
 *
 * <p>Phân biệt ba cái ra ngoài là rò rỉ thông tin về sản phẩm chưa mở bán: kẻ
 * dò chỉ cần so sánh "chưa dịch" với "không tồn tại" là biết sản phẩm nào đang
 * được soạn. Log nội bộ vẫn phân biệt được vì có {@code chiTiet}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String detail) {
        super(detail);
    }
}
