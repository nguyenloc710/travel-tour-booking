package vn.travel.booking.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.travel.booking.quote.service.QuoteSweeper;
import vn.travel.booking.web.generated.model.AdminQuoteDetail;
import vn.travel.booking.web.generated.model.AdminQuotePage;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.QuoteReceipt;
import vn.travel.booking.web.generated.model.QuoteStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Luồng báo giá đầu-cuối — docs/23 mục 7, vòng đời docs/14 mục 7.
 *
 * <p>Đây là thứ làm cho CTA "Yêu cầu báo giá" của {@code PRIVATE_TOUR} dẫn tới
 * một chỗ có thật, và là tiêu chí ra số 6 của cổng G4 (docs/40).
 *
 * <p>Ba bài đáng chú ý nhất:
 *
 * <ul>
 *   <li>{@link #cannotSendEmptyQuote()} — một email báo giá không có dòng nào
 *       là một lần làm khách mất thời gian, và tư vấn viên không nhận ra vì màn
 *       hình của họ vẫn hiện đủ thông tin yêu cầu.
 *   <li>{@link #totalComputedByServerNotClient()} — client gửi tổng sai thì
 *       tổng vẫn đúng, vì không có chỗ nào nhận tổng từ client.
 *   <li>{@link #expiredQuoteCannotBeAccepted()} — quy tắc 4, không tự gia hạn.
 * </ul>
 *
 * <h2>Bộ dữ liệu</h2>
 *
 * <pre>
 * P1 privat-rundrejse      PRIVATE_TOUR · da + vi · DK, VN · lead 7 ngày · hạn 14 ngày
 * P2 nord-til-syd          GROUP_TOUR   · da      · DK      · không đi qua báo giá
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class QuoteFlowIT {

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

    private static final String PASSWORD = "mat-khau-rat-dai";
    private static final int LEAD_TIME_DAYS = 7;
    private static final int QUOTE_VALID_DAYS = 14;

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    QuoteSweeper sweepExpiry;

    @BeforeEach
    void prepareData() {
        jdbc.execute("""
                DELETE FROM staff_user_role;
                DELETE FROM quote_line;
                DELETE FROM quote;
                DELETE FROM idempotency_key;
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_private;
                DELETE FROM product_group_tour;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM slug_history;
                DELETE FROM staff_user;

                INSERT INTO region (id, code, sort_order) VALUES
                  ('cc000000-0000-4000-8000-0000000000f1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('cc000000-0000-4000-8000-0000000000f1','da','nordvietnam','Nordvietnam'),
                  ('cc000000-0000-4000-8000-0000000000f1','vi','mien-bac','Miền Bắc');
                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('cc000000-0000-4000-8000-0000000000f2',
                   'cc000000-0000-4000-8000-0000000000f1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('cc000000-0000-4000-8000-0000000000f2','da','hanoi','Hanoi'),
                  ('cc000000-0000-4000-8000-0000000000f2','vi','ha-noi','Hà Nội');

                -- P1 — tour riêng. Bản `da` là NGUỒN, bản `vi` là bản dịch.
                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('cc000000-0000-4000-8000-000000000001','PRIVATE_TOUR',
                        'cc000000-0000-4000-8000-0000000000f2',10,'/img/privat.jpg');
                INSERT INTO product_private (product_id, lead_time_days, quote_valid_days)
                VALUES ('cc000000-0000-4000-8000-000000000001',7,14);
                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status) VALUES
                  ('cc000000-0000-4000-8000-000000000001','da','privat-rundrejse','Privat rundrejse',
                   'Jeres egen rejse.', ARRAY['Et.','To.'], ARRAY['A','B','C'],'Rismarker','PUBLISHED'),
                  ('cc000000-0000-4000-8000-000000000001','vi','tour-rieng','Tour riêng',
                   'Chuyến đi của riêng bạn.', ARRAY['Một.','Hai.'], ARRAY['A','B','C'],
                   'Ruộng bậc thang','PUBLISHED');
                INSERT INTO product_market (product_id, market, is_published) VALUES
                  ('cc000000-0000-4000-8000-000000000001','DK',TRUE),
                  ('cc000000-0000-4000-8000-000000000001','VN',TRUE);

                -- P2 — tour đoàn, đặt thẳng được, KHÔNG đi qua báo giá.
                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('cc000000-0000-4000-8000-000000000002','GROUP_TOUR',
                        'cc000000-0000-4000-8000-0000000000f2',14,'/img/p.jpg');
                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES ('cc000000-0000-4000-8000-000000000002',12,20,12,'da',2);
                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status) VALUES
                  ('cc000000-0000-4000-8000-000000000002','da','nord-til-syd','Nord til syd',
                   'Hele landet.', ARRAY['Et.','To.'], ARRAY['A','B','C'],'Rismarker','PUBLISHED');
                INSERT INTO product_market (product_id, market, is_published) VALUES
                  ('cc000000-0000-4000-8000-000000000002','DK',TRUE);
                """);

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        addStaff("cc100000-0000-4000-8000-000000000001", "bientap@travel.test", "Biên tập", hash, "EDITOR");
        addStaff("cc100000-0000-4000-8000-000000000002", "tuvan@travel.test", "Trần Tư Vấn", hash, "CONSULTANT");
    }

    // ------------------------------------------------- khách gửi yêu cầu

    @Test
    @DisplayName("Khách gửi yêu cầu: sinh một quote ở DRAFT, mã có tiền tố Q-")
    void quoteRequestCreatesDraftWithQPrefix() {
        ResponseEntity<QuoteReceipt> response = sendQuoteRequest("da", requestBody(
                "privat-rundrejse", 4, LocalDate.now().plusDays(60)));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        QuoteReceipt receipt = response.getBody();
        assertNotNull(receipt);

        assertEquals(QuoteStatus.DRAFT, receipt.getStatus());
        // Tiền tố Q- để tổng đài không lẫn mã báo giá với mã đơn khi khách đọc
        // qua điện thoại.
        assertTrue(receipt.getReference().startsWith("Q-DK-"),
                "mã báo giá phải có tiền tố Q-DK-, nhận được " + receipt.getReference());
        assertNotNull(receipt.getCreatedAt());

        assertEquals(1, count("SELECT count(*) FROM quote WHERE status = 'DRAFT'"));
    }

    @Test
    @DisplayName("Loại đặt thẳng được thì không hỏi giá — 422 PRODUCT_NOT_QUOTABLE")
    void directlyBookableTypeIsNotQuotable() {
        ResponseEntity<ErrorResponse> response = sendQuoteRequestExpectingError("da", requestBody(
                "nord-til-syd", 2, LocalDate.now().plusDays(60)));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertEquals("PRODUCT_NOT_QUOTABLE", response.getBody().getCode());
        assertEquals(0, count("SELECT count(*) FROM quote"));
    }

    /**
     * Quy tắc 1 của docs/14 mục 7.
     *
     * <p>Frontend đã chặn ở lịch chọn, và backend <b>vẫn kiểm lại</b>: chặn ở một
     * phía là chặn được đúng những người dùng trình duyệt.
     */
    @Test
    @DisplayName("Ngày quá gần: 422 LEAD_TIME_NOT_MET, kèm ngày sớm nhất")
    void leadTimeNotMetReturns422() {
        ResponseEntity<ErrorResponse> response = sendQuoteRequestExpectingError("da", requestBody(
                "privat-rundrejse", 2, LocalDate.now().plusDays(LEAD_TIME_DAYS - 1)));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        ErrorResponse error = response.getBody();
        assertEquals("LEAD_TIME_NOT_MET", error.getCode());
        // docs/14 mục 2.3: lỗi không đạt hạn báo trước PHẢI trả về đúng con số cấu hình
        // và ngày sớm nhất có thể đi, để giao diện điền thẳng vào ô chọn ngày.
        assertNotNull(error.getParams());
        assertEquals(LEAD_TIME_DAYS, ((Number) error.getParams().get("leadTimeDays")).intValue());
        assertEquals(LocalDate.now().plusDays(LEAD_TIME_DAYS).toString(),
                error.getParams().get("earliestDate"));
    }

    @Test
    @DisplayName("Không có ngày mong muốn vẫn hỏi giá được")
    void quoteWorksWithoutPreferredDate() {
        String body = """
                {"productSlug":"privat-rundrejse","partySize":2,
                 "contactName":"Anne Sørensen","contactEmail":"anne@example.dk",
                 "contactPhone":"+4520000001"}
                """;

        assertEquals(HttpStatus.CREATED, sendQuoteRequest("da", body).getStatusCode());
    }

    /**
     * Slug phụ thuộc locale — cùng một tour có slug khác nhau ở {@code da} và
     * {@code vi}, và slug của locale này không mở được ở locale kia.
     */
    @Test
    @DisplayName("Slug của locale kia trả 404, không trả bản dịch thay thế")
    void otherLocaleSlugReturns404() {
        ResponseEntity<ErrorResponse> response = sendQuoteRequestExpectingError("vi", requestBody(
                "privat-rundrejse", 2, LocalDate.now().plusDays(60)));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().getCode());
    }

    @Test
    @DisplayName("Gọi lại cùng Idempotency-Key: cùng mã, một dòng duy nhất")
    void sameIdempotencyKeyReturnsSameBooking() {
        UUID key = UUID.randomUUID();
        String body = requestBody("privat-rundrejse", 3, LocalDate.now().plusDays(60));

        QuoteReceipt firstAttempt = sendQuoteRequest("da", body, key).getBody();
        QuoteReceipt secondAttempt = sendQuoteRequest("da", body, key).getBody();

        assertNotNull(firstAttempt);
        assertNotNull(secondAttempt);
        // Khách bấm nút hai lần thì tư vấn viên KHÔNG được nhận hai yêu cầu
        // giống hệt nhau rồi gọi điện hai lần.
        assertEquals(firstAttempt.getReference(), secondAttempt.getReference());
        assertEquals(1, count("SELECT count(*) FROM quote"));
    }

    // ------------------------------------------------- quyền của M8

    @Test
    @DisplayName("EDITOR không thấy màn hình báo giá — 403")
    void editorCannotSeeQuotes() {
        ResponseEntity<String> response = login("bientap@travel.test")
                .get("/api/v1/admin/quotes", String.class);

        // Báo giá mang tên, điện thoại và yêu cầu riêng của khách; người viết
        // nội dung không có việc gì với dữ liệu đó (docs/31).
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Danh sách mặc định chỉ DRAFT — phần việc đang nợ")
    void listDefaultsToDraftOnly() {
        sendQuoteRequest("da", requestBody("privat-rundrejse", 2, LocalDate.now().plusDays(60)));
        String sent = sendQuoteRequest("da", requestBody("privat-rundrejse", 5, null))
                .getBody().getReference();

        Session session = login("tuvan@travel.test");
        buildPriceTiers(session, sent, "DKK", 1);
        changeStatus(session, sent, "SENT");

        AdminQuotePage defaults = session.get("/api/v1/admin/quotes", AdminQuotePage.class).getBody();
        assertNotNull(defaults);
        assertEquals(1L, defaults.getTotalItems());
        assertEquals(QuoteStatus.DRAFT, defaults.getItems().getFirst().getStatus());

        AdminQuotePage all = session
                .get("/api/v1/admin/quotes?status=ALL", AdminQuotePage.class).getBody();
        assertNotNull(all);
        assertEquals(2L, all.getTotalItems());
    }

    // ------------------------------------------------- dựng bảng giá

    /**
     * {@code total} không có trong thân yêu cầu, và đó là chủ ý: nhận tổng rồi
     * tin là mở đường cho một báo giá mà tổng không khớp bảng — thứ khách mang
     * ra tranh cãi.
     */
    @Test
    @DisplayName("Tổng do máy chủ cộng từ các dòng, không nhận từ client")
    void totalComputedByServerNotClient() {
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 2,
                LocalDate.now().plusDays(60))).getBody().getReference();

        Session session = login("tuvan@travel.test");
        ResponseEntity<AdminQuoteDetail> response = session.call(HttpMethod.PUT,
                "/api/v1/admin/quotes/" + reference + "/lines",
                """
                {"currency":"DKK","lines":[
                  {"labelKey":"line.base","quantity":"2","unitAmount":"18000.00","amount":"36000.00"},
                  {"labelKey":"line.guide","amount":"4000.00"},
                  {"labelKey":"line.earlyBird","amount":"-2000.00"}
                ]}
                """, AdminQuoteDetail.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AdminQuoteDetail detail = response.getBody();
        assertNotNull(detail);

        assertEquals(3, detail.getLines().size());
        // 36000 + 4000 − 2000. Dòng âm là giảm trừ, cùng quy ước với booking_line.
        assertEquals("38000.00", detail.getTotal().getAmount());
        assertEquals("DKK", detail.getTotal().getCurrency());
        assertEquals(LEAD_TIME_DAYS, detail.getLeadTimeDays());
        assertEquals(QUOTE_VALID_DAYS, detail.getQuoteValidDays());
    }

    /**
     * Không phải chuyện gõ nhầm ba chữ cái: báo giá thị trường {@code DK} ghi
     * bằng {@code VND} là một lần quy đổi tỷ giá đi vào hệ thống bằng cửa sau,
     * và hệ thống này <b>không có tỷ giá ở đâu cả</b>.
     */
    @Test
    @DisplayName("Sai tiền tệ so với thị trường: 400, không âm thầm quy đổi")
    void wrongCurrencyReturns400NoConversion() {
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 2,
                LocalDate.now().plusDays(60))).getBody().getReference();

        ResponseEntity<ErrorResponse> response = login("tuvan@travel.test").call(HttpMethod.PUT,
                "/api/v1/admin/quotes/" + reference + "/lines",
                "{\"currency\":\"VND\",\"lines\":[{\"labelKey\":\"line.base\",\"amount\":\"1000\"}]}",
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_FAILED", response.getBody().getCode());
    }

    @Test
    @DisplayName("Đã gửi rồi thì không sửa bảng giá sau lưng khách — 409")
    void sentQuoteCannotEditPriceTiers() {
        Session session = login("tuvan@travel.test");
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 2, null))
                .getBody().getReference();

        buildPriceTiers(session, reference, "DKK", 1);
        changeStatus(session, reference, "SENT");

        ResponseEntity<ErrorResponse> response = session.call(HttpMethod.PUT,
                "/api/v1/admin/quotes/" + reference + "/lines",
                "{\"currency\":\"DKK\",\"lines\":[{\"labelKey\":\"line.base\",\"amount\":\"9\"}]}",
                ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("QUOTE_NOT_ACCEPTABLE", response.getBody().getCode());
        assertEquals("SENT", response.getBody().getParams().get("from"));
    }

    // ------------------------------------------------- gửi và trả lời

    @Test
    @DisplayName("Không gửi được báo giá chưa có dòng nào — 409")
    void cannotSendEmptyQuote() {
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 2, null))
                .getBody().getReference();

        ResponseEntity<ErrorResponse> response = login("tuvan@travel.test").call(
                HttpMethod.POST, "/api/v1/admin/quotes/" + reference + "/status",
                "{\"toStatus\":\"SENT\"}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("QUOTE_NOT_ACCEPTABLE", response.getBody().getCode());
        assertEquals(QuoteStatus.DRAFT.name(),
                jdbc.queryForObject("SELECT status FROM quote WHERE reference = ?",
                        String.class, reference));
    }

    /** Quy tắc 2: {@code valid_until = ngày gửi + quote_valid_days}, máy chủ tính. */
    @Test
    @DisplayName("Gửi đặt hạn = hôm nay + quoteValidDays, không nhận từ client")
    void sendingSetsValidUntilFromServer() {
        Session session = login("tuvan@travel.test");
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 2, null))
                .getBody().getReference();

        buildPriceTiers(session, reference, "DKK", 1);
        AdminQuoteDetail afterSend = changeStatus(session, reference, "SENT");

        assertEquals(QuoteStatus.SENT, afterSend.getStatus());
        assertEquals(LocalDate.now().plusDays(QUOTE_VALID_DAYS), afterSend.getValidUntil());
    }

    @Test
    @DisplayName("Vòng đầy đủ: yêu cầu → dựng giá → gửi → khách nhận")
    void fullRoundRequestPriceSendAccept() {
        Session session = login("tuvan@travel.test");
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 4,
                LocalDate.now().plusDays(90))).getBody().getReference();

        assertEquals(QuoteStatus.DRAFT, detail(session, reference).getStatus());

        buildPriceTiers(session, reference, "DKK", 2);
        changeStatus(session, reference, "SENT");

        AdminQuoteDetail accepted = changeStatus(session, reference, "ACCEPTED");
        assertEquals(QuoteStatus.ACCEPTED, accepted.getStatus());

        // Yêu cầu gốc của khách còn nguyên sau cả vòng — đó là thứ tư vấn viên
        // đọc lại khi khách gọi hỏi "tôi đã nói gì".
        assertEquals(4, accepted.getPartySize());
        assertEquals("anne@example.dk", accepted.getContactEmail());
        assertEquals("Hai người ăn chay.", accepted.getMessage());
    }

    @Test
    @DisplayName("DRAFT không nhảy thẳng sang ACCEPTED — 409 kèm from và to")
    void draftCannotJumpToAccepted() {
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 2, null))
                .getBody().getReference();

        ResponseEntity<ErrorResponse> response = login("tuvan@travel.test").call(
                HttpMethod.POST, "/api/v1/admin/quotes/" + reference + "/status",
                "{\"toStatus\":\"ACCEPTED\"}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("QUOTE_NOT_ACCEPTABLE", response.getBody().getCode());
        assertEquals("DRAFT", response.getBody().getParams().get("from"));
        assertEquals("ACCEPTED", response.getBody().getParams().get("to"));
    }

    /**
     * Quy tắc 4 — không tự gia hạn.
     *
     * <p>Đẩy {@code valid_until} về quá khứ bằng SQL chứ không chờ đồng hồ: một
     * bài test chờ mười bốn ngày là một bài test không ai chạy.
     */
    @Test
    @DisplayName("Quá hạn thì không chấp nhận được — 409 QUOTE_EXPIRED")
    void expiredQuoteCannotBeAccepted() {
        Session session = login("tuvan@travel.test");
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 2, null))
                .getBody().getReference();

        buildPriceTiers(session, reference, "DKK", 1);
        changeStatus(session, reference, "SENT");

        jdbc.update("UPDATE quote SET valid_until = ? WHERE reference = ?",
                java.sql.Date.valueOf(LocalDate.now().minusDays(1)), reference);

        ResponseEntity<ErrorResponse> response = session.call(HttpMethod.POST,
                "/api/v1/admin/quotes/" + reference + "/status",
                "{\"toStatus\":\"ACCEPTED\"}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("QUOTE_EXPIRED", response.getBody().getCode());
    }

    @Test
    @DisplayName("Job quét hạn cho báo giá quá hạn sang EXPIRED, không đụng cái còn hạn")
    void expirySweepMovesOverdueQuotesToExpired() {
        Session session = login("tuvan@travel.test");

        String expired = sendQuoteRequest("da", requestBody("privat-rundrejse", 2, null))
                .getBody().getReference();
        String stillValid = sendQuoteRequest("da", requestBody("privat-rundrejse", 3, null))
                .getBody().getReference();

        for (String reference : List.of(expired, stillValid)) {
            buildPriceTiers(session, reference, "DKK", 1);
            changeStatus(session, reference, "SENT");
        }
        jdbc.update("UPDATE quote SET valid_until = ? WHERE reference = ?",
                java.sql.Date.valueOf(LocalDate.now().minusDays(1)), expired);

        assertEquals(1, sweepExpiry.cleanup());

        assertEquals("EXPIRED", status(expired));
        assertEquals("SENT", status(stillValid));
    }

    @Test
    @DisplayName("Chi tiết một báo giá chưa dựng giá: không có tổng, không có hạn")
    void quoteWithoutLinesHasNoTotalOrExpiry() {
        String reference = sendQuoteRequest("da", requestBody("privat-rundrejse", 2, null))
                .getBody().getReference();

        AdminQuoteDetail detail = detail(login("tuvan@travel.test"), reference);

        // docs/14 mục 2.3: yêu cầu mới nhận chưa tính giá, chưa có hạn thanh toán,
        // chưa có dòng giá nào.
        assertNull(detail.getTotal());
        assertNull(detail.getValidUntil());
        assertTrue(detail.getLines().isEmpty());
    }

    // ------------------------------------------------------------ tiện ích

    private static String requestBody(String slug, int paxCount, LocalDate requestedDate) {
        return """
                {"productSlug":"%s","partySize":%d,%s
                 "contactName":"Anne Sørensen","contactEmail":"anne@example.dk",
                 "contactPhone":"+4520000001","message":"Hai người ăn chay."}
                """.formatted(slug, paxCount,
                requestedDate == null ? "" : "\"requestedDate\":\"" + requestedDate + "\",");
    }

    private ResponseEntity<QuoteReceipt> sendQuoteRequest(String locale, String body) {
        return sendQuoteRequest(locale, body, UUID.randomUUID());
    }

    private ResponseEntity<QuoteReceipt> sendQuoteRequest(String locale, String body, UUID key) {
        return client(locale, body, key, QuoteReceipt.class);
    }

    private ResponseEntity<ErrorResponse> sendQuoteRequestExpectingError(String locale, String body) {
        return client(locale, body, UUID.randomUUID(), ErrorResponse.class);
    }

    private <T> ResponseEntity<T> client(String locale, String body, UUID key, Class<T> type) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build()
                .post()
                .uri("/api/v1/dk/quote-requests")
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .header("Idempotency-Key", key.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    private AdminQuoteDetail detail(Session session, String reference) {
        ResponseEntity<AdminQuoteDetail> response =
                session.get("/api/v1/admin/quotes/" + reference, AdminQuoteDetail.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private void buildPriceTiers(Session session, String reference, String currency, int rowCount) {
        StringBuilder row = new StringBuilder();
        for (int i = 1; i <= rowCount; i++) {
            row.append(i > 1 ? "," : "")
                    .append("{\"labelKey\":\"line.base\",\"amount\":\"%d000.00\"}".formatted(i));
        }
        ResponseEntity<AdminQuoteDetail> response = session.call(HttpMethod.PUT,
                "/api/v1/admin/quotes/" + reference + "/lines",
                "{\"currency\":\"%s\",\"lines\":[%s]}".formatted(currency, row),
                AdminQuoteDetail.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private AdminQuoteDetail changeStatus(Session session, String reference, String toStatus) {
        ResponseEntity<AdminQuoteDetail> response = session.call(HttpMethod.POST,
                "/api/v1/admin/quotes/" + reference + "/status",
                "{\"toStatus\":\"%s\"}".formatted(toStatus), AdminQuoteDetail.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private String status(String reference) {
        return jdbc.queryForObject("SELECT status FROM quote WHERE reference = ?",
                String.class, reference);
    }

    private int count(String sql) {
        Integer count = jdbc.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    private void addStaff(String id, String email, String name, String hash, String roles) {
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash, is_active)
                VALUES (CAST(? AS uuid), ?, ?, ?, TRUE)
                """, id, email, name, hash);
        jdbc.update("""
                INSERT INTO staff_user_role (id, staff_user_id, role_code)
                VALUES (gen_random_uuid(), CAST(? AS uuid), ?)
                """, id, roles);
    }

    private Session login(String email) {
        Session session = new Session();
        assertEquals(HttpStatus.NO_CONTENT, session.login(email, PASSWORD).getStatusCode());
        return session;
    }

    private final class Session {

        private final List<String> cookies = new ArrayList<>();

        ResponseEntity<String> login(String email, String password) {
            return call(HttpMethod.POST, "/api/v1/admin/session",
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password),
                    String.class);
        }

        <T> ResponseEntity<T> get(String path, Class<T> type) {
            return call(HttpMethod.GET, path, null, type);
        }

        <T> ResponseEntity<T> call(HttpMethod httpMethod, String path, String body, Class<T> type) {
            RestClient.RequestBodySpec request = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultStatusHandler(status -> true, (req, res) -> { })
                    .build()
                    .method(httpMethod)
                    .uri(path);

            for (String c : cookies) {
                request.header(HttpHeaders.COOKIE, c);
            }
            csrfToken().ifPresent(t -> request.header("X-XSRF-TOKEN", t));

            if (body != null) {
                request.contentType(MediaType.APPLICATION_JSON).body(body);
            }

            ResponseEntity<T> response = request.retrieve().toEntity(type);
            rememberCookies(response);
            return response;
        }

        private void rememberCookies(ResponseEntity<?> response) {
            List<String> newCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (newCookies == null) {
                return;
            }
            for (String c : newCookies) {
                String summary = c.split(";", 2)[0];
                String name = summary.split("=", 2)[0];
                cookies.removeIf(existing -> existing.startsWith(name + "="));
                cookies.add(summary);
            }
        }

        private Optional<String> csrfToken() {
            return cookies.stream()
                    .filter(c -> c.startsWith("XSRF-TOKEN="))
                    .map(c -> c.substring("XSRF-TOKEN=".length()))
                    .findFirst();
        }
    }
}
