package vn.travel.booking.application.product;

/**
 * Tham số của một lần tìm sản phẩm.
 *
 * <p>{@code market} và {@code locale} là <b>hai trường riêng</b> và luôn phải
 * riêng: market quyết định khách mua gì, locale quyết định khách đọc bằng tiếng
 * gì. Gộp hai thứ này là nhầm lẫn tốn kém nhất của dự án (CLAUDE.md điều 1).
 *
 * <p>{@code regionSlug} và {@code destinationSlug} là slug <b>trong locale đang
 * xem</b>: {@code nordvietnam} với {@code da}, {@code mien-bac} với {@code vi}.
 * Không phải mã miền hay mã điểm đến.
 */
public record ProductQuery(
        String market,
        String locale,
        String regionSlug,
        String destinationSlug,
        ProductType productType,
        String q,
        ProductSort sort,
        int page,
        int size) {

    public int offset() {
        return page * size;
    }
}
