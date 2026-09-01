package vn.travel.booking.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.application.product.GetProductUseCase;
import vn.travel.booking.application.product.ListProductsUseCase;
import vn.travel.booking.application.product.ProductQuery;
import vn.travel.booking.web.generated.api.ProductsApi;
import vn.travel.booking.web.generated.model.ProductDetail;
import vn.travel.booking.web.generated.model.ProductPage;
import vn.travel.booking.web.generated.model.ProductSort;
import vn.travel.booking.web.generated.model.ProductType;

import java.time.Duration;

/**
 * Controller {@code implements} interface SINH RA từ {@code contracts/openapi.yaml}.
 *
 * <p>Đổi spec mà quên sửa chỗ này là <b>lỗi biên dịch</b> chứ không phải bug lúc
 * chạy — lý do duy nhất để chọn spec-first (ADR-002).
 */
@RestController
public class ProductController implements ProductsApi {

    private final ListProductsUseCase listProducts;
    private final GetProductUseCase getProduct;

    public ProductController(ListProductsUseCase listProducts, GetProductUseCase getProduct) {
        this.listProducts = listProducts;
        this.getProduct = getProduct;
    }

    @Override
    public ResponseEntity<ProductPage> listProducts(
            String market,
            String acceptLanguage,
            String region,
            String destination,
            ProductType productType,
            String q,
            ProductSort sort,
            Integer page,
            Integer size) {

        String locale = RequestScope.locale(acceptLanguage);

        ProductQuery truyVan = new ProductQuery(
                RequestScope.market(market),
                locale,
                region,
                destination,
                productType == null ? null
                        : vn.travel.booking.application.product.ProductType.valueOf(productType.getValue()),
                q,
                sortCuaUngDung(sort),
                page == null ? 0 : page,
                size == null ? 24 : size);

        ProductPage than = ProductMapper.sangTrang(listProducts.execute(truyVan));

        // Listing sản phẩm: 60 giây. Ngày khởi hành và giá thì no-store —
        // docs/13 mục 8. Chỗ còn thay đổi từng phút không được cache.
        return phanHoi(locale, Duration.ofMinutes(1)).body(than);
    }

    @Override
    public ResponseEntity<ProductDetail> getProduct(String market, String acceptLanguage, String slug) {
        String locale = RequestScope.locale(acceptLanguage);

        ProductDetail than = ProductMapper.sangChiTiet(
                getProduct.execute(RequestScope.market(market), locale, slug));

        return phanHoi(locale, Duration.ofMinutes(1)).body(than);
    }

    private static ResponseEntity.BodyBuilder phanHoi(String locale, Duration tuoi) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                // Thiếu Vary là CDN phục vụ bản tiếng Đan cho khách Việt.
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .cacheControl(CacheControl.maxAge(tuoi).cachePublic());
    }

    /**
     * Enum sinh ra mang giá trị dạng {@code "title,asc"}; enum của tầng
     * application mang tên hằng. Dịch bằng tên hằng chứ không bằng giá trị, để
     * đổi cách viết trong spec không kéo theo sửa tầng application.
     */
    private static vn.travel.booking.application.product.ProductSort sortCuaUngDung(ProductSort sort) {
        ProductSort thuc_te = sort == null ? ProductSort.TITLE_ASC : sort;
        return vn.travel.booking.application.product.ProductSort.valueOf(thuc_te.name());
    }
}
