package vn.travel.booking.admin.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.travel.booking.auth.dto.StaffPrincipal;
import vn.travel.booking.common.util.AcceptLanguages;
import vn.travel.booking.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.travel.booking.admin.service.AdminBookingService;
import vn.travel.booking.admin.service.AdminContentService;
import vn.travel.booking.admin.service.AdminUserService;
import vn.travel.booking.admin.service.AdminCatalogService;
import vn.travel.booking.admin.service.AdminDepartureService;
import vn.travel.booking.admin.service.AdminPriceTierService;
import vn.travel.booking.admin.service.AdminProductService;
import vn.travel.booking.admin.service.AdminProductTranslationService;
import vn.travel.booking.quote.service.AdminQuoteService;
import vn.travel.booking.admin.service.TranslationWorkService;
import vn.travel.booking.admin.dto.AdminBookingDetailView;
import vn.travel.booking.admin.dto.AdminBookingQuery;
import vn.travel.booking.admin.dto.AdminBookingRow;
import vn.travel.booking.admin.dto.BookingEventRow;
import vn.travel.booking.admin.dto.BookingPassengerRow;
import vn.travel.booking.admin.dto.AdminProductQuery;
import vn.travel.booking.admin.dto.CopyResult;
import vn.travel.booking.admin.dto.DepartureCreateInput;
import vn.travel.booking.admin.dto.DeparturePatchInput;
import vn.travel.booking.admin.dto.DeparturePriceInput;
import vn.travel.booking.admin.dto.DeparturePriceView;
import vn.travel.booking.admin.dto.DestinationOption;
import vn.travel.booking.admin.dto.DepartureView;
import vn.travel.booking.admin.dto.MarketState;
import vn.travel.booking.admin.dto.PriceTierInput;
import vn.travel.booking.admin.dto.PriceTierView;
import vn.travel.booking.admin.dto.ProductCreateInput;
import vn.travel.booking.admin.dto.ProductDetailView;
import vn.travel.booking.admin.dto.ProductPatchInput;
import vn.travel.booking.product.dto.ProductTypeBlocks;
import vn.travel.booking.quote.dto.AdminQuoteDetailView;
import vn.travel.booking.quote.dto.AdminQuoteQuery;
import vn.travel.booking.quote.dto.AdminQuoteRow;
import vn.travel.booking.quote.dto.QuoteLineDraft;
import vn.travel.booking.quote.dto.QuoteLineRow;
import vn.travel.booking.admin.dto.AdminProductRow;
import vn.travel.booking.admin.dto.ContentLocaleState;
import vn.travel.booking.admin.dto.DestinationDetailView;
import vn.travel.booking.admin.dto.DestinationTranslationInput;
import vn.travel.booking.admin.dto.DestinationTranslationView;
import vn.travel.booking.admin.dto.LectureCreateInput;
import vn.travel.booking.admin.dto.LectureDetailView;
import vn.travel.booking.admin.dto.LecturePatchInput;
import vn.travel.booking.admin.dto.LectureRow;
import vn.travel.booking.admin.dto.LectureTranslationInput;
import vn.travel.booking.admin.dto.LectureTranslationView;
import vn.travel.booking.admin.dto.PostCreateInput;
import vn.travel.booking.admin.dto.PostDetailView;
import vn.travel.booking.admin.dto.PostPatchInput;
import vn.travel.booking.admin.dto.PostRow;
import vn.travel.booking.admin.dto.PostTranslationInput;
import vn.travel.booking.admin.dto.PostTranslationView;
import vn.travel.booking.admin.dto.StaffUserView;
import vn.travel.booking.admin.dto.TagView;
import vn.travel.booking.admin.dto.CoverageRow;
import vn.travel.booking.admin.dto.ProductTranslationInput;
import vn.travel.booking.admin.dto.ProductTranslationView;
import vn.travel.booking.admin.dto.QueueItem;
import vn.travel.booking.admin.mapper.AdminDepartureWebMapper;
import vn.travel.booking.admin.mapper.AdminLectureMapper;
import vn.travel.booking.admin.mapper.AdminPostMapper;
import vn.travel.booking.admin.mapper.AdminProductWebMapper;
import vn.travel.booking.admin.mapper.AdminTranslationWorkMapper;
import vn.travel.booking.destination.mapper.AdminDestinationMapper;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.pricing.mapper.PricingMapper;
import vn.travel.booking.web.generated.model.AdminDeparture;
import vn.travel.booking.web.generated.model.AdminDestination;
import vn.travel.booking.web.generated.model.AdminDepartureCopy;
import vn.travel.booking.web.generated.model.AdminDepartureCopyResult;
import vn.travel.booking.web.generated.model.AdminDepartureCreate;
import vn.travel.booking.web.generated.model.AdminDeparturePatch;
import vn.travel.booking.web.generated.model.AdminDeparturePrice;
import vn.travel.booking.web.generated.model.AdminDeparturePriceInput;
import vn.travel.booking.web.generated.model.AdminMarketAssignment;
import vn.travel.booking.web.generated.model.AdminPriceTier;
import vn.travel.booking.web.generated.model.AdminPriceTierInput;
import vn.travel.booking.web.generated.model.AdminProductCreate;
import vn.travel.booking.web.generated.model.AdminProductDetail;
import vn.travel.booking.web.generated.model.AdminProductMarketState;
import vn.travel.booking.web.generated.model.AdminProductPatch;
import vn.travel.booking.web.generated.model.ComboFields;
import vn.travel.booking.web.generated.model.CruiseFields;
import vn.travel.booking.web.generated.model.DayTourFields;
import vn.travel.booking.web.generated.model.GroupTourFields;
import vn.travel.booking.web.generated.model.IndividualPackageFields;
import vn.travel.booking.web.generated.model.Money;
import vn.travel.booking.web.generated.model.PrivateTourFields;
import vn.travel.booking.web.generated.model.AdminProductPage;
import vn.travel.booking.web.generated.model.AdminProductSummary;
import vn.travel.booking.web.generated.model.AdminProductTranslation;
import vn.travel.booking.web.generated.model.AdminProductTranslationInput;
import vn.travel.booking.web.generated.model.AdminTranslationState;
import vn.travel.booking.web.generated.model.ProductType;
import vn.travel.booking.web.generated.model.TranslationCoverageRow;
import vn.travel.booking.web.generated.model.TranslationEntityType;
import vn.travel.booking.web.generated.model.TranslationGap;
import vn.travel.booking.web.generated.model.TranslationQueueItem;
import vn.travel.booking.web.generated.model.LoginRequest;
import vn.travel.booking.web.generated.model.StaffProfile;
import vn.travel.booking.web.generated.model.TranslationStatus;
import vn.travel.booking.web.generated.model.AdminBookingDetail;
import vn.travel.booking.web.generated.model.AdminBookingEvent;
import vn.travel.booking.web.generated.model.AdminBookingPage;
import vn.travel.booking.web.generated.model.AdminBookingPassenger;
import vn.travel.booking.web.generated.model.AdminBookingScope;
import vn.travel.booking.web.generated.model.AdminBookingStatusChange;
import vn.travel.booking.web.generated.model.AdminBookingSummary;
import vn.travel.booking.web.generated.model.AdminContentLocaleState;
import vn.travel.booking.web.generated.model.AdminDestinationDetail;
import vn.travel.booking.web.generated.model.AdminDestinationPatch;
import vn.travel.booking.web.generated.model.AdminDestinationTranslation;
import vn.travel.booking.web.generated.model.AdminDestinationTranslationInput;
import vn.travel.booking.web.generated.model.AdminLectureCreate;
import vn.travel.booking.web.generated.model.AdminLectureDetail;
import vn.travel.booking.web.generated.model.AdminLecturePage;
import vn.travel.booking.web.generated.model.AdminLecturePatch;
import vn.travel.booking.web.generated.model.AdminLectureSummary;
import vn.travel.booking.web.generated.model.AdminLectureTranslation;
import vn.travel.booking.web.generated.model.AdminLectureTranslationInput;
import vn.travel.booking.web.generated.model.AdminPostCreate;
import vn.travel.booking.web.generated.model.AdminPostDetail;
import vn.travel.booking.web.generated.model.AdminPostPage;
import vn.travel.booking.web.generated.model.AdminPostPatch;
import vn.travel.booking.web.generated.model.AdminPostSummary;
import vn.travel.booking.web.generated.model.AdminPostTagAssignment;
import vn.travel.booking.web.generated.model.AdminPostTranslation;
import vn.travel.booking.web.generated.model.AdminPostTranslationInput;
import vn.travel.booking.web.generated.model.AdminRoleAssignment;
import vn.travel.booking.web.generated.model.AdminStaffUser;
import vn.travel.booking.web.generated.model.AdminStaffUserPatch;
import vn.travel.booking.web.generated.model.AdminTag;
import vn.travel.booking.web.generated.model.AdminQuoteDetail;
import vn.travel.booking.web.generated.model.AdminQuoteFilter;
import vn.travel.booking.web.generated.model.AdminQuotePage;
import vn.travel.booking.web.generated.model.AdminQuoteLinesInput;
import vn.travel.booking.web.generated.model.AdminQuoteStatusChange;
import vn.travel.booking.web.generated.model.AdminQuoteSummary;
import vn.travel.booking.web.generated.model.QuoteLine;
import vn.travel.booking.web.generated.model.QuoteStatus;
// Trạng thái đơn ở đây là kiểu SINH TỪ SPEC, không phải enum trong domain. Hai
// cái trùng tên và luôn trùng giá trị; chỗ nào cần enum domain thì gọi đủ tên.
import vn.travel.booking.web.generated.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bề mặt quản trị.
 *
 * <p>Khác bề mặt công khai ở bốn điểm (docs/13 mục 2), và điểm cuối là điểm dễ
 * quên nhất: <b>mọi phản hồi ở đây là {@code no-store}</b>. Nội dung chưa xuất
 * bản không được nằm trong bất kỳ cache nào — kể cả cache của trình duyệt trên
 * máy nhân viên.
 */
