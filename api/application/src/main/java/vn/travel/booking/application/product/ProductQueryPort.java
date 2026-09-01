package vn.travel.booking.application.product;

import vn.travel.booking.application.shared.PagedResult;

import java.util.Optional;

/**
 * Cổng ra ngoài do tầng application khai báo, infrastructure hiện thực.
 */
public interface ProductQueryPort {

    PagedResult<ProductSummary> findProducts(ProductQuery query);

    /**
     * Rỗng khi: không có sản phẩm nào mang slug này, hoặc có nhưng chưa dịch
     * sang locale này, hoặc có nhưng chưa xuất bản ở thị trường này. Ba tình
     * huống trả về cùng một kết quả — docs/13 mục 5.1.
     */
    Optional<ProductDetail> findProduct(String market, String locale, String slug);
}
