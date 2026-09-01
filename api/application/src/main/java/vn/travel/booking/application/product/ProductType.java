package vn.travel.booking.application.product;

/**
 * Sáu loại sản phẩm — ADR-005, docs/04.
 *
 * <p>Giữ bản sao ở tầng application thay vì dùng thẳng enum sinh từ spec: tầng
 * này không được phụ thuộc vào code sinh ra của tầng web. Hai danh sách lệch
 * nhau thì {@code ProductController} không biên dịch được, nên lệch không âm thầm.
 */
public enum ProductType {
    GROUP_TOUR,
    INDIVIDUAL_PACKAGE,
    PRIVATE_TOUR,
    CRUISE,
    COMBO,
    DAY_TOUR
}
