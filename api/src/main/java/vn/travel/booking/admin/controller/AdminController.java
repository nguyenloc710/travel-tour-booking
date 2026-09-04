package vn.travel.booking.admin.controller;

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
import vn.travel.booking.admin.dto.CoverageRow;
import vn.travel.booking.admin.dto.ProductTranslationInput;
import vn.travel.booking.admin.dto.ProductTranslationView;
import vn.travel.booking.admin.dto.QueueItem;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.pricing.mapper.PricingMapper;
import vn.travel.booking.web.generated.api.AdminApi;
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
import java.util.List;
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
public class AdminController implements AdminApi {

    private final AuthenticationManager xacThuc;
    private final AdminProductTranslationService banDich;
    private final AdminCatalogService danhMuc;
    private final TranslationWorkService congViecDich;
    private final AdminProductService sanPham;
    private final AdminDepartureService ngayKhoiHanh;
    private final AdminPriceTierService bacGia;
    private final AdminBookingService donDat;
    private final AdminQuoteService baoGia;
    private final SecurityContextRepository khoPhien = new HttpSessionSecurityContextRepository();

    public AdminController(AuthenticationManager xacThuc,
                           AdminProductTranslationService banDich,
                           AdminCatalogService danhMuc,
                           TranslationWorkService congViecDich,
                           AdminProductService sanPham,
                           AdminDepartureService ngayKhoiHanh,
                           AdminPriceTierService bacGia,
                           AdminBookingService donDat,
                           AdminQuoteService baoGia) {
        this.xacThuc = xacThuc;
        this.banDich = banDich;
        this.danhMuc = danhMuc;
        this.congViecDich = congViecDich;
        this.sanPham = sanPham;
        this.ngayKhoiHanh = ngayKhoiHanh;
        this.bacGia = bacGia;
        this.donDat = donDat;
        this.baoGia = baoGia;
    }

    // ------------------------------------------------------------ phiên

