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
    private final AdminPriceTierService bacGia;
    private final AdminContentService noiDung;

    public AdminController(
                           AdminProductTranslationService translationService,
                           AdminCatalogService catalog,
                           TranslationWorkService translationWorkService,
                           AdminProductService product,
                           AdminDepartureService departureService,
                           AdminPriceTierService bacGia,
                           AdminContentService noiDung) {
        this.translationService = translationService;
        this.catalog = catalog;
        this.translationWorkService = translationWorkService;
        this.product = product;
        this.departureService = departureService;
        this.bacGia = bacGia;
        this.noiDung = noiDung;
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
    public ResponseEntity<List<AdminProductTranslation>> danhSachBanDich(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(translationService.list(id).stream()
                .map(AdminController::toView)
                .toList());
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/products/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminProductTranslation> luuBanDich(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminProductTranslationInput input
    ) {

        ProductTranslationView daLuu = translationService.save(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(),
                new ProductTranslationInput(
                        input.getSlug(), input.getTitle(), input.getShortDescription(),
                        input.getLongDescription(), input.getWhyChooseThis(),
                        input.getHeroImageAlt(), input.getStatus().getValue()));

        return noCache().body(toView(daLuu));
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
    public ResponseEntity<AdminProductPage> danhSachSanPhamQuanTri(
            @Valid @RequestParam(value = "productType", required = false) @Nullable ProductType productType,
            @Valid @RequestParam(value = "market", required = false) @Nullable String market,
            @Valid @RequestParam(value = "gap", required = false) @Nullable TranslationGap gap,
            @Size(max = 120)  @Valid @RequestParam(value = "q", required = false) @Nullable String q,
            @Min(0)  @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(100)  @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {

        PagedResult<AdminProductRow> ket_qua = catalog.list(new AdminProductQuery(
                productType == null ? null : productType.getValue(),
                market, gap == null ? null : gap.getValue(), q, page, size));

        return noCache().body(new AdminProductPage(
                ket_qua.items().stream().map(AdminController::toView).toList(),
                ket_qua.page(), ket_qua.size(), ket_qua.totalItems(), ket_qua.totalPages()));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/destinations",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<AdminDestination>> danhSachDiemDenQuanTri(
    ) {
        return noCache().body(catalog.destination().stream()
                .map(AdminController::toView)
                .toList());
    }

    private static AdminDestination toView(DestinationOption d) {
        return new AdminDestination(d.id(), d.code(), d.name(), d.regionName());
    }

    // ------------------------------------------------------------ việc dịch

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/translations/queue",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<TranslationQueueItem>> hangDoiDich(
            @Valid @RequestParam(value = "entityType", required = false) @Nullable TranslationEntityType entityType,
            @Min(1) @Max(200)  @Valid @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit
    ) {

        return noCache().body(translationWorkService
                .queue(entityType == null ? null : entityType.getValue(), limit)
                .stream()
                .map(AdminController::toView)
                .toList());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/translations/coverage",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<TranslationCoverageRow>> doPhuDich(
            @Valid @RequestParam(value = "locale", required = false) @Nullable String locale
    ) {
        return noCache().body(translationWorkService.doPhu(locale).stream()
                .map(AdminController::toView)
                .toList());
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
    public ResponseEntity<AdminProductDetail> taoSanPham(
            @Valid @RequestBody AdminProductCreate input
    ) {
        ProductDetailView daTao = product.tao(new ProductCreateInput(
                input.getProductType().getValue(),
                input.getPrimaryDestinationId(),
                nho(input.getDurationDays()),
                input.getHeroImage(),
                input.getMapImage(),
                input.getLayout(),
                input.getIsNew(),
                input.getConsultantId(),
                sangBanDich(input.getSource()),
                block(input.getGroupTour(), input.getIndividualPackage(), input.getPrivateTour(),
                        input.getCruise(), input.getCombo(), input.getDayTour())));

        return noCache(HttpStatus.CREATED).body(toView(daTao));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/products/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminProductDetail> chiTietSanPhamQuanTri(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(toView(product.detail(id)));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/products/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminProductDetail> suaSanPham(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminProductPatch input
    ) {
        ProductDetailView daSua = product.sua(id, new ProductPatchInput(
                input.getPrimaryDestinationId(),
                nho(input.getDurationDays()),
                input.getHeroImage(),
                input.getMapImage(),
                input.getLayout(),
                input.getIsNew(),
                input.getConsultantId(),
                block(input.getGroupTour(), input.getIndividualPackage(), input.getPrivateTour(),
                        input.getCruise(), input.getCombo(), input.getDayTour())));

        return noCache().body(toView(daSua));
    }

    /** Xoá mềm. Chỉ {@code ADMIN}: xoá nhầm một tour đang bán là sự cố không hoàn tác được. */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/admin/products/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> xoaSanPham(
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
    public ResponseEntity<AdminProductMarketState> ganThiTruong(
            @PathVariable("id") UUID id,
            @PathVariable("market") String market,
            @Valid @RequestBody AdminMarketAssignment input
    ) {

        MarketState daGan = product.ganThiTruong(id, market, input.getPublished());
        return noCache().body(toView(daGan));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/products/{id}/price-tiers",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminPriceTier>> luuBacGia(
            @PathVariable("id") UUID id,
            @NotNull  @Valid @RequestParam(value = "market", required = true) String market,
            @Valid@Size(min = 1)  @RequestBody List<@Valid AdminPriceTierInput> thang
    ) {

        return noCache().body(bacGia.save(id, market, thang.stream()
                        .map(b -> new PriceTierInput(nho(b.getMinPax()), nho(b.getMaxPax()),
                                new BigDecimal(b.getPricePerPerson())))
                        .toList())
                .stream()
                .map(AdminController::toView)
                .toList());
    }

    // ------------------------------------------------------------ ngày khởi hành

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/products/{id}/departures",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<List<AdminDeparture>> danhSachNgayKhoiHanhQuanTri(
            @PathVariable("id") UUID id,
            @Valid @RequestParam(value = "market", required = false) @Nullable String market
    ) {
        return noCache().body(departureService.list(id, market).stream()
                .map(AdminController::toView)
                .toList());
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/products/{id}/departures",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDeparture> taoNgayKhoiHanh(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminDepartureCreate input
    ) {
        DepartureView daTao = departureService.tao(id, new DepartureCreateInput(
                input.getMarket().getValue(),
                input.getDepartDate(),
                nho(input.getDays()),
                nho(input.getCapacity()),
                input.getCabinCategory() == null ? null : input.getCabinCategory().getValue(),
                input.getBaseStatus() == null ? null : input.getBaseStatus().getValue(),
                input.getDepartureOriginId()));

        return noCache(HttpStatus.CREATED).body(toView(daTao));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/departures/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDeparture> suaNgayKhoiHanh(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminDeparturePatch input
    ) {
        DepartureView daSua = departureService.sua(id, new DeparturePatchInput(
                input.getDepartDate(),
                nho(input.getDays()),
                nho(input.getCapacity()),
                input.getCabinCategory() == null ? null : input.getCabinCategory().getValue(),
                input.getBaseStatus() == null ? null : input.getBaseStatus().getValue(),
                input.getDepartureOriginId()));

        return noCache().body(toView(daSua));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/products/{id}/departures/copy",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDepartureCopyResult> nhanBanLichKhoiHanh(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminDepartureCopy input
    ) {

        CopyResult ket_qua = departureService.duplicate(id,
                input.getFromMarket().getValue(), input.getToMarket().getValue(),
                input.getFromDate());

        return noCache().body(
                new AdminDepartureCopyResult(ket_qua.created(), ket_qua.skipped()));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/departures/{id}/prices",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminDeparturePrice>> luuGiaNgayKhoiHanh(
            @PathVariable("id") UUID id,
            @Valid@Size(min = 1)  @RequestBody List<@Valid AdminDeparturePriceInput> price
    ) {

        return noCache().body(departureService.savePrices(id, price.stream()
                        .map(g -> new DeparturePriceInput(g.getPaxTypeCode(),
                                g.getOccupancy().getValue(),
                                new BigDecimal(g.getAmount())))
                        .toList())
                .stream()
                .map(AdminController::toView)
                .toList());
    }

    // ------------------------------------------------------------ ánh xạ đợt 5b

    /**
     * {@code Integer} sang {@code Short}.
     *
     * <p>Spec dùng {@code int32} vì JSON không có kiểu 16 bit, còn cột là
     * {@code SMALLINT} và Hibernate chạy {@code ddl-auto: validate} nên entity
     * phải là {@code Short}. Giá trị ngoài dải đã bị {@code @Min}/{@code @Max}
     * của spec chặn trước khi tới đây.
     */
    private static Short nho(Integer count) {
        return count == null ? null : count.shortValue();
    }

    private static Integer lon(Short count) {
        return count == null ? null : count.intValue();
    }

    private static ProductTypeBlocks block(GroupTourFields g, IndividualPackageFields i,
                                          PrivateTourFields p, CruiseFields c,
                                          ComboFields cb, DayTourFields d) {
        return new ProductTypeBlocks(
                g == null ? null : new ProductTypeBlocks.GroupTour(
                        nho(g.getMinPax()), nho(g.getMaxPax()), nho(g.getGuaranteedThreshold()),
                        g.getTourLeaderLanguage().getValue(), nho(g.getFitnessLevel())),
                i == null ? null : new ProductTypeBlocks.IndividualPackage(
                        nho(i.getMinPartySize()), nho(i.getFlexibleDateWindowDays())),
                p == null ? null : new ProductTypeBlocks.PrivateTour(
                        nho(p.getLeadTimeDays()), nho(p.getQuoteValidDays())),
                c == null ? null : new ProductTypeBlocks.Cruise(
                        c.getShipName(), nho(c.getPortCount())),
                cb == null ? null : new ProductTypeBlocks.Combo(
                        nho(cb.getNights()), cb.getValidFrom(), cb.getValidTo()),
                d == null ? null : new ProductTypeBlocks.DayTour(
                        nho(d.getDurationHours()), nho(d.getCutoffHours())));
    }

    private static ProductTranslationInput sangBanDich(AdminProductTranslationInput i) {
        return new ProductTranslationInput(
                i.getSlug(), i.getTitle(), i.getShortDescription(), i.getLongDescription(),
                i.getWhyChooseThis(), i.getHeroImageAlt(), i.getStatus().getValue());
    }

    private static AdminProductDetail toView(ProductDetailView v) {
        AdminProductDetail ra = new AdminProductDetail(
                v.id(), ProductType.fromValue(v.productType()), v.primaryDestinationId(),
                v.heroImage(), v.isNew(), v.reviewCount(),
                v.markets().stream().map(AdminController::toView).toList(),
                v.translations().stream()
                        .map(t -> new AdminTranslationState(t.locale(),
                                TranslationStatus.fromValue(t.status()), t.isSource(), t.outdated()))
                        .toList())
                .durationDays(lon(v.durationDays()))
                .mapImage(v.mapImage())
                .layout(v.layout())
                .rating(v.rating() == null ? null : v.rating().doubleValue())
                .consultantId(v.consultantId())
                .lastModifiedAt(v.lastModifiedAt())
                .lastModifiedBy(v.lastModifiedBy());

        ProductTypeBlocks k = v.blocks();
        if (k.groupTour() != null) {
            ra.setGroupTour(new GroupTourFields(lon(k.groupTour().minPax()),
                    lon(k.groupTour().maxPax()), lon(k.groupTour().guaranteedThreshold()),
                    GroupTourFields.TourLeaderLanguageEnum.fromValue(k.groupTour().tourLeaderLanguage()),
                    lon(k.groupTour().fitnessLevel())));
        }
        if (k.individualPackage() != null) {
            ra.setIndividualPackage(new IndividualPackageFields(
                    lon(k.individualPackage().minPartySize()),
                    lon(k.individualPackage().flexibleDateWindowDays())));
        }
        if (k.privateTour() != null) {
            ra.setPrivateTour(new PrivateTourFields(lon(k.privateTour().leadTimeDays()),
                    lon(k.privateTour().quoteValidDays())));
        }
        if (k.cruise() != null) {
            ra.setCruise(new CruiseFields(k.cruise().shipName(), lon(k.cruise().portCount())));
        }
        if (k.combo() != null) {
            ra.setCombo(new ComboFields(lon(k.combo().nights()),
                    k.combo().validFrom(), k.combo().validTo()));
        }
        if (k.dayTour() != null) {
            ra.setDayTour(new DayTourFields(lon(k.dayTour().durationHours()),
                    lon(k.dayTour().cutoffHours())));
        }
        return ra;
    }

    private static AdminProductMarketState toView(MarketState m) {
        return new AdminProductMarketState(
                AdminProductMarketState.MarketEnum.fromValue(m.market()), m.published());
    }

    private static AdminDeparture toView(DepartureView d) {
        return new AdminDeparture(d.id(),
                AdminDeparture.MarketEnum.fromValue(d.market()),
                d.departDate(), d.returnDate(), lon(d.days()), d.baseStatus(),
                lon(d.capacity()), lon(d.seatsBooked()),
                d.prices().stream().map(AdminController::toView).toList())
                .cabinCategory(d.cabinCategory())
                .departureOriginId(d.departureOriginId());
    }

    private static AdminDeparturePrice toView(DeparturePriceView g) {
        return new AdminDeparturePrice(g.paxTypeCode(), g.occupancy(), tien(g.amount()));
    }

    private static AdminPriceTier toView(PriceTierView t) {
        return new AdminPriceTier(t.id(), AdminPriceTier.MarketEnum.fromValue(t.market()),
                lon(t.minPax()), tien(t.pricePerPerson()))
                .maxPax(lon(t.maxPax()));
    }

    /** {@code amount} là <b>chuỗi</b> trong JSON — số dấu phẩy động của JavaScript làm hỏng tiền. */
    private static Money tien(vn.travel.booking.common.money.Money m) {
        return new Money(m.amount().toPlainString(), m.currency());
    }

    // ------------------------------------------------------------ ánh xạ

    private static AdminProductSummary toView(AdminProductRow r) {
        return new AdminProductSummary(
                r.id(), ProductType.fromValue(r.productType()), r.sourceTitle(),
                TranslationStatus.fromValue(r.sourceStatus()),
                r.markets().stream()
                        .map(m -> new AdminProductMarketState(
                                AdminProductMarketState.MarketEnum.fromValue(m.market()),
                                m.published()))
                        .toList(),
                r.translations().stream()
                        .map(t -> new AdminTranslationState(
                                t.locale(), TranslationStatus.fromValue(t.status()),
                                t.isSource(), t.outdated()))
                        .toList())
                .lastModifiedAt(r.lastModifiedAt());
    }

    private static TranslationQueueItem toView(QueueItem q) {
        return new TranslationQueueItem(
                TranslationEntityType.fromValue(q.entityType()), q.id(), q.locale(),
                TranslationGap.fromValue(q.gap()), q.priority(), q.sourceTitle(),
                q.sourceLastModifiedAt())
                .translatedAt(q.translatedAt());
    }

    private static TranslationCoverageRow toView(CoverageRow c) {
        return new TranslationCoverageRow(
                TranslationEntityType.fromValue(c.entityType()), c.locale(),
                c.total(), c.translated(), c.upToDate());
    }

    private static AdminProductTranslation toView(ProductTranslationView v) {
        return new AdminProductTranslation(
                v.locale(), v.slug(), v.title(), v.shortDescription(),
                v.longDescription(), v.whyChooseThis(), v.heroImageAlt(),
                TranslationStatus.fromValue(v.status()), v.isSource(), v.lastModifiedAt())
                .outdated(v.outdated())
                .lastModifiedBy(v.lastModifiedBy());
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
    public ResponseEntity<AdminDestinationDetail> chiTietDiemDenQuanTri(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(toView(noiDung.destination(id, AcceptLanguages.SOURCE)));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/destinations/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminDestinationDetail> suaDiemDen(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminDestinationPatch input
    ) {

        return noCache().body(toView(noiDung.suaDiemDen(
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
    public ResponseEntity<Void> xoaDiemDen(
            @PathVariable("id") UUID id
    ) {
        noiDung.xoaDiemDen(id, SecurityUtils.currentStaff().id());
        return noCache(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/destinations/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminDestinationDetail> luuBanDichDiemDen(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminDestinationTranslationInput input
    ) {

        return noCache().body(toView(noiDung.luuBanDichDiemDen(
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
    public ResponseEntity<List<AdminTag>> danhSachThe(
    ) {
        return noCache().body(noiDung.tag(AcceptLanguages.SOURCE).stream()
                .map(AdminController::toView).toList());
    }

    // -------------------------------------------------------------- bài viết

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/posts",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminPostPage> danhSachBaiViet(
            @Size(max = 120)  @Valid @RequestParam(value = "q", required = false) @Nullable String q,
            @Min(0)  @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(100)  @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        PagedResult<PostRow> ket_qua = noiDung.danhSachBaiViet(
                q, page, size, AcceptLanguages.SOURCE);

        return noCache().body(new AdminPostPage(
                ket_qua.items().stream().map(AdminController::toView).toList(),
                ket_qua.page(), ket_qua.size(), ket_qua.totalItems(), ket_qua.totalPages()));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/posts",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> taoBaiViet(
            @Valid @RequestBody AdminPostCreate input
    ) {
        return noCache(HttpStatus.CREATED).body(toView(noiDung.taoBaiViet(
                new PostCreateInput(
                        input.getHeroImage(),
                        input.getPublishedAt(),
                        input.getTagIds(),
                        toView(input.getTranslation())),
                AcceptLanguages.SOURCE, SecurityUtils.roles(),
                SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/posts/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> chiTietBaiViet(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(toView(noiDung.post(id, AcceptLanguages.SOURCE)));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/posts/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> suaBaiViet(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPostPatch input
    ) {
        return noCache().body(toView(noiDung.suaBaiViet(
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
    public ResponseEntity<AdminPostDetail> datTheChoBaiViet(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminPostTagAssignment input
    ) {

        return noCache().body(toView(noiDung.datTheChoBaiViet(
                id, input.getTagIds(), AcceptLanguages.SOURCE,
                SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/admin/posts/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<Void> xoaBaiViet(
            @PathVariable("id") UUID id
    ) {
        noiDung.xoaBaiViet(id, SecurityUtils.currentStaff().id());
        return noCache(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/posts/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminPostDetail> luuBanDichBaiViet(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminPostTranslationInput input
    ) {

        return noCache().body(toView(noiDung.luuBanDichBaiViet(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(),
                toView(input), SecurityUtils.currentStaff().id())));
    }

    // ------------------------------------------------------- buổi thuyết trình

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/lectures",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminLecturePage> danhSachSuKienQuanTri(
            @Min(0)  @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(100)  @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        PagedResult<LectureRow> ket_qua = noiDung.listEvents(
                page, size, AcceptLanguages.SOURCE);

        return noCache().body(new AdminLecturePage(
                ket_qua.items().stream().map(AdminController::toView).toList(),
                ket_qua.page(), ket_qua.size(), ket_qua.totalItems(), ket_qua.totalPages()));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/lectures",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminLectureDetail> taoSuKien(
            @Valid @RequestBody AdminLectureCreate input
    ) {
        return noCache(HttpStatus.CREATED).body(toView(noiDung.taoSuKien(
                new LectureCreateInput(
                        input.getMarket().getValue(),
                        input.getEventDate(),
                        gio(input.getStartTime()),
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
    public ResponseEntity<AdminLectureDetail> chiTietSuKien(
            @PathVariable("id") UUID id
    ) {
        return noCache().body(toView(noiDung.suKien(id, AcceptLanguages.SOURCE)));
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/api/v1/admin/lectures/{id}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminLectureDetail> suaSuKien(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminLecturePatch input
    ) {
        return noCache().body(toView(noiDung.suaSuKien(
                id,
                new LecturePatchInput(
                        input.getEventDate(), gio(input.getStartTime()),
                        input.getCity(), input.getVenue(), input.getSeats()),
                AcceptLanguages.SOURCE, SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/admin/lectures/{id}",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<Void> xoaSuKien(
            @PathVariable("id") UUID id
    ) {
        noiDung.xoaSuKien(id, SecurityUtils.currentStaff().id());
        return noCache(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/lectures/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminLectureDetail> luuBanDichSuKien(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminLectureTranslationInput input
    ) {

        return noCache().body(toView(noiDung.luuBanDichSuKien(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(),
                new LectureTranslationInput(input.getTitle(), input.getDescription()),
                SecurityUtils.currentStaff().id())));
    }

    // ---------------------------------------------------- ánh xạ nội dung khác

    private static AdminDestinationDetail toView(DestinationDetailView d) {
        return new AdminDestinationDetail(
                d.id(), d.code(), d.regionId(),
                // Miền chưa có bản dịch ở ngôn ngữ nguồn thì để trống thay vì
                // nổ: đó là dữ liệu thiếu, không phải lỗi lập trình, và màn hình
                // vẫn phải mở được để người ta sửa.
                d.regionName() == null ? "" : d.regionName(),
                d.sortOrder(),
                d.translations().stream().map(AdminController::toView).toList())
                .productCount(d.productCount())
                .lastModifiedAt(d.lastModifiedAt())
                .lastModifiedBy(d.lastModifiedBy());
    }

    private static AdminDestinationTranslation toView(DestinationTranslationView t) {
        return new AdminDestinationTranslation(t.locale(), t.slug(), t.name(), t.isSource())
                .summary(t.summary())
                .lastModifiedAt(t.lastModifiedAt())
                .lastModifiedBy(t.lastModifiedBy());
    }

    private static AdminTag toView(TagView t) {
        return new AdminTag(t.id(), t.code(), t.name());
    }

    private static AdminPostSummary toView(PostRow p) {
        return new AdminPostSummary(
                p.id(), p.title(),
                localeStatus(p.locales()),
                p.tags().stream().map(AdminController::toView).toList())
                .heroImage(p.heroImage())
                .publishedAt(p.publishedAt())
                .lastModifiedAt(p.lastModifiedAt())
                .lastModifiedBy(p.lastModifiedBy());
    }

    private static AdminPostDetail toView(PostDetailView p) {
        return new AdminPostDetail(
                p.id(),
                p.tags().stream().map(AdminController::toView).toList(),
                p.translations().stream().map(AdminController::toView).toList())
                .heroImage(p.heroImage())
                .publishedAt(p.publishedAt())
                .lastModifiedAt(p.lastModifiedAt())
                .lastModifiedBy(p.lastModifiedBy());
    }

    private static AdminPostTranslation toView(PostTranslationView t) {
        return new AdminPostTranslation(
                t.locale(), t.slug(), t.title(), t.excerpt(), t.body(),
                TranslationStatus.fromValue(t.status()), t.isSource())
                .lastModifiedAt(t.lastModifiedAt())
                .lastModifiedBy(t.lastModifiedBy());
    }

    private static PostTranslationInput toView(AdminPostTranslationInput input) {
        return new PostTranslationInput(
                input.getSlug(), input.getTitle(), input.getExcerpt(),
                input.getBody(), input.getStatus().getValue());
    }

    private static AdminLectureSummary toView(LectureRow l) {
        return new AdminLectureSummary(
                l.id(),
                AdminLectureSummary.MarketEnum.fromValue(l.market()),
                l.eventDate(), l.city(), l.seats(), l.seatsTaken(), l.title(),
                localeStatus(l.locales()))
                .startTime(timeText(l.startTime()))
                .venue(l.venue())
                .lastModifiedAt(l.lastModifiedAt())
                .lastModifiedBy(l.lastModifiedBy());
    }

    private static AdminLectureDetail toView(LectureDetailView l) {
        return new AdminLectureDetail(
                l.id(),
                AdminLectureDetail.MarketEnum.fromValue(l.market()),
                l.eventDate(), l.city(), l.seats(), l.seatsTaken(),
                l.translations().stream().map(AdminController::toView).toList())
                .startTime(timeText(l.startTime()))
                .venue(l.venue())
                .lastModifiedAt(l.lastModifiedAt())
                .lastModifiedBy(l.lastModifiedBy());
    }

    private static AdminLectureTranslation toView(LectureTranslationView t) {
        return new AdminLectureTranslation(t.locale(), t.title(), t.description(), t.isSource())
                .lastModifiedAt(t.lastModifiedAt())
                .lastModifiedBy(t.lastModifiedBy());
    }

    private static Map<String, AdminContentLocaleState> localeStatus(
            Map<String, ContentLocaleState> source) {

        Map<String, AdminContentLocaleState> ra = new LinkedHashMap<>();
        source.forEach((locale, status) ->
                ra.put(locale, AdminContentLocaleState.fromValue(status.name())));
        return ra;
    }

    /**
     * Giờ bắt đầu đi qua hợp đồng dưới dạng <b>chuỗi</b> {@code HH:MM}, không
     * phải một kiểu thời gian.
     *
     * <p>Nó là giờ <b>địa phương của buổi thuyết trình</b>, không mang múi giờ,
     * và cũng không phải một thời điểm trên trục thời gian. Cho nó thành
     * {@code date-time} là mời mỗi tầng tự gán một múi giờ khác nhau.
     */
    private static LocalTime gio(String hhmm) {
        return hhmm == null || hhmm.isBlank() ? null : LocalTime.parse(hhmm);
    }

    private static String timeText(LocalTime gio) {
        return gio == null ? null : gio.truncatedTo(ChronoUnit.MINUTES).toString();
    }

    private static ResponseEntity.BodyBuilder noCache() {
        return noCache(HttpStatus.OK);
    }

    private static ResponseEntity.BodyBuilder noCache(HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }
}
