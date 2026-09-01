package vn.travel.booking.web;

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
import vn.travel.booking.application.admin.AdminProductTranslationUseCases;
import vn.travel.booking.application.admin.ProductTranslationInput;
import vn.travel.booking.application.admin.ProductTranslationView;
import vn.travel.booking.web.generated.api.AdminApi;
import vn.travel.booking.web.generated.model.AdminProductTranslation;
import vn.travel.booking.web.generated.model.AdminProductTranslationInput;
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
    private final AdminProductTranslationUseCases banDich;
    private final SecurityContextRepository khoPhien = new HttpSessionSecurityContextRepository();

    public AdminController(AuthenticationManager xacThuc, AdminProductTranslationUseCases banDich) {
        this.xacThuc = xacThuc;
        this.banDich = banDich;
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
     * {@code AdminProductTranslationUseCases}.
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