    /**
     * Đăng nhập. Phản hồi <b>không mang token</b>: phiên nằm trong cookie
     * {@code HttpOnly}, thứ mã chèn vào trang không đọc được (docs/22 mục 9).
     */
    @Override
    public ResponseEntity<Void> dangNhap(LoginRequest loginRequest) {
        // Interface sinh từ spec chỉ mang thân yêu cầu; request và response lấy
        // từ ngữ cảnh servlet. Thêm tham số vào chữ ký là lệch interface, và
        // lệch interface là lỗi biên dịch — đúng như ADR-002 muốn.
        HttpServletRequest yeuCau = servlet().getRequest();
        HttpServletResponse phanHoi = servlet().getResponse();

        Authentication ketQua;
        try {
            ketQua = xacThuc.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(), loginRequest.getPassword()));
        } catch (BadCredentialsException | org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            // Một câu trả lời cho cả sai email lẫn sai mật khẩu — phân biệt hai
            // cái là cho phép dò xem địa chỉ nào có trong hệ thống.
            return ResponseEntity.status(401).build();
        }

        SecurityContext ngu_canh = SecurityContextHolder.createEmptyContext();
        ngu_canh.setAuthentication(ketQua);
        SecurityContextHolder.setContext(ngu_canh);

        // Chống cố định phiên: đổi id phiên đang có, hoặc tạo mới nếu chưa có.
        // Gọi changeSessionId() khi chưa có phiên nào thì Tomcat ném
        // IllegalStateException — và lời gọi đăng nhập đầu tiên chính là lúc đó.
        if (yeuCau.getSession(false) != null) {
            yeuCau.changeSessionId();
        } else {
            yeuCau.getSession(true);
        }
        khoPhien.saveContext(ngu_canh, yeuCau, phanHoi);

        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @Override
    public ResponseEntity<Void> dangXuat() {
        HttpSession phien = servlet().getRequest().getSession(false);
        if (phien != null) {
            phien.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @Override
    public ResponseEntity<StaffProfile> hoSoNhanVien() {
        StaffPrincipal nhanVien = SecurityUtils.nhanVienHienTai();

        return khongCache().body(new StaffProfile(
                nhanVien.id(), nhanVien.email(), nhanVien.displayName(),
                nhanVien.roleList().stream()
                        .map(StaffProfile.RolesEnum::fromValue)
                        .toList()));
    }

    // ------------------------------------------------------------ bản dịch

    /**
     * {@code @PreAuthorize} chỉ trả lời "ai được vào cửa". Luật "vào rồi thì sửa
     * được bản nào" phụ thuộc locale và nằm ở tầng nghiệp vụ — xem
     * {@code AdminProductTranslationService}.
     */
    @Override
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN','CONSULTANT')")
    public ResponseEntity<List<AdminProductTranslation>> danhSachBanDich(UUID id) {
        return khongCache().body(banDich.danhSach(id).stream()
                .map(AdminController::sang)
                .toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminProductTranslation> luuBanDich(
            UUID id, String locale, AdminProductTranslationInput input) {

        ProductTranslationView daLuu = banDich.luu(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.vaiTro(),
                new ProductTranslationInput(
                        input.getSlug(), input.getTitle(), input.getShortDescription(),
                        input.getLongDescription(), input.getWhyChooseThis(),
                        input.getHeroImageAlt(), input.getStatus().getValue()));

        return khongCache().body(sang(daLuu));
    }

    // ------------------------------------------------------------ danh mục

    /**
     * Danh sách sản phẩm (docs/22 M2). Cả bốn vai trò đều <b>đọc</b> được — ma
     * trận quyền ở docs/22 mục 2.1 cho `CONSULTANT` và `TRANSLATOR` quyền R với
     * bản `da`. Quyền <b>ghi</b> mới phân theo vai trò và theo locale.
     */
    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminProductPage> danhSachSanPhamQuanTri(
            ProductType productType, String market, TranslationGap gap,
            String q, Integer page, Integer size) {

        PagedResult<AdminProductRow> ket_qua = danhMuc.danhSach(new AdminProductQuery(
                productType == null ? null : productType.getValue(),
                market, gap == null ? null : gap.getValue(), q, page, size));

        return khongCache().body(new AdminProductPage(
                ket_qua.items().stream().map(AdminController::sang).toList(),
                ket_qua.page(), ket_qua.size(), ket_qua.totalItems(), ket_qua.totalPages()));
    }

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<AdminDestination>> danhSachDiemDenQuanTri() {
        return khongCache().body(danhMuc.diemDen().stream()
                .map(AdminController::sang)
                .toList());
    }

    private static AdminDestination sang(DestinationOption d) {
        return new AdminDestination(d.id(), d.code(), d.name(), d.regionName());
    }

    // ------------------------------------------------------------ việc dịch

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<TranslationQueueItem>> hangDoiDich(
            TranslationEntityType entityType, Integer limit) {

        return khongCache().body(congViecDich
                .hangDoi(entityType == null ? null : entityType.getValue(), limit)
                .stream()
                .map(AdminController::sang)
                .toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<List<TranslationCoverageRow>> doPhuDich(String locale) {
        return khongCache().body(congViecDich.doPhu(locale).stream()
                .map(AdminController::sang)
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

    @Override
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminProductDetail> taoSanPham(AdminProductCreate input) {
        ProductDetailView daTao = sanPham.tao(new ProductCreateInput(
                input.getProductType().getValue(),
                input.getPrimaryDestinationId(),
                nho(input.getDurationDays()),
                input.getHeroImage(),
                input.getMapImage(),
                input.getIsNew(),
                input.getConsultantId(),
                sangBanDich(input.getSource()),
                khoi(input.getGroupTour(), input.getIndividualPackage(), input.getPrivateTour(),
                        input.getCruise(), input.getCombo(), input.getDayTour())));

        return khongCache(HttpStatus.CREATED).body(sang(daTao));
    }

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminProductDetail> chiTietSanPhamQuanTri(UUID id) {
        return khongCache().body(sang(sanPham.chiTiet(id)));
    }

    @Override
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminProductDetail> suaSanPham(UUID id, AdminProductPatch input) {
        ProductDetailView daSua = sanPham.sua(id, new ProductPatchInput(
                input.getPrimaryDestinationId(),
                nho(input.getDurationDays()),
                input.getHeroImage(),
                input.getMapImage(),
                input.getIsNew(),
                input.getConsultantId(),
                khoi(input.getGroupTour(), input.getIndividualPackage(), input.getPrivateTour(),
                        input.getCruise(), input.getCombo(), input.getDayTour())));

        return khongCache().body(sang(daSua));
    }

    /** Xoá mềm. Chỉ {@code ADMIN}: xoá nhầm một tour đang bán là sự cố không hoàn tác được. */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> xoaSanPham(UUID id) {
        sanPham.xoaMem(id);
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProductMarketState> ganThiTruong(
            UUID id, String market, AdminMarketAssignment input) {

        MarketState daGan = sanPham.ganThiTruong(id, market, input.getPublished());
        return khongCache().body(sang(daGan));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminPriceTier>> luuBacGia(
            UUID id, String market, List<AdminPriceTierInput> thang) {

        return khongCache().body(bacGia.luu(id, market, thang.stream()
                        .map(b -> new PriceTierInput(nho(b.getMinPax()), nho(b.getMaxPax()),
                                new BigDecimal(b.getPricePerPerson())))
                        .toList())
                .stream()
                .map(AdminController::sang)
                .toList());
    }

    // ------------------------------------------------------------ ngày khởi hành

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<List<AdminDeparture>> danhSachNgayKhoiHanhQuanTri(UUID id, String market) {
        return khongCache().body(ngayKhoiHanh.danhSach(id, market).stream()
                .map(AdminController::sang)
                .toList());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDeparture> taoNgayKhoiHanh(UUID id, AdminDepartureCreate input) {
        DepartureView daTao = ngayKhoiHanh.tao(id, new DepartureCreateInput(
                input.getMarket().getValue(),
                input.getDepartDate(),
                nho(input.getDays()),
                nho(input.getCapacity()),
                input.getCabinCategory() == null ? null : input.getCabinCategory().getValue(),
                input.getBaseStatus() == null ? null : input.getBaseStatus().getValue(),
                input.getDepartureOriginId()));

        return khongCache(HttpStatus.CREATED).body(sang(daTao));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDeparture> suaNgayKhoiHanh(UUID id, AdminDeparturePatch input) {
        DepartureView daSua = ngayKhoiHanh.sua(id, new DeparturePatchInput(
                input.getDepartDate(),
                nho(input.getDays()),
                nho(input.getCapacity()),
                input.getCabinCategory() == null ? null : input.getCabinCategory().getValue(),
                input.getBaseStatus() == null ? null : input.getBaseStatus().getValue(),
                input.getDepartureOriginId()));

        return khongCache().body(sang(daSua));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDepartureCopyResult> nhanBanLichKhoiHanh(
            UUID id, AdminDepartureCopy input) {

        CopyResult ket_qua = ngayKhoiHanh.nhanBan(id,
                input.getFromMarket().getValue(), input.getToMarket().getValue(),
                input.getFromDate());

        return khongCache().body(
                new AdminDepartureCopyResult(ket_qua.created(), ket_qua.skipped()));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminDeparturePrice>> luuGiaNgayKhoiHanh(
            UUID id, List<AdminDeparturePriceInput> gia) {

        return khongCache().body(ngayKhoiHanh.luuGia(id, gia.stream()
                        .map(g -> new DeparturePriceInput(g.getPaxTypeCode(),
                                g.getOccupancy().getValue(),
                                new BigDecimal(g.getAmount())))
                        .toList())
                .stream()
                .map(AdminController::sang)
                .toList());
    }

    // ------------------------------------------------------------ vận hành đơn
    //
    // Ma trận quyền docs/22 mục 2.1, dòng "Đơn đặt: xem": CONSULTANT R, ADMIN R.
    // EDITOR và TRANSLATOR là "–" — không thấy màn hình. Đó không phải sự thận
    // trọng thừa: đơn đặt mang email, điện thoại, ngày sinh và số hộ chiếu của
    // khách, còn người viết nội dung không có việc gì với dữ liệu đó (docs/31).

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminBookingPage> danhSachDon(
            AdminBookingScope scope, BookingStatus status, String market,
            LocalDate from, LocalDate to, String q, Integer page, Integer size) {

        PagedResult<AdminBookingRow> ket_qua = donDat.danhSach(new AdminBookingQuery(
                scope == null ? null : scope.getValue(),
                status == null ? null : status.getValue(),
                market, from, to, q, page, size));

        return khongCache().body(new AdminBookingPage(
                ket_qua.items().stream().map(AdminController::sang).toList(),
                ket_qua.page(), ket_qua.size(), ket_qua.totalItems(), ket_qua.totalPages()));
    }

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminBookingDetail> chiTietDon(String reference) {
        return khongCache().body(sang(donDat.chiTiet(reference)));
    }

    /**
     * Đường <b>ghi</b> của M7 — dòng "Đơn đặt: đổi trạng thái, huỷ, hoàn" của ma
     * trận docs/22 mục 2.1.
     *
     * <p>Cùng vai trò với đường đọc ở v1, nhưng khai riêng chứ không dùng chung
     * một hằng: hai dòng khác nhau trong ma trận thì là hai câu hỏi khác nhau,
     * và ngày nào đó chúng sẽ trả lời khác nhau.
     *
     * <p>{@code AdminBookingTargetStatus} hẹp hơn {@code BookingStatus} nên phép
     * chuyển qua enum của domain luôn thành công — bốn giá trị của nó là tập con
     * đúng nghĩa, và trình biên dịch giữ cho nó đúng vì cả hai đều sinh từ spec.
     */
    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminBookingDetail> doiTrangThaiDon(
            String reference, AdminBookingStatusChange input) {

        return khongCache().body(sang(donDat.doiTrangThai(
                reference,
                vn.travel.booking.booking.dto.BookingStatus.valueOf(input.getToStatus().getValue()),
                SecurityUtils.nhanVienHienTai().id(),
                input.getNote())));
    }

    // ------------------------------------------------------------ ánh xạ đơn

    private static AdminBookingSummary sang(AdminBookingRow d) {
        return new AdminBookingSummary(
                d.id(), d.reference(),
                BookingStatus.fromValue(d.status().name()),
                AdminBookingSummary.MarketEnum.fromValue(d.market()),
                d.locale(), d.productTitle(), d.paxCount(),
                RefMapper.sangTien(d.total()), d.contactEmail(), d.createdAt())
                .departDate(d.departDate());
    }

    private static AdminBookingDetail sang(AdminBookingDetailView d) {
        return new AdminBookingDetail(
                d.id(), d.reference(),
                BookingStatus.fromValue(d.status().name()),
                AdminBookingDetail.MarketEnum.fromValue(d.market()),
                d.locale(), d.productId(), d.productTitle(),
                d.contactEmail(), d.contactPhone(), d.createdAt(),
                PricingMapper.sangBang(d.breakdown()),
                d.passengers().stream().map(AdminController::sang).toList(),
                d.events().stream().map(AdminController::sang).toList())
                .departureId(d.departureId())
                .departDate(d.departDate());
    }

    private static AdminBookingPassenger sang(BookingPassengerRow h) {
        return new AdminBookingPassenger(h.seq(), h.paxTypeCode(), h.fullName())
                .dateOfBirth(h.dateOfBirth())
                .passportNo(h.passportNo())
                .passportExpiry(h.passportExpiry())
                .nationality(h.nationality());
    }

    private static AdminBookingEvent sang(BookingEventRow e) {
        return new AdminBookingEvent(
                e.id(),
                BookingStatus.fromValue(e.toStatus().name()),
                AdminBookingEvent.ActorTypeEnum.fromValue(e.actorType()),
                e.createdAt())
                .fromStatus(e.fromStatus() == null ? null
                        : BookingStatus.fromValue(e.fromStatus().name()))
                .actorId(e.actorId())
                .actorName(e.actorName())
                .note(e.note());
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
    private static Short nho(Integer so) {
        return so == null ? null : so.shortValue();
    }

    private static Integer lon(Short so) {
        return so == null ? null : so.intValue();
    }

    private static ProductTypeBlocks khoi(GroupTourFields g, IndividualPackageFields i,
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

    private static AdminProductDetail sang(ProductDetailView v) {
        AdminProductDetail ra = new AdminProductDetail(
                v.id(), ProductType.fromValue(v.productType()), v.primaryDestinationId(),
                v.heroImage(), v.isNew(), v.reviewCount(),
                v.markets().stream().map(AdminController::sang).toList(),
                v.translations().stream()
                        .map(t -> new AdminTranslationState(t.locale(),
                                TranslationStatus.fromValue(t.status()), t.isSource(), t.outdated()))
                        .toList())
                .durationDays(lon(v.durationDays()))
                .mapImage(v.mapImage())
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

    private static AdminProductMarketState sang(MarketState m) {
        return new AdminProductMarketState(
                AdminProductMarketState.MarketEnum.fromValue(m.market()), m.published());
    }

    private static AdminDeparture sang(DepartureView d) {
        return new AdminDeparture(d.id(),
                AdminDeparture.MarketEnum.fromValue(d.market()),
                d.departDate(), d.returnDate(), lon(d.days()), d.baseStatus(),
                lon(d.capacity()), lon(d.seatsBooked()),
                d.prices().stream().map(AdminController::sang).toList())
                .cabinCategory(d.cabinCategory())
                .departureOriginId(d.departureOriginId());
    }

    private static AdminDeparturePrice sang(DeparturePriceView g) {
        return new AdminDeparturePrice(g.paxTypeCode(), g.occupancy(), tien(g.amount()));
    }

    private static AdminPriceTier sang(PriceTierView t) {
        return new AdminPriceTier(t.id(), AdminPriceTier.MarketEnum.fromValue(t.market()),
                lon(t.minPax()), tien(t.pricePerPerson()))
                .maxPax(lon(t.maxPax()));
    }

    /** {@code amount} là <b>chuỗi</b> trong JSON — số dấu phẩy động của JavaScript làm hỏng tiền. */
    private static Money tien(vn.travel.booking.common.money.Money m) {
        return new Money(m.amount().toPlainString(), m.currency());
    }

    // ------------------------------------------------------------ ánh xạ

    private static AdminProductSummary sang(AdminProductRow r) {
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

    private static TranslationQueueItem sang(QueueItem q) {
        return new TranslationQueueItem(
                TranslationEntityType.fromValue(q.entityType()), q.id(), q.locale(),
                TranslationGap.fromValue(q.gap()), q.priority(), q.sourceTitle(),
                q.sourceLastModifiedAt())
                .translatedAt(q.translatedAt());
    }

    private static TranslationCoverageRow sang(CoverageRow c) {
        return new TranslationCoverageRow(
                TranslationEntityType.fromValue(c.entityType()), c.locale(),
                c.total(), c.translated(), c.upToDate());
    }

    private static AdminProductTranslation sang(ProductTranslationView v) {
        return new AdminProductTranslation(
                v.locale(), v.slug(), v.title(), v.shortDescription(),
                v.longDescription(), v.whyChooseThis(), v.heroImageAlt(),
                TranslationStatus.fromValue(v.status()), v.isSource(), v.lastModifiedAt())
                .outdated(v.outdated())
                .lastModifiedBy(v.lastModifiedBy());
    }


    // ------------------------------------------------------------ báo giá
    //
    // Ma trận quyền docs/22 mục 2.1, dòng "Báo giá: dựng, gửi": CONSULTANT W,
    // ADMIN W. EDITOR và TRANSLATOR là "–" — báo giá mang tên, điện thoại và
    // yêu cầu riêng của khách, và người viết nội dung không có việc gì với dữ
    // liệu đó (docs/31), đúng như với đơn đặt.
    //
    // Cả BỐN endpoint cùng một vai trò, kể cả đường đọc: khác đơn đặt ở chỗ ma
    // trận không tách "xem báo giá" khỏi "dựng báo giá" thành hai dòng, nên ở
    // đây không có hai câu hỏi để trả lời khác nhau.

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminQuotePage> danhSachBaoGia(
            AdminQuoteFilter status, String market, String q, Integer page, Integer size) {

        PagedResult<AdminQuoteRow> ket_qua = baoGia.danhSach(new AdminQuoteQuery(
                status == null ? null : status.getValue(), market, q, page, size));

        return khongCache().body(new AdminQuotePage(
                ket_qua.items().stream().map(AdminController::sang).toList(),
                ket_qua.page(), ket_qua.size(), ket_qua.totalItems(), ket_qua.totalPages()));
    }

    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminQuoteDetail> chiTietBaoGia(String reference) {
        return khongCache().body(sang(baoGia.chiTiet(reference)));
    }

    /**
     * Dựng bảng giá — docs/14 mục 7 quy tắc 5 và 6.
     *
     * <p>{@code total} <b>không có trong thân yêu cầu</b> và đó là chủ ý: máy
     * chủ cộng từ các dòng. Nhận tổng rồi tin là mở đường cho một báo giá mà
     * tổng không khớp bảng, và đó đúng là thứ khách mang ra tranh cãi.
     */
    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminQuoteDetail> dungBangGiaBaoGia(
            String reference, AdminQuoteLinesInput input) {

        return khongCache().body(sang(baoGia.datBangGia(
                reference,
                input.getCurrency(),
                input.getLines().stream()
                        .map(d -> new QuoteLineDraft(
                                d.getLabelKey(),
                                d.getQuantity() == null ? null : new BigDecimal(d.getQuantity()),
                                d.getUnitAmount() == null ? null : new BigDecimal(d.getUnitAmount()),
                                new BigDecimal(d.getAmount())))
                        .toList(),
                SecurityUtils.nhanVienHienTai().id())));
    }

    /**
     * Gửi báo giá, hoặc ghi nhận khách đã trả lời — máy trạng thái docs/14 mục 7.
     *
     * <p>{@code AdminQuoteTargetStatus} hẹp hơn {@code QuoteStatus} (ba giá trị
     * thay vì năm), nên phép chuyển sang enum của domain luôn thành công.
     * {@code DRAFT} không nằm trong đó vì nó là điểm xuất phát; {@code EXPIRED}
     * không nằm trong đó vì nó là kết luận của đồng hồ, không phải quyết định
     * của người.
     */
    @Override
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminQuoteDetail> doiTrangThaiBaoGia(
            String reference, AdminQuoteStatusChange input) {

        return khongCache().body(sang(baoGia.doiTrangThai(
                reference,
                vn.travel.booking.quote.dto.QuoteStatus.valueOf(input.getToStatus().getValue()),
                SecurityUtils.nhanVienHienTai().id())));
    }

    // ------------------------------------------------------- ánh xạ báo giá

    private static AdminQuoteSummary sang(AdminQuoteRow r) {
        return new AdminQuoteSummary(
                r.id(), r.reference(),
                QuoteStatus.fromValue(r.status().name()),
                AdminQuoteSummary.MarketEnum.fromValue(r.market()),
                r.locale(), r.productId(), r.productTitle(), r.partySize(),
                r.contactName(), r.contactEmail(), r.createdAt())
                .requestedDate(r.requestedDate())
                .total(RefMapper.sangTien(r.total()))
                .validUntil(r.validUntil());
    }

    /**
     * Chi tiết = tóm tắt + bốn trường.
     *
     * <p>Lớp sinh ra làm phẳng {@code allOf} thành một lớp duy nhất, nên phải
     * chép mười một trường của tóm tắt sang lần nữa ở đây. Không gọi lại được
     * {@code sang(AdminQuoteRow)} vì hai kiểu sinh ra không có quan hệ kế thừa —
     * đó là cái giá của {@code allOf} trong bộ sinh mã, và nó rẻ hơn việc lồng
     * một đối tượng {@code summary} vào giữa phản hồi.
     */
    private static AdminQuoteDetail sang(AdminQuoteDetailView d) {
        AdminQuoteRow r = d.tomTat();

        return new AdminQuoteDetail(
                r.id(), r.reference(),
                QuoteStatus.fromValue(r.status().name()),
                AdminQuoteDetail.MarketEnum.fromValue(r.market()),
                r.locale(), r.productId(), r.productTitle(), r.partySize(),
                r.contactName(), r.contactEmail(), r.createdAt(),
                d.contactPhone(),
                d.lines().stream().map(AdminController::sang).toList())
                .requestedDate(r.requestedDate())
                .total(RefMapper.sangTien(r.total()))
                .validUntil(r.validUntil())
                .message(d.message())
                .leadTimeDays(d.leadTimeDays())
                .quoteValidDays(d.quoteValidDays());
    }

    private static QuoteLine sang(QuoteLineRow l) {
        return new QuoteLine(l.seq(), l.labelKey(), RefMapper.sangTien(l.amount()))
                .quantity(l.quantity() == null ? null : l.quantity().toPlainString())
                .unitAmount(RefMapper.sangTien(l.unitAmount()));
    }

    private static ServletRequestAttributes servlet() {
        return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    }

    private static ResponseEntity.BodyBuilder khongCache() {
        return khongCache(HttpStatus.OK);
    }

    private static ResponseEntity.BodyBuilder khongCache(HttpStatus trangThai) {
        return ResponseEntity.status(trangThai)
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }
}
