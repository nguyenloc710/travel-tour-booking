package vn.travel.booking.product.dto;

import java.util.UUID;

/**
 * Sản phẩm đã qua đủ ba cổng: có bản dịch cho locale này, đã xuất bản ở thị
 * trường này, chưa xoá mềm.
 *
 * <p>Dùng cho những endpoint con của sản phẩm ({@code /itinerary},
 * {@code /hotels}, {@code /departures}): chúng phải trả 404 khi sản phẩm cha
 * không nhìn thấy được, chứ không trả một danh sách rỗng. Rỗng nói "tour này
 * chưa có lịch trình"; 404 nói "tour này không tồn tại ở đây" — hai câu khác nhau.
 */
public record VisibleProduct(UUID id, ProductType productType) {
}
