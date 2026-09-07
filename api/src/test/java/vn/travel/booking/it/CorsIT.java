package vn.travel.booking.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CORS — bề mặt duy nhất mà mọi test khác của dự án này đi vòng qua.
 *
 * <p><b>Vì sao tồn tại:</b> API không có CORS suốt từ đầu, và không một test nào
 * bắt được. Test tích hợp gọi từ phía máy chủ nên không có khái niệm "gốc"; site
 * khách gọi đường ĐỌC cũng từ máy chủ Next. Chỉ đường GHI — giữ chỗ, đặt tour,
 * yêu cầu báo giá, và toàn bộ trang quản trị — mới chạy trong trình duyệt, và nó
 * chưa bao giờ được mở bằng trình duyệt thật cho tới khi có người thử đặt tour.
 *
 * <p>Hậu quả lúc đó: trình duyệt chặn ngay ở preflight, {@code fetch} ném lỗi
 * mạng trần không có thân phản hồi, nên frontend <b>không có mã lỗi nào để
 * dịch</b> và khách chỉ thấy một câu lỗi chung. Không log nào ở backend, vì yêu
 * cầu chưa từng tới controller.
 *
 * <p>Ba test dưới đây gửi header {@code Origin} — thứ duy nhất phân biệt một lời
 * gọi từ trình duyệt với một lời gọi từ máy chủ.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CorsIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("travel")
                    .withUsername("travel")
                    .withPassword("travel");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    int cong;

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + cong).build();
    }

    @Test
    @DisplayName("preflight của site khách được chấp nhận, kèm đủ header của đường ghi")
    void preflightSiteKhach() {
        ResponseHeaders kq = preflight("http://localhost:3000", "POST",
                "content-type,idempotency-key,accept-language");

        assertEquals(HttpStatus.OK, kq.trangThai);
        assertEquals("http://localhost:3000", kq.headers.getFirst("Access-Control-Allow-Origin"));

        // Idempotency-Key là header của RIÊNG dự án này, nên trình duyệt chỉ gửi
        // nó sau khi preflight nói rõ là được. Thiếu nó thì đặt tour chết mà
        // không ai biết vì sao.
        String choPhep = kq.headers.getFirst("Access-Control-Allow-Headers");
        assertTrue(choPhep != null && choPhep.toLowerCase().contains("idempotency-key"),
                "phải cho phép Idempotency-Key, đang là: " + choPhep);

        String phuongThuc = kq.headers.getFirst("Access-Control-Allow-Methods");
        assertTrue(phuongThuc != null && phuongThuc.contains("POST"),
                "phải cho phép POST, đang là: " + phuongThuc);
    }

    @Test
    @DisplayName("trang quản trị được gửi kèm cookie phiên")
    void preflightTrangQuanTri() {
        ResponseHeaders kq = preflight("http://localhost:3001", "PUT", "content-type,x-xsrf-token");

        assertEquals(HttpStatus.OK, kq.trangThai);
        assertEquals("http://localhost:3001", kq.headers.getFirst("Access-Control-Allow-Origin"));

        // Trang quản trị xác thực bằng cookie phiên, nên nếu thiếu dòng này thì
        // trình duyệt gửi yêu cầu KHÔNG kèm cookie và mọi lời gọi trả 401.
        assertEquals("true", kq.headers.getFirst("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("gốc lạ bị từ chối — danh sách là danh sách cho phép, không phải trang trí")
    void gocLaBiTuChoi() {
        ResponseHeaders kq = preflight("https://ke-tan-cong.example", "POST", "content-type");

        // Spring trả 403 cho preflight của gốc không nằm trong danh sách. Điều
        // quan trọng hơn mã trạng thái: KHÔNG có Allow-Origin, nên dù mã có là
        // gì thì trình duyệt cũng chặn.
        assertNull(kq.headers.getFirst("Access-Control-Allow-Origin"));
        assertEquals(HttpStatus.FORBIDDEN, kq.trangThai);
    }

    private ResponseHeaders preflight(String goc, String phuongThuc, String header) {
        var kq = client()
                .method(HttpMethod.OPTIONS)
                .uri("/api/v1/dk/seat-holds")
                .header(HttpHeaders.ORIGIN, goc)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, phuongThuc)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, header)
                .retrieve()
                .onStatus(trangThai -> true, (yc, pt) -> { })
                .toBodilessEntity();
        return new ResponseHeaders(HttpStatus.valueOf(kq.getStatusCode().value()), kq.getHeaders());
    }

    private record ResponseHeaders(HttpStatus trangThai, HttpHeaders headers) { }
}
