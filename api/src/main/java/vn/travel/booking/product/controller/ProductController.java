package vn.travel.booking.product.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.travel.booking.common.util.RequestScope;
import vn.travel.booking.product.mapper.ProductWebMapper;
import vn.travel.booking.product.dto.DepartureView;
import vn.travel.booking.product.service.ProductService;
import vn.travel.booking.product.service.ProductContentService;
import vn.travel.booking.product.service.SlugRedirectService;
import vn.travel.booking.product.dto.ProductQuery;
import vn.travel.booking.web.generated.model.Departure;
import vn.travel.booking.web.generated.model.DepartureStatus;
import vn.travel.booking.web.generated.model.HotelStay;
import vn.travel.booking.web.generated.model.ItineraryDay;
import vn.travel.booking.web.generated.model.ProductDetail;
import vn.travel.booking.web.generated.model.SlugRedirect;
import vn.travel.booking.web.generated.model.ProductPage;
import vn.travel.booking.web.generated.model.ProductSort;
import vn.travel.booking.web.generated.model.ProductType;

import java.time.Duration;
import java.util.List;

/**
 * Controller {@code implements} interface SINH RA từ {@code contracts/openapi.yaml}.
 *
 * <p>Đổi spec mà quên sửa chỗ này là <b>lỗi biên dịch</b> chứ không phải bug lúc
 * chạy — lý do duy nhất để chọn spec-first (ADR-002).
 */
@RestController
@Validated
public class ProductController {

    private final ProductService products;
    private final ProductContentService contentService;
    private final SlugRedirectService slugRedirectService;
    private final ProductWebMapper mapper;

    public ProductController(ProductService products,
                             ProductContentService contentService,
                             SlugRedirectService slugRedirectService,
                             ProductWebMapper mapper) {
        this.products = products;
        this.contentService = contentService;
        this.slugRedirectService = slugRedirectService;
        this.mapper = mapper;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/products",
        produces = { "application/json" }
    )
    public ResponseEntity<ProductPage> listProducts(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @Valid @RequestParam(value = "region", required = false) @Nullable String region,
            @Valid @RequestParam(value = "destination", required = false) @Nullable String destination,
            @Valid @RequestParam(value = "theme", required = false) @Nullable List<String> theme,
            @Valid @RequestParam(value = "productType", required = false) @Nullable ProductType productType,
            @Size(max = 100)  @Valid @RequestParam(value = "q", required = false) @Nullable String q,
            @Valid @RequestParam(value = "sort", required = false, defaultValue = "title,asc") ProductSort sort,
            @Min(0)  @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(60)  @Valid @RequestParam(value = "size", required = false, defaultValue = "24") Integer size
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        ProductQuery query = new ProductQuery(
                RequestScope.market(market),
                locale,
                region,
                destination,
                theme,
                productType == null ? null
                        : vn.travel.booking.product.dto.ProductType.valueOf(productType.getValue()),
                q,
                mapper.toDomainSort(sort),
                page == null ? 0 : page,
                size == null ? 24 : size);

        ProductPage body = mapper.toPage(products.list(query));

        // Listing sản phẩm: 60 giây. Ngày khởi hành và giá thì no-store —
        // docs/13 mục 8. Chỗ còn thay đổi từng phút không được cache.
        return response(locale, Duration.ofMinutes(1)).body(body);
    }

    /**
     * Slug cũ trỏ tới đâu bây giờ.
     *
     * <p>Cache <b>một ngày</b>, dài hơn mọi endpoint khác của bề mặt công khai:
     * slug cũ không đổi nữa. Bản ghi đích có thể đổi slug lần thứ hai, nhưng khi
     * đó dòng cũ vẫn trỏ đúng entity — trigger {@code trg_luu_slug_cu} ghi thêm
     * dòng mới chứ không sửa dòng cũ.
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/redirects/{type}/{slug}",
        produces = { "application/json" }
    )
    public ResponseEntity<SlugRedirect> resolveSlug(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("type") String type,
            @PathVariable("slug") String slug
    ) {

        String locale = RequestScope.locale(acceptLanguage);
        String newSlug = slugRedirectService.resolve(RequestScope.market(market), locale, type, slug);

        return response(locale, Duration.ofDays(1)).body(new SlugRedirect(newSlug));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/products/{slug}",
        produces = { "application/json" }
    )
    public ResponseEntity<ProductDetail> getProduct(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("slug") String slug
    ) {
        String locale = RequestScope.locale(acceptLanguage);

        ProductDetail body = mapper.toDetail(
                products.detail(RequestScope.market(market), locale, slug));

        return response(locale, Duration.ofMinutes(1)).body(body);
    }

    /**
     * Chặng dừng của lộ trình — cùng cache một phút với lịch trình: nó dựng từ
     * chính lịch trình đó, nên hai bên hết hạn cùng nhau.
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/products/{slug}/stops",
        produces = { "application/json" }
    )
    public ResponseEntity<List<vn.travel.booking.web.generated.model.ProductStop>> getProductStops(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("slug") String slug
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        List<vn.travel.booking.web.generated.model.ProductStop> body = mapper.toStopList(
                contentService.stops(RequestScope.market(market), locale, slug));

        return response(locale, Duration.ofMinutes(1)).body(body);
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/products/{slug}/itinerary",
        produces = { "application/json" }
    )
    public ResponseEntity<List<ItineraryDay>> getItinerary(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("slug") String slug
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        List<ItineraryDay> body = mapper.toItineraryDayList(
                contentService.itinerary(RequestScope.market(market), locale, slug));

        return response(locale, Duration.ofMinutes(1)).body(body);
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/products/{slug}/hotels",
        produces = { "application/json" }
    )
    public ResponseEntity<List<HotelStay>> getHotelStays(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("slug") String slug
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        List<HotelStay> body = mapper.toHotelStayList(
                contentService.hotelStays(RequestScope.market(market), locale, slug));

        return response(locale, Duration.ofMinutes(1)).body(body);
    }

    /**
     * <b>Không cache.</b> Chỗ còn thay đổi từng phút; hiện số chỗ cũ là dẫn khách
     * vào một giao dịch chắc chắn thất bại ở bước cuối (docs/13 mục 8).
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/products/{slug}/departures",
        produces = { "application/json" }
    )
    public ResponseEntity<List<Departure>> listDepartures(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("slug") String slug
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        List<Departure> body = mapper.toDepartureList(
                contentService.departures(RequestScope.market(market), locale, slug));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    private static ResponseEntity.BodyBuilder response(String locale, Duration age) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                // Thiếu Vary là CDN phục vụ bản tiếng Đan cho khách Việt.
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .cacheControl(CacheControl.maxAge(age).cachePublic());
    }
}
