package vn.travel.booking.admin.controller;

import vn.travel.booking.auth.dto.StaffPrincipal;
import vn.travel.booking.common.util.AcceptLanguages;
import vn.travel.booking.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
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
import vn.travel.booking.admin.service.AdminCatalogService;
import vn.travel.booking.admin.service.AdminProductTranslationService;
import vn.travel.booking.admin.service.TranslationWorkService;
import vn.travel.booking.admin.dto.AdminProductQuery;
import vn.travel.booking.admin.dto.AdminProductRow;
import vn.travel.booking.admin.dto.CoverageRow;
import vn.travel.booking.admin.dto.ProductTranslationInput;
import vn.travel.booking.admin.dto.ProductTranslationView;
import vn.travel.booking.admin.dto.QueueItem;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.web.generated.api.AdminApi;
import vn.travel.booking.web.generated.model.AdminProductMarketState;
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
    private final SecurityContextRepository khoPhien = new HttpSessionSecurityContextRepository();

    public AdminController(AuthenticationManager xacThuc,
                           AdminProductTranslationService banDich,
                           AdminCatalogService danhMuc,
                           TranslationWorkService congViecDich) {
        this.xacThuc = xacThuc;
        this.banDich = banDich;
        this.danhMuc = danhMuc;
        this.congViecDich = congViecDich;
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

    private static ServletRequestAttributes servlet() {
        return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    }

    private static ResponseEntity.BodyBuilder khongCache() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }
}
