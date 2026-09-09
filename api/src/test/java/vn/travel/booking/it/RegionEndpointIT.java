package vn.travel.booking.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.client.RestClient;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.Region;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Postgres THẬT qua Testcontainers, không H2.
 *
 * <p>H2 không mô phỏng được ICU collation và {@code unaccent} — mà đó chính là
 * hai thứ dễ sai nhất của dự án này (docs/10 mục 9).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RegionEndpointIT {

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

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Ba miền, nhưng chỉ HAI miền có bản dịch {@code vi}. Miền thứ ba chỉ có
     * {@code da} — đây là trường hợp rìa mà mọi truy vấn nội dung phải xử lý
     * đúng, và nếu dữ liệu thử không chứa sẵn thì không ai gặp nó cho tới khi
     * lên production.
     */
    @BeforeEach
    void prepareData() {
        // Trả trạng thái thị trường về đúng dữ liệu tra cứu: DK bật, VN tắt.
        // Thiếu dòng này thì một test bật VN lên sẽ làm test sau đó xanh nhầm.
        jdbc.update("UPDATE market SET is_active = (code = 'DK')");

        jdbc.update("DELETE FROM region_translation");
        jdbc.update("DELETE FROM region");

        jdbc.update("INSERT INTO region (id, code, sort_order) VALUES "
                + "('d0000000-0000-4000-8000-000000000001','NORTH',1),"
                + "('d0000000-0000-4000-8000-000000000002','CENTRAL',2),"
                + "('d0000000-0000-4000-8000-000000000003','SOUTH',3)");

        jdbc.update("INSERT INTO region_translation (region_id, locale, slug, name) VALUES "
                + "('d0000000-0000-4000-8000-000000000001','da','nordvietnam','Nordvietnam'),"
                + "('d0000000-0000-4000-8000-000000000001','vi','mien-bac','Miền Bắc'),"
                + "('d0000000-0000-4000-8000-000000000002','da','centralvietnam','Det centrale Vietnam'),"
                + "('d0000000-0000-4000-8000-000000000002','vi','mien-trung','Miền Trung'),"
                + "('d0000000-0000-4000-8000-000000000003','da','sydvietnam','Sydvietnam')");
    }

    @Test
    @DisplayName("Migration V1 chạy được trên Postgres thật")
    void migrationRunsOnRealPostgres() {
        Integer tableCount = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'",
                Integer.class);
        assertNotNull(tableCount);
        assertTrue(tableCount > 20, "Lược đồ của docs/12 phải dựng xong, đang có " + tableCount + " bảng");
    }

    @Test
    @DisplayName("f_unaccent index được và bỏ dấu đúng cho cả tiếng Việt lẫn tiếng Đan")
    void fUnaccentIndexesAndStripsAccents() {
        assertEquals("Hoi An", jdbc.queryForObject(
                "SELECT f_unaccent('Hội An')", String.class));
        assertEquals("Halong-bugten", jdbc.queryForObject(
                "SELECT f_unaccent('Halong-bugten')", String.class));
    }

    @Test
    @DisplayName("Collation Đan Mạch xếp æ ø å sau z, không theo thứ tự Unicode")
    void danishCollationOrdersSpecialLettersAfterZ() {
        List<String> danishOrder = jdbc.queryForList(
                "SELECT v FROM (VALUES ('øst'),('zoo'),('abe')) AS t(v) "
                        + "ORDER BY v COLLATE \"da-DK-x-icu\"",
                String.class);
        assertEquals(List.of("abe", "zoo", "øst"), danishOrder,
                "String.compareTo() của Java sẽ xếp sai chỗ này");
    }

    @Test
    @DisplayName("Locale da trả đủ ba miền, kèm Content-Language và Vary")
    void localeDaReturnsAllThreeRegions() {
        ResponseEntity<Region[]> response = call("dk", "da");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().length);
        assertEquals("Nordvietnam", response.getBody()[0].getName());
        assertEquals(0, response.getBody()[0].getProductCount());
        assertEquals("da", response.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE));
        assertTrue(response.getHeaders().getVary().contains(HttpHeaders.ACCEPT_LANGUAGE),
                "Thiếu Vary là CDN phục vụ bản tiếng Đan cho khách Việt");
    }

    @Test
    @DisplayName("Miền thiếu bản dịch vi BIẾN MẤT khỏi locale vi — không fallback về da")
    void localeViHidesUntranslatedRegion() {
        ResponseEntity<Region[]> response = call("dk", "vi");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().length,
                "Miền SOUTH chỉ có bản da nên phải biến mất khỏi locale vi");

        List<String> name = List.of(response.getBody()[0].getName(), response.getBody()[1].getName());
        assertEquals(List.of("Miền Bắc", "Miền Trung"), name);
        assertTrue(name.stream().noneMatch(t -> t.contains("Sydvietnam")),
                "Không được hiện bản tiếng Đan thay thế — docs/02 mục 4");
    }

    @Test
    @DisplayName("Market và locale độc lập: mua ở dk nhưng đọc vi vẫn hợp lệ")
    void marketAndLocaleAreIndependent() {
        // Thị trường VN đang tắt trong dữ liệu tra cứu vì sáu con số nghiệp vụ
        // của nó chưa ai quyết (docs/41 mục 4, Q-2). Bật lên để kiểm đúng điều
        // cần kiểm ở đây: hai tham số độc lập với nhau.
        jdbc.update("UPDATE market SET is_active = TRUE WHERE code = 'VN'");

        assertEquals(HttpStatus.OK, call("dk", "vi").getStatusCode());
        assertEquals(HttpStatus.OK, call("vn", "da").getStatusCode());
    }

    @Test
    @DisplayName("Thị trường đang tắt trả 404, không trả danh sách rỗng")
    void disabledMarketReturns404() {
        // Rỗng và "không tồn tại" là hai câu trả lời khác nhau: rỗng nói với
        // khách rằng thị trường này có tồn tại nhưng chưa có gì để bán.
        ResponseEntity<ErrorResponse> response = callExpectingError("vn", "vi");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getCode());
    }

    @Test
    @DisplayName("Ngôn ngữ không hỗ trợ thì trả 400 kèm mã, không tự đoán giùm khách")
    void unsupportedLanguageReturns400() {
        ResponseEntity<ErrorResponse> response = callExpectingError("dk", "de");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("UNSUPPORTED_LOCALE", response.getBody().getCode());
    }

    @Test
    @DisplayName("Bản ghi xoá mềm biến mất khỏi kết quả, không hiện ra khách")
    void softDeletedRowsDisappearFromResults() {
        jdbc.update("UPDATE region_translation SET soft_delete = TRUE, last_modified_by = NULL "
                + "WHERE region_id = 'd0000000-0000-4000-8000-000000000001' AND locale = 'da'");

        ResponseEntity<Region[]> response = call("dk", "da");

        assertEquals(2, response.getBody().length,
                "Miền có bản dịch da đã xoá mềm phải rơi khỏi kết quả");
        assertTrue(java.util.Arrays.stream(response.getBody())
                        .noneMatch(r -> "Nordvietnam".equals(r.getName())),
                "Xoá mềm hoạt động âm thầm — quên lọc là rò dữ liệu đã xoá ra khách");
    }

    @Test
    @DisplayName("Slug của bản ghi đã xoá mềm dùng lại được — index duy nhất phải bộ phận")
    void slugReusableAfterSoftDelete() {
        jdbc.update("UPDATE region_translation SET soft_delete = TRUE "
                + "WHERE region_id = 'd0000000-0000-4000-8000-000000000001' AND locale = 'da'");

        jdbc.update("INSERT INTO region (id, code, sort_order) VALUES "
                + "('d0000000-0000-4000-8000-000000000004','NORTH_MOI',4)");

        // Không có WHERE NOT soft_delete trên index thì câu này ném lỗi trùng khoá,
        // và biên tập viên không hiểu vì sao slug của tour đã xoá vẫn bị chiếm.
        jdbc.update("INSERT INTO region_translation (region_id, locale, slug, name) VALUES "
                + "('d0000000-0000-4000-8000-000000000004','da','nordvietnam','Nordvietnam ny')");

        assertEquals(3, call("dk", "da").getBody().length);
    }

    @Test
    @DisplayName("Trigger tự đặt last_modified_at, ứng dụng không phải nhớ")
    void triggerSetsLastModifiedAt() {
        jdbc.update("UPDATE region SET sort_order = 9 "
                + "WHERE id = 'd0000000-0000-4000-8000-000000000001'");

        Boolean changed = jdbc.queryForObject(
                "SELECT last_modified_at > created_at FROM region "
                        + "WHERE id = 'd0000000-0000-4000-8000-000000000001'",
                Boolean.class);

        assertEquals(Boolean.TRUE, changed,
                "Thiếu trigger thì cột \"sửa lần cuối\" đứng yên vĩnh viễn");
    }

    @Test
    @DisplayName("Bảng nhật ký booking_event cố tình KHÔNG có soft_delete")
    void auditLogHasNoSoftDelete() {
        Integer columnCount = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_name = 'booking_event' "
                        + "AND column_name IN ('soft_delete','last_modified_by')",
                Integer.class);

        assertEquals(0, columnCount,
                "Thêm soft_delete vào nhật ký kiểm toán là cho phép giấu lịch sử");
    }

    private ResponseEntity<ErrorResponse> callExpectingError(String market, String locale) {
        return client().get()
                .uri("/api/v1/{market}/regions", market)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(ErrorResponse.class);
    }

    private ResponseEntity<Region[]> call(String market, String locale) {
        return client().get()
                .uri("/api/v1/{market}/regions", market)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(Region[].class);
    }

    private RestClient client() {
        // defaultStatusHandler nuốt lỗi để test đọc được cả phản hồi 4xx.
        return RestClient.builder()
                .baseUrl("http://localhost:" + cong)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build();
    }
}