@RestController
@Validated
public class AdminController {

    private final AdminProductTranslationService translationService;
    private final AdminCatalogService catalog;
    private final TranslationWorkService translationWorkService;
    private final AdminProductService product;
    private final AdminDepartureService departureService;
    private final AdminPriceTierService priceTierService;
    private final AdminContentService contentService;
    private final AdminDestinationMapper destinationMapper;
    private final AdminProductWebMapper productMapper;
    private final AdminDepartureWebMapper departureMapper;
    private final AdminPostMapper postMapper;
    private final AdminLectureMapper lectureMapper;
    private final AdminTranslationWorkMapper translationWorkMapper;

    public AdminController(
            AdminProductTranslationService translationService,
            AdminCatalogService catalog,
            TranslationWorkService translationWorkService,
            AdminProductService product,
            AdminDepartureService departureService,
            AdminPriceTierService priceTierService,
            AdminContentService contentService,
            AdminDestinationMapper destinationMapper,
            AdminProductWebMapper productMapper,
            AdminDepartureWebMapper departureMapper,
            AdminPostMapper postMapper,
            AdminLectureMapper lectureMapper,
            AdminTranslationWorkMapper translationWorkMapper) {
        this.translationService = translationService;
        this.catalog = catalog;
        this.translationWorkService = translationWorkService;
        this.product = product;
        this.departureService = departureService;
        this.priceTierService = priceTierService;
        this.contentService = contentService;
        this.destinationMapper = destinationMapper;
        this.productMapper = productMapper;
        this.departureMapper = departureMapper;
        this.postMapper = postMapper;
        this.lectureMapper = lectureMapper;
        this.translationWorkMapper = translationWorkMapper;
    }

