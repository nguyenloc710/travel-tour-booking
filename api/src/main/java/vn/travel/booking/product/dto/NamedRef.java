package vn.travel.booking.product.dto;

/**
 * Tham chiếu tới một thực thể khác: slug và tên, đã dịch sang locale đang xem.
 *
 * <p>Cả hai trường đều <b>phụ thuộc locale</b>. Slug của locale này không mở
 * được ở locale kia — đó là chủ ý, không phải thiếu sót (docs/02 mục 5).
 */
public record NamedRef(String slug, String name) {
}
