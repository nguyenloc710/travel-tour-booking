package vn.travel.booking.it;

import org.springframework.core.ParameterizedTypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Swagger và spec springdoc sinh ra từ annotation trên controller.
 *
 * <p><b>Vì sao lớp test này tồn tại.</b> Controller thôi {@code implements}
 * interface sinh từ {@code contracts/openapi.yaml}, nên trình biên dịch không
 * còn bắt được việc code và spec lệch nhau. Bài
 * {@link #springdocPathsMatchContract()} thế chỗ cho phép kiểm đã mất: nó so tập
 * đường dẫn springdoc đọc được từ code với tập đường dẫn trong file spec.
 *
 * <p>Nó <b>không</b> mạnh bằng lỗi biên dịch — nó không so tham số, kiểu dữ liệu
 * hay mã trạng thái, chỉ so đường dẫn. Thêm một endpoint mà quên khai trong
 * {@code contracts/openapi.yaml} sẽ đỏ ở đây; đổi kiểu một tham số thì không.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SwaggerIT {

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
    int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private static final ParameterizedTypeReference<Map<String, Object>> JSON =
            new ParameterizedTypeReference<>() { };

    private Map<String, Object> apiDocs() {
        ResponseEntity<Map<String, Object>> r = client().get()
                .uri("/v3/api-docs").retrieve().toEntity(JSON);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        return r.getBody();
    }

    @Test
    @DisplayName("/v3/api-docs mở được mà KHÔNG cần đăng nhập, và là OpenAPI 3")
    void apiDocsIsPublic() {
        Map<String, Object> body = apiDocs();

        assertTrue(String.valueOf(body.get("openapi")).startsWith("3."),
                "phải là spec OpenAPI 3");

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) body.get("info");
        assertEquals("Travel Tour Booking API", info.get("title"),
                "tiêu đề lấy từ OpenApiConfig");
    }

    @Test
    @DisplayName("Trang Swagger UI phục vụ được")
    void swaggerUiIsServed() {
        ResponseEntity<String> response = client().get()
                .uri("/swagger-ui/index.html").retrieve().toEntity(String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("swagger"),
                "phải trả trang Swagger UI, không phải trang trắng");
    }

    @Test
    @DisplayName("Tập đường dẫn springdoc đọc từ code KHỚP contracts/openapi.yaml")
    void springdocPathsMatchContract() throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) apiDocs().get("paths");
        Set<String> fromCode = new TreeSet<>(paths.keySet());

        Set<String> fromContract = contractPaths();

        assertFalse(fromContract.isEmpty(), "không đọc được đường dẫn nào từ file spec");

        Set<String> onlyInCode = new TreeSet<>(fromCode);
        onlyInCode.removeAll(fromContract);
        Set<String> onlyInContract = new TreeSet<>(fromContract);
        onlyInContract.removeAll(fromCode);

        assertEquals(Set.of(), onlyInCode,
                "endpoint có trong code nhưng THIẾU trong contracts/openapi.yaml — "
                        + "spec vẫn là thứ frontend sinh client từ đó");
        assertEquals(Set.of(), onlyInContract,
                "endpoint khai trong contracts/openapi.yaml nhưng code KHÔNG phục vụ");
    }

    /** Đọc khoá đường dẫn ở mục {@code paths:} của file spec — thụt đúng hai dấu cách. */
    private static Set<String> contractPaths() throws IOException {
        Path spec = find("contracts/openapi.yaml");
        Pattern p = Pattern.compile("^ {2}(/[^:\\s]*):\\s*$");
        Set<String> paths = new TreeSet<>();
        for (String line : Files.readAllLines(spec)) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                paths.add(m.group(1));
            }
        }
        return paths;
    }

    private static Path find(String path) {
        for (Path origin : List.of(Path.of(""), Path.of("api"), Path.of(".."))) {
            Path candidate = origin.resolve(path);
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("không tìm thấy " + path
                + " từ " + Path.of("").toAbsolutePath());
    }
}
