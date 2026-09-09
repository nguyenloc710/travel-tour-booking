package vn.travel.booking.common.exception;

/**
 * Có đăng nhập, nhưng không được phép làm việc này.
 *
 * <p>Khác {@code NotFoundException}: 403 nói "tài nguyên có thật, bạn không có
 * quyền", 404 nói "không có gì ở đây". Ở bề mặt quản trị thì phân biệt được, vì
 * người dùng đã xác thực và danh mục sản phẩm không phải bí mật với họ — khác
 * hẳn bề mặt công khai, nơi 404 cố tình gộp ba tình huống (docs/13 mục 5.1).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String detail) {
        super(detail);
    }
}
