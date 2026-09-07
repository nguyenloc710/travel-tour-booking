package vn.travel.booking.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    private final List<String> gocChoPhep;

    public SecurityConfig(
            @Value("${travel.cors.allowed-origins}") List<String> gocChoPhep) {
        this.gocChoPhep = gocChoPhep;
    }

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrf = new CsrfTokenRequestAttributeHandler();
        // Tắt nạp trễ: mặc định Spring chỉ sinh thẻ CSRF khi có ai đó đọc tới nó,
        // nên phản hồi đầu tiên không kèm cookie XSRF-TOKEN và client không có gì
        // để gửi ở lời gọi ghi tiếp theo.
        csrf.setCsrfRequestAttributeName(null);

        return http
                // CORS phải bật TRƯỚC mọi thứ khác: thiếu nó thì trình duyệt
                // chặn ngay ở bước preflight và không lời gọi ghi nào tới được
                // controller — xem chú thích ở bean corsConfigurationSource.
                .cors(c -> c.configurationSource(corsConfigurationSource()))

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
     * CORS — danh sách gốc được phép gọi API từ trình duyệt.
     *
     * <p><b>Vì sao cần:</b> site khách chạy ở cổng 3000 và trang quản trị ở
     * 3001, còn API ở 8080. Với trình duyệt đó là ba <i>gốc</i> khác nhau, nên
     * mọi lời gọi từ mã chạy trong trình duyệt là lời gọi chéo gốc. Không khai
     * CORS thì trình duyệt chặn ngay ở bước preflight: `fetch` ném lỗi mạng
     * trần, không có thân phản hồi, nên frontend <b>không có mã lỗi nào để
     * dịch</b> và khách chỉ thấy một câu lỗi chung.
     *
     * <p><b>Vì sao lọt tới tận bây giờ:</b> đường ĐỌC của site khách gọi từ máy
     * chủ Next chứ không từ trình duyệt, nên nó không đụng CORS; test tích hợp
     * cũng chạy phía máy chủ. Đường GHI — giữ chỗ, tạo đơn, gửi yêu cầu báo giá,
     * và toàn bộ trang quản trị — là thứ duy nhất chạy trong trình duyệt, và nó
     * chưa bao giờ được mở bằng trình duyệt thật cho tới hôm nay.
     *
     * <p><b>Gốc khai tường minh, không dùng `*`.</b> Trang quản trị xác thực
     * bằng cookie phiên, nên nó cần {@code allowCredentials}; mà đặc tả CORS
     * cấm dùng {@code *} cùng với credentials — và điều đó là đúng: một API cho
     * gửi kèm cookie từ bất kỳ trang nào là một API bất kỳ trang nào cũng thao
     * tác được thay người dùng đang đăng nhập.
     *
     * <p>Danh sách lấy từ cấu hình để production khai tên miền thật mà không
     * phải sửa mã. Mặc định là hai cổng dev.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(gocChoPhep);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Ba header của riêng dự án này, ngoài các header đơn giản: Idempotency-Key
        // cho đường ghi công khai, X-XSRF-TOKEN cho bề mặt quản trị, và
        // Accept-Language vốn là header đơn giản nhưng khai lại cho rõ ý.
        c.setAllowedHeaders(List.of(
                "Content-Type", "Accept", "Accept-Language",
                "Idempotency-Key", "X-XSRF-TOKEN"));
        c.setAllowCredentials(true);
        // Kết quả preflight cache một giờ: không có nó thì MỖI lời gọi ghi tốn
        // hai vòng mạng.
        c.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource nguon = new UrlBasedCorsConfigurationSource();
        nguon.registerCorsConfiguration("/api/**", c);
        return nguon;
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
