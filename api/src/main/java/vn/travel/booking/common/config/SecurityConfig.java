package vn.travel.booking.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Bề mặt công khai mở, bề mặt quản trị đóng.
 *
 * <p>Danh sách đường dẫn công khai khai báo <b>tập trung ở đây</b>: thêm một
 * endpoint đọc mới thì phải nhớ khai vào đây, và quên thì nó bị chặn — hỏng
 * theo hướng an toàn, không phải hỏng theo hướng rò rỉ.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrf = new CsrfTokenRequestAttributeHandler();
        // Tắt nạp trễ: mặc định Spring chỉ sinh thẻ CSRF khi có ai đó đọc tới nó,
        // nên phản hồi đầu tiên không kèm cookie XSRF-TOKEN và client không có gì
        // để gửi ở lời gọi ghi tiếp theo.
        csrf.setCsrfRequestAttributeName(null);

        return http
                // Bề mặt công khai chỉ đọc và không dùng cookie phiên, nên CSRF
                // không áp dụng cho nó. Bề mặt quản trị thì có: nó ghi, và nó
                // xác thực bằng cookie — đúng điều kiện để một trang khác lừa
                // trình duyệt gửi yêu cầu thay người dùng.
                .csrf(c -> c
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrf)
                        .ignoringRequestMatchers(
                                // Bề mặt công khai: không dùng cookie phiên, nên
                                // CSRF không áp dụng. CSRF chống việc một trang
                                // khác lừa TRÌNH DUYỆT gửi kèm cookie của nạn
                                // nhân; ở đây không có cookie nào để lừa gửi.
                                //
                                // {market:[a-z]{2}} chứ KHÔNG phải `*`, và đây là
                                // chỗ đã có lỗ hổng thật: `*` khớp MỌI đoạn đường
                                // dẫn, kể cả `admin`. Nên "/api/v1/*/products/**"
                                // miễn CSRF luôn cho /api/v1/admin/products/** —
                                // tức là tạo sản phẩm, sửa, xoá, gán thị trường,
                                // lưu bản dịch và bậc giá đều ghi được chỉ bằng
                                // cookie. Mã thị trường dài đúng hai ký tự
                                // (`market.code` là VARCHAR(2)), nên ràng buộc độ
                                // dài loại `admin` ra một cách chính xác.
                                "/api/v1/{market:[a-z]{2}}/products/**",
                                "/api/v1/{market:[a-z]{2}}/regions",
                                "/api/v1/{market:[a-z]{2}}/destinations/**",
                                "/api/v1/{market:[a-z]{2}}/themes",
                                "/api/v1/{market:[a-z]{2}}/posts/**",
                                "/api/v1/{market:[a-z]{2}}/lectures",
                                // Đường GHI của khách cũng vậy: khách không đăng
                                // nhập ở v1, và chống gọi lại là việc của
                                // Idempotency-Key chứ không phải của CSRF.
                                "/api/v1/{market:[a-z]{2}}/pricing/**",
                                "/api/v1/{market:[a-z]{2}}/seat-holds/**",
                                "/api/v1/{market:[a-z]{2}}/bookings/**",
                                "/api/v1/{market:[a-z]{2}}/quote-requests",
                                // Chính lời gọi đăng nhập: lúc đó chưa có phiên
                                // nên chưa có thẻ nào để gửi, và đòi thẻ ở đây tạo
                                // ra một bước lấy thẻ mà spec không có. Rủi ro còn
                                // lại là "login CSRF" — kẻ tấn công lừa nạn nhân
                                // đăng nhập vào tài khoản CỦA HẮN, hạng nhẹ hơn
                                // hẳn, và SameSite=Lax đã chặn phần lớn. Mọi lời
                                // gọi GHI khác của quản trị vẫn phải có thẻ.
                                "/api/v1/admin/session"))

                .authorizeHttpRequests(a -> a
                        // Đăng nhập phải mở, nếu không thì không ai vào được.
                        .requestMatchers("/api/v1/admin/session").permitAll()
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        // Mọi thứ còn lại của API v1 là bề mặt công khai chỉ đọc.
                        .requestMatchers("/api/v1/**").permitAll()
                        .anyRequest().denyAll())

                // Phiên tạo khi đăng nhập, không tạo sẵn cho khách vãng lai —
                // mỗi lượt đọc công khai mà sinh một phiên là một rò rỉ bộ nhớ chậm.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // Chưa đăng nhập thì 401, không chuyển hướng tới trang đăng nhập:
                // đây là API, không phải trang web có form.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // Không form login, không HTTP Basic, không logout mặc định:
                // ba thứ đó sinh ra đường dẫn và hành vi không có trong spec.
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .build();
    }

    /**
     * BCrypt, không phải SHA. Mật khẩu băm bằng hàm băm nhanh là mật khẩu bị dò
     * xong trong vài giờ khi cơ sở dữ liệu rò ra ngoài.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cauHinh) throws Exception {
        return cauHinh.getAuthenticationManager();
    }
}