    // ------------------------------------------------------------ bản dịch

    /**
     * {@code @PreAuthorize} chỉ trả lời "ai được vào cửa". Luật "vào rồi thì sửa
     * được bản nào" phụ thuộc locale và nằm ở tầng nghiệp vụ — xem
     * {@code AdminProductTranslationService}.
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/products/{id}/translations",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN','CONSULTANT')")
    public ResponseEntity<List<AdminProductTranslation>> listTranslations(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(productMapper.toTranslationList(translationService.list(id)));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/products/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminProductTranslation> saveTranslation(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminProductTranslationInput input
    ) {

        ProductTranslationView saved = translationService.save(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(),
                productMapper.toTranslationInput(input));

        return noCache().body(productMapper.toTranslation(saved));
    }

    // ------------------------------------------------------------ danh mục

    /**
     * Danh sách sản phẩm (docs/22 M2). Cả bốn vai trò đều <b>đọc</b> được — ma
     * trận quyền ở docs/22 mục 2.1 cho `CONSULTANT` và `TRANSLATOR` quyền R với
     * bản `da`. Quyền <b>ghi</b> mới phân theo vai trò và theo locale.
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/products",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminProductPage> listProducts(
            @Valid @RequestParam(value = "productType", required = false) @Nullable ProductType productType,
            @Valid @RequestParam(value = "market", required = false) @Nullable String market,
            @Valid @RequestParam(value = "gap", required = false) @Nullable TranslationGap gap,
            @Size(max = 120)  @Valid @RequestParam(value = "q", required = false) @Nullable String q,
            @Min(0)  @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(100)  @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {

        PagedResult<AdminProductRow> result = catalog.list(new AdminProductQuery(
                productType == null ? null : productType.getValue(),
                market, gap == null ? null : gap.getValue(), q, page, size));

        return noCache().body(productMapper.toPage(result));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/destinations",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<AdminDestination>> listDestinations(
    ) {
        return noCache().body(catalog.destination().stream()
                .map(destinationMapper::toOption)
                .toList());
    }

    // ------------------------------------------------------------ việc dịch

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/translations/queue",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<TranslationQueueItem>> translationQueue(
            @Valid @RequestParam(value = "entityType", required = false) @Nullable TranslationEntityType entityType,
            @Min(1) @Max(200)  @Valid @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit
    ) {

        return noCache().body(translationWorkMapper.toQueueItemList(
                translationWorkService.queue(entityType == null ? null : entityType.getValue(), limit)));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/translations/coverage",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<TranslationCoverageRow>> translationCoverage(
            @Valid @RequestParam(value = "locale", required = false) @Nullable String locale
    ) {
        return noCache().body(translationWorkMapper.toCoverageRowList(
                translationWorkService.coverage(locale)));
    }

    // ------------------------------------------------------------ sản phẩm: ghi
    //
    // Quyền theo ma trận docs/22 mục 2.1:
    //   nội dung sản phẩm  → EDITOR, ADMIN
    //   thị trường và giá  → CHỈ ADMIN
    //
    // Hai dòng đó là hai câu chuyện khác nhau, không phải hai mức của cùng một
    // câu chuyện: người viết mô tả tour không nên chạm được vào con số khách
    // phải trả, và người gán thị trường là người chịu trách nhiệm doanh thu.

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/products",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminProductDetail> createProduct(
            @Valid @RequestBody AdminProductCreate input
    ) {
        ProductDetailView created = product.create(new ProductCreateInput(
                input.getProductType().getValue(),
                input.getPrimaryDestinationId(),
                productMapper.toShort(input.getDurationDays()),
                input.getHeroImage(),
                input.getMapImage(),
                input.getLayout(),
                input.getIsNew(),
                input.getConsultantId(),
                productMapper.toTranslationInput(input.getSource()),
                productMapper.block(input.getGroupTour(), input.getIndividualPackage(), input.getPrivateTour(),
                        input.getCruise(), input.getCombo(), input.getDayTour())));

        return noCache(HttpStatus.CREATED).body(productMapper.toDetail(created));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/products/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminProductDetail> getProductDetail(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(productMapper.toDetail(product.detail(id)));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/products/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminProductDetail> patchProduct(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminProductPatch input
    ) {
        ProductDetailView patched = product.update(id, new ProductPatchInput(
                input.getPrimaryDestinationId(),
                productMapper.toShort(input.getDurationDays()),
                input.getHeroImage(),
                input.getMapImage(),
                input.getLayout(),
                input.getIsNew(),
                input.getConsultantId(),
                productMapper.block(input.getGroupTour(), input.getIndividualPackage(), input.getPrivateTour(),
                        input.getCruise(), input.getCombo(), input.getDayTour())));

        return noCache().body(productMapper.toDetail(patched));
    }

    /** Xoá mềm. Chỉ {@code ADMIN}: xoá nhầm một tour đang bán là sự cố không hoàn tác được. */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/admin/products/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteProduct(
            @PathVariable("id") UUID id
    ) {
        product.softDelete(id);
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/products/{id}/markets/{market}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProductMarketState> setProductMarket(
            @PathVariable("id") UUID id,
            @PathVariable("market") String market,
            @Valid @RequestBody AdminMarketAssignment input
    ) {

        MarketState assigned = product.setProductMarket(id, market, input.getPublished());
        return noCache().body(productMapper.toMarketState(assigned));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/products/{id}/price-tiers",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminPriceTier>> savePriceTiers(
            @PathVariable("id") UUID id,
            @NotNull  @Valid @RequestParam(value = "market", required = true) String market,
            @Valid@Size(min = 1)  @RequestBody List<@Valid AdminPriceTierInput> tiers
    ) {

        return noCache().body(departureMapper.toPriceTierList(priceTierService.save(id, market, tiers.stream()
                        .map(b -> new PriceTierInput(departureMapper.toShort(b.getMinPax()), departureMapper.toShort(b.getMaxPax()),
                                new BigDecimal(b.getPricePerPerson())))
                        .toList())));
    }

    // ------------------------------------------------------------ ngày khởi hành

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/products/{id}/departures",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<List<AdminDeparture>> listDepartures(
            @PathVariable("id") UUID id,
            @Valid @RequestParam(value = "market", required = false) @Nullable String market
    ) {
        return noCache().body(departureMapper.toDepartureList(departureService.list(id, market)));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/products/{id}/departures",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDeparture> createDeparture(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminDepartureCreate input
    ) {
        DepartureView created = departureService.create(id, new DepartureCreateInput(
                input.getMarket().getValue(),
                input.getDepartDate(),
                departureMapper.toShort(input.getDays()),
                departureMapper.toShort(input.getCapacity()),
                input.getCabinCategory() == null ? null : input.getCabinCategory().getValue(),
                input.getBaseStatus() == null ? null : input.getBaseStatus().getValue(),
                input.getDepartureOriginId()));

        return noCache(HttpStatus.CREATED).body(departureMapper.toDeparture(created));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/departures/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDeparture> patchDeparture(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminDeparturePatch input
    ) {
        DepartureView patched = departureService.update(id, new DeparturePatchInput(
                input.getDepartDate(),
                departureMapper.toShort(input.getDays()),
                departureMapper.toShort(input.getCapacity()),
                input.getCabinCategory() == null ? null : input.getCabinCategory().getValue(),
                input.getBaseStatus() == null ? null : input.getBaseStatus().getValue(),
                input.getDepartureOriginId()));

        return noCache().body(departureMapper.toDeparture(patched));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/products/{id}/departures/copy",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDepartureCopyResult> duplicateDepartures(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminDepartureCopy input
    ) {

        CopyResult result = departureService.duplicate(id,
                input.getFromMarket().getValue(), input.getToMarket().getValue(),
                input.getFromDate());

        return noCache().body(
                new AdminDepartureCopyResult(result.created(), result.skipped()));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/departures/{id}/prices",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminDeparturePrice>> saveDeparturePrices(
            @PathVariable("id") UUID id,
            @Valid@Size(min = 1)  @RequestBody List<@Valid AdminDeparturePriceInput> price
    ) {

        return noCache().body(departureMapper.toDeparturePriceList(departureService.savePrices(id, price.stream()
                        .map(g -> new DeparturePriceInput(g.getPaxTypeCode(),
                                g.getOccupancy().getValue(),
                                new BigDecimal(g.getAmount())))
                        .toList())));
    }



    // ------------------------------------------------------------ nội dung khác
    //
    // Ma trận quyền docs/22 mục 2.1, dòng "Nội dung khác: điểm đến, bài viết,
    // sự kiện": CONSULTANT R, EDITOR W, TRANSLATOR W (bản `vi`), ADMIN W.
    //
    // `@PreAuthorize` ở đây chỉ trả lời "ai được vào cửa". Luật "vào rồi thì sửa
    // được locale nào" nằm ở AdminContentService, vì nó phụ thuộc cả vai trò lẫn
    // locale đang sửa — thứ một biểu thức trên chữ ký phương thức không nói được.

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/destinations/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminDestinationDetail> getDestinationDetail(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(destinationMapper.toDetail(contentService.destination(id, AcceptLanguages.SOURCE)));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/destinations/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminDestinationDetail> patchDestination(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminDestinationPatch input
    ) {

        return noCache().body(destinationMapper.toDetail(contentService.updateDestination(
                id, input.getRegionId(), input.getSortOrder(),
                AcceptLanguages.SOURCE, SecurityUtils.currentStaff().id())));
    }

    /**
     * Xoá mềm một điểm đến — <b>chỉ {@code ADMIN}</b>.
     *
     * <p>Hẹp hơn quyền sửa có chủ ý. Sửa tên một điểm đến là việc biên tập; xoá
     * nó đi có thể làm cả một nhóm sản phẩm biến mất khỏi website mà không có
     * lỗi nào ghi ra, và đó là quyết định ở tầng khác.
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/admin/destinations/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteDestination(
            @PathVariable("id") UUID id
    ) {
        contentService.deleteDestination(id, SecurityUtils.currentStaff().id());
        return noCache(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/destinations/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminDestinationDetail> saveDestinationTranslation(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminDestinationTranslationInput input
    ) {

        return noCache().body(destinationMapper.toDetail(contentService.saveDestinationTranslation(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(),
                new DestinationTranslationInput(
                        input.getSlug(), input.getName(), input.getSummary()),
                SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/tags",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<AdminTag>> listTags(
    ) {
        return noCache().body(postMapper.toTagList(contentService.tags(AcceptLanguages.SOURCE)));
    }

    // -------------------------------------------------------------- bài viết

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/posts",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminPostPage> listPosts(
            @Size(max = 120)  @Valid @RequestParam(value = "q", required = false) @Nullable String q,
            @Min(0)  @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(100)  @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        PagedResult<PostRow> result = contentService.listPosts(
                q, page, size, AcceptLanguages.SOURCE);

        return noCache().body(postMapper.toPage(result));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/posts",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> createPost(
            @Valid @RequestBody AdminPostCreate input
    ) {
        return noCache(HttpStatus.CREATED).body(postMapper.toDetail(contentService.createPost(
                new PostCreateInput(
                        input.getHeroImage(),
                        input.getPublishedAt(),
                        input.getTagIds(),
                        postMapper.toInput(input.getTranslation())),
                AcceptLanguages.SOURCE, SecurityUtils.roles(),
                SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/posts/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> getPostDetail(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(postMapper.toDetail(contentService.post(id, AcceptLanguages.SOURCE)));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/posts/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> patchPost(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPostPatch input
    ) {
        return noCache().body(postMapper.toDetail(contentService.updatePost(
                id,
                new PostPatchInput(input.getHeroImage(), input.getPublishedAt()),
                AcceptLanguages.SOURCE, SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/posts/{id}/tags",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> setPostTags(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPostTagAssignment input
    ) {

        return noCache().body(postMapper.toDetail(contentService.setPostTags(
                id, input.getTagIds(), AcceptLanguages.SOURCE,
                SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/admin/posts/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<Void> deletePost(
            @PathVariable("id") UUID id
    ) {
        contentService.deletePost(id, SecurityUtils.currentStaff().id());
        return noCache(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/posts/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> savePostTranslation(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminPostTranslationInput input
    ) {

        return noCache().body(postMapper.toDetail(contentService.savePostTranslation(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(),
                postMapper.toInput(input), SecurityUtils.currentStaff().id())));
    }

    // ------------------------------------------------------- buổi thuyết trình

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/lectures",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminLecturePage> listEvents(
            @Min(0)  @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(100)  @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        PagedResult<LectureRow> result = contentService.listEvents(
                page, size, AcceptLanguages.SOURCE);

        return noCache().body(lectureMapper.toPage(result));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/lectures",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminLectureDetail> createEvent(
            @Valid @RequestBody AdminLectureCreate input
    ) {
        return noCache(HttpStatus.CREATED).body(lectureMapper.toDetail(contentService.createEvent(
                new LectureCreateInput(
                        input.getMarket().getValue(),
                        input.getEventDate(),
                        parseTime(input.getStartTime()),
                        input.getCity(),
                        input.getVenue(),
                        input.getSeats(),
                        new LectureTranslationInput(
                                input.getTranslation().getTitle(),
                                input.getTranslation().getDescription())),
                AcceptLanguages.SOURCE, SecurityUtils.roles(),
                SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/lectures/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminLectureDetail> getEventDetail(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(lectureMapper.toDetail(contentService.getEvent(id, AcceptLanguages.SOURCE)));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/lectures/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminLectureDetail> patchEvent(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminLecturePatch input
    ) {
        return noCache().body(lectureMapper.toDetail(contentService.updateEvent(
                id,
                new LecturePatchInput(
                        input.getEventDate(), parseTime(input.getStartTime()),
                        input.getCity(), input.getVenue(), input.getSeats()),
                AcceptLanguages.SOURCE, SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/admin/lectures/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable("id") UUID id
    ) {
        contentService.deleteEvent(id, SecurityUtils.currentStaff().id());
        return noCache(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/lectures/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminLectureDetail> saveEventTranslation(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminLectureTranslationInput input
    ) {

        return noCache().body(lectureMapper.toDetail(contentService.saveEventTranslation(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(),
                new LectureTranslationInput(input.getTitle(), input.getDescription()),
                SecurityUtils.currentStaff().id())));
    }

    /**
     * Giờ bắt đầu đi qua hợp đồng dưới dạng <b>chuỗi</b> {@code HH:MM}, không
     * phải một kiểu thời gian.
     *
     * <p>Nó là giờ <b>địa phương của buổi thuyết trình</b>, không mang múi giờ,
     * và cũng không phải một thời điểm trên trục thời gian. Cho nó thành
     * {@code date-time} là mời mỗi tầng tự gán một múi giờ khác nhau.
     */
    private static LocalTime parseTime(String hhmm) {
        return hhmm == null || hhmm.isBlank() ? null : LocalTime.parse(hhmm);
    }

    private static String timeText(LocalTime time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.MINUTES).toString();
    }

    private static ResponseEntity.BodyBuilder noCache() {
        return noCache(HttpStatus.OK);
    }

    private static ResponseEntity.BodyBuilder noCache(HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }
}
