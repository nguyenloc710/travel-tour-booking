package vn.travel.booking.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.auth.dto.StaffPrincipal;
import vn.travel.booking.auth.mapper.AdminUserMapper;
import vn.travel.booking.common.util.SecurityUtils;
import vn.travel.booking.web.generated.model.LoginRequest;
import vn.travel.booking.web.generated.model.StaffProfile;

import static vn.travel.booking.common.util.AdminResponses.noCache;
import static vn.travel.booking.common.util.AdminResponses.servlet;

/**
 * Phiên đăng nhập của nhân viên và hồ sơ người đang đăng nhập — {@code docs/22}
 * mục 9.
 *
 * <p>Tách khỏi phần còn lại của bề mặt quản trị vì đây là thứ <b>duy nhất</b>
 * chạm vào {@code HttpSession} và {@code SecurityContext}. Gộp nó chung với CRUD
 * sản phẩm hay đơn đặt là để một lỗi ở luồng xác thực nằm cạnh mã không liên
 * quan gì tới xác thực.
 */
@RestController
@Validated
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AdminUserMapper mapper;

    /**
     * Ghi {@code SecurityContext} vào phiên bằng tay.
     *
     * <p>Bộ lọc của Spring Security chỉ tự ghi cho request đi qua chuỗi lọc xác
     * thực; lời gọi đăng nhập này tự xác thực nên phải tự ghi, nếu không thì
     * phiên trống và request kế tiếp lại 401.
     */
    private final SecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, AdminUserMapper mapper) {
        this.authenticationManager = authenticationManager;
        this.mapper = mapper;
    }

    /**
     * Đăng nhập. Phản hồi <b>không mang token</b>: phiên nằm trong cookie
     * {@code HttpOnly}, thứ mã chèn vào trang không đọc được (docs/22 mục 9).
     */
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/api/v1/admin/session",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        HttpServletRequest request = servlet().getRequest();
        HttpServletResponse response = servlet().getResponse();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(), loginRequest.getPassword()));
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            // Một câu trả lời cho cả sai email lẫn sai mật khẩu — phân biệt hai
            // cái là cho phép dò xem địa chỉ nào có trong hệ thống.
            return ResponseEntity.status(401).build();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Chống cố định phiên: đổi id phiên đang có, hoặc tạo mới nếu chưa có.
        // Gọi changeSessionId() khi chưa có phiên nào thì Tomcat ném
        // IllegalStateException — và lời gọi đăng nhập đầu tiên chính là lúc đó.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        } else {
            request.getSession(true);
        }
        contextRepository.saveContext(context, request, response);

        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @RequestMapping(
            method = RequestMethod.DELETE,
            value = "/api/v1/admin/session"
    )
    public ResponseEntity<Void> logout() {
        HttpSession session = servlet().getRequest().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api/v1/admin/me",
            produces = {"application/json"}
    )
    public ResponseEntity<StaffProfile> getStaffProfile() {
        StaffPrincipal staff = SecurityUtils.currentStaff();
        return noCache().body(mapper.toProfile(staff));
    }
}
