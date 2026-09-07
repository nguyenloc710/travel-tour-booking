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
 *   <li>{@link #khongGuiDuocBaoGiaRong()} — một email báo giá không có dòng nào
 *       là một lần làm khách mất thời gian, và tư vấn viên không nhận ra vì màn
 *       hình của họ vẫn hiện đủ thông tin yêu cầu.
 *   <li>{@link #tongDoMayChuCongKhongNhanTuClient()} — client gửi tổng sai thì
 *       tổng vẫn đúng, vì không có chỗ nào nhận tổng từ client.
 *   <li>{@link #quaHanThiKhongChapNhanDuoc()} — quy tắc 4, không tự gia hạn.
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

    private static final String MAT_KHAU = "mat-khau-rat-dai";
    private static final int HAN_BAO_TRUOC = 7;
    private static final int SO_NGAY_HIEU_LUC = 14;

    @LocalServerPort
    int cong;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    QuoteSweeper quetHan;

    @BeforeEach
    void chuanBiDuLieu() {
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

        String bam = new BCryptPasswordEncoder().encode(MAT_KHAU);
        themNhanVien("cc100000-0000-4000-8000-000000000001", "bientap@travel.test", "Biên tập", bam, "EDITOR");
        themNhanVien("cc100000-0000-4000-8000-000000000002", "tuvan@travel.test", "Trần Tư Vấn", bam, "CONSULTANT");
    }

    // ------------------------------------------------- khách gửi yêu cầu

    @Test
    @DisplayName("Khách gửi yêu cầu: sinh một quote ở DRAFT, mã có tiền tố Q-")
    void guiYeuCauSinhDraft() {
        ResponseEntity<QuoteReceipt> phanHoi = guiYeuCau("da", thanYeuCau(
                "privat-rundrejse", 4, LocalDate.now().plusDays(60)));

        assertEquals(HttpStatus.CREATED, phanHoi.getStatusCode());
        QuoteReceipt bienNhan = phanHoi.getBody();
        assertNotNull(bienNhan);

        assertEquals(QuoteStatus.DRAFT, bienNhan.getStatus());
        // Tiền tố Q- để tổng đài không lẫn mã báo giá với mã đơn khi khách đọc
        // qua điện thoại.
        assertTrue(bienNhan.getReference().startsWith("Q-DK-"),
                "mã báo giá phải có tiền tố Q-DK-, nhận được " + bienNhan.getReference());
        assertNotNull(bienNhan.getCreatedAt());

        assertEquals(1, dem("SELECT count(*) FROM quote WHERE status = 'DRAFT'"));
    }

    @Test
    @DisplayName("Loại đặt thẳng được thì không hỏi giá — 422 PRODUCT_NOT_QUOTABLE")
    void loaiKhacKhongHoiGiaDuoc() {
        ResponseEntity<ErrorResponse> phanHoi = guiYeuCauLoi("da", thanYeuCau(
                "nord-til-syd", 2, LocalDate.now().plusDays(60)));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, phanHoi.getStatusCode());
        assertEquals("PRODUCT_NOT_QUOTABLE", phanHoi.getBody().getCode());
        assertEquals(0, dem("SELECT count(*) FROM quote"));
    }

    /**
     * Quy tắc 1 của docs/14 mục 7.
     *
     * <p>Frontend đã chặn ở lịch chọn, và backend <b>vẫn kiểm lại</b>: chặn ở một
     * phía là chặn được đúng những người dùng trình duyệt.
     */
    @Test
    @DisplayName("Ngày quá gần: 422 LEAD_TIME_NOT_MET, kèm ngày sớm nhất")
    void ngayQuaGan() {
        ResponseEntity<ErrorResponse> phanHoi = guiYeuCauLoi("da", thanYeuCau(
                "privat-rundrejse", 2, LocalDate.now().plusDays(HAN_BAO_TRUOC - 1)));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, phanHoi.getStatusCode());
        ErrorResponse loi = phanHoi.getBody();
        assertEquals("LEAD_TIME_NOT_MET", loi.getCode());

        // Tham số là DỮ LIỆU, không phải câu tiếng người: frontend dựng câu
        // "tour này cần báo trước 7 ngày, sớm nhất là 12/09" từ hai giá trị này.
        assertEquals(HAN_BAO_TRUOC, ((Number) loi.getParams().get("leadTimeDays")).intValue());
        assertEquals(LocalDate.now().plusDays(HAN_BAO_TRUOC).toString(),
                loi.getParams().get("earliestDate"));
    }

    @Test
    @DisplayName("Không có ngày mong muốn vẫn hỏi giá được")
    void khongCoNgayVanHoiDuoc() {
        String than = """
                {"productSlug":"privat-rundrejse","partySize":2,
                 "contactName":"Anne Sørensen","contactEmail":"anne@example.dk",
                 "contactPhone":"+4520000001"}
                """;

        assertEquals(HttpStatus.CREATED, guiYeuCau("da", than).getStatusCode());
    }

    /**
     * Slug phụ thuộc locale — cùng một tour có slug khác nhau ở {@code da} và
     * {@code vi}, và slug của locale này không mở được ở locale kia.
     */
    @Test
    @DisplayName("Slug của locale kia trả 404, không trả bản dịch thay thế")
    void slugCuaLocaleKiaTra404() {
        ResponseEntity<ErrorResponse> phanHoi = guiYeuCauLoi("vi", thanYeuCau(
                "privat-rundrejse", 2, LocalDate.now().plusDays(60)));

        assertEquals(HttpStatus.NOT_FOUND, phanHoi.getStatusCode());
        assertEquals("NOT_FOUND", phanHoi.getBody().getCode());
    }

    @Test
    @DisplayName("Gọi lại cùng Idempotency-Key: cùng mã, một dòng duy nhất")
    void goiLaiCungKhoa() {
        UUID khoa = UUID.randomUUID();
        String than = thanYeuCau("privat-rundrejse", 3, LocalDate.now().plusDays(60));

        QuoteReceipt lanMot = guiYeuCau("da", than, khoa).getBody();
        QuoteReceipt lanHai = guiYeuCau("da", than, khoa).getBody();

        assertNotNull(lanMot);
        assertNotNull(lanHai);
        // Khách bấm nút hai lần thì tư vấn viên KHÔNG được nhận hai yêu cầu
        // giống hệt nhau rồi gọi điện hai lần.
        assertEquals(lanMot.getReference(), lanHai.getReference());
        assertEquals(1, dem("SELECT count(*) FROM quote"));
    }

    // ------------------------------------------------- quyền của M8

    @Test
    @DisplayName("EDITOR không thấy màn hình báo giá — 403")
    void bienTapKhongThayBaoGia() {
        ResponseEntity<String> phanHoi = dangNhap("bientap@travel.test")
                .lay("/api/v1/admin/quotes", String.class);

        // Báo giá mang tên, điện thoại và yêu cầu riêng của khách; người viết
        // nội dung không có việc gì với dữ liệu đó (docs/31).
        assertEquals(HttpStatus.FORBIDDEN, phanHoi.getStatusCode());
    }

    @Test
    @DisplayName("Danh sách mặc định chỉ DRAFT — phần việc đang nợ")
    void danhSachMacDinhChiDraft() {
        guiYeuCau("da", thanYeuCau("privat-rundrejse", 2, LocalDate.now().plusDays(60)));
        String daGui = guiYeuCau("da", thanYeuCau("privat-rundrejse", 5, null))
                .getBody().getReference();

        Phien phien = dangNhap("tuvan@travel.test");
        dungBangGia(phien, daGui, "DKK", 1);
        doiTrangThai(phien, daGui, "SENT");

        AdminQuotePage macDinh = phien.lay("/api/v1/admin/quotes", AdminQuotePage.class).getBody();
        assertNotNull(macDinh);
        assertEquals(1L, macDinh.getTotalItems());
        assertEquals(QuoteStatus.DRAFT, macDinh.getItems().getFirst().getStatus());

        AdminQuotePage tatCa = phien
                .lay("/api/v1/admin/quotes?status=ALL", AdminQuotePage.class).getBody();
        assertNotNull(tatCa);
        assertEquals(2L, tatCa.getTotalItems());
    }

    // ------------------------------------------------- dựng bảng giá

    /**
     * {@code total} không có trong thân yêu cầu, và đó là chủ ý: nhận tổng rồi
     * tin là mở đường cho một báo giá mà tổng không khớp bảng — thứ khách mang
     * ra tranh cãi.
     */
    @Test
    @DisplayName("Tổng do máy chủ cộng từ các dòng, không nhận từ client")
    void tongDoMayChuCongKhongNhanTuClient() {
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2,
                LocalDate.now().plusDays(60))).getBody().getReference();

        Phien phien = dangNhap("tuvan@travel.test");
        ResponseEntity<AdminQuoteDetail> phanHoi = phien.goi(HttpMethod.PUT,
                "/api/v1/admin/quotes/" + ma + "/lines",
                """
                {"currency":"DKK","lines":[
                  {"labelKey":"line.base","quantity":"2","unitAmount":"18000.00","amount":"36000.00"},
                  {"labelKey":"line.guide","amount":"4000.00"},
                  {"labelKey":"line.earlyBird","amount":"-2000.00"}
                ]}
                """, AdminQuoteDetail.class);

        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
        AdminQuoteDetail bg = phanHoi.getBody();
        assertNotNull(bg);

        assertEquals(3, bg.getLines().size());
        // 36000 + 4000 − 2000. Dòng âm là giảm trừ, cùng quy ước với booking_line.
        assertEquals("38000.00", bg.getTotal().getAmount());
        assertEquals("DKK", bg.getTotal().getCurrency());
        assertEquals(HAN_BAO_TRUOC, bg.getLeadTimeDays());
        assertEquals(SO_NGAY_HIEU_LUC, bg.getQuoteValidDays());
    }

    /**
     * Không phải chuyện gõ nhầm ba chữ cái: báo giá thị trường {@code DK} ghi
     * bằng {@code VND} là một lần quy đổi tỷ giá đi vào hệ thống bằng cửa sau,
     * và hệ thống này <b>không có tỷ giá ở đâu cả</b>.
     */
    @Test
    @DisplayName("Sai tiền tệ so với thị trường: 400, không âm thầm quy đổi")
    void saiTienTe() {
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2,
                LocalDate.now().plusDays(60))).getBody().getReference();

        ResponseEntity<ErrorResponse> phanHoi = dangNhap("tuvan@travel.test").goi(HttpMethod.PUT,
                "/api/v1/admin/quotes/" + ma + "/lines",
                "{\"currency\":\"VND\",\"lines\":[{\"labelKey\":\"line.base\",\"amount\":\"1000\"}]}",
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, phanHoi.getStatusCode());
        assertEquals("VALIDATION_FAILED", phanHoi.getBody().getCode());
    }

    @Test
    @DisplayName("Đã gửi rồi thì không sửa bảng giá sau lưng khách — 409")
    void daGuiThiKhongSuaBangGia() {
        Phien phien = dangNhap("tuvan@travel.test");
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2, null))
                .getBody().getReference();

        dungBangGia(phien, ma, "DKK", 1);
        doiTrangThai(phien, ma, "SENT");

        ResponseEntity<ErrorResponse> phanHoi = phien.goi(HttpMethod.PUT,
                "/api/v1/admin/quotes/" + ma + "/lines",
                "{\"currency\":\"DKK\",\"lines\":[{\"labelKey\":\"line.base\",\"amount\":\"9\"}]}",
                ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, phanHoi.getStatusCode());
        assertEquals("QUOTE_NOT_ACCEPTABLE", phanHoi.getBody().getCode());
        assertEquals("SENT", phanHoi.getBody().getParams().get("from"));
    }

    // ------------------------------------------------- gửi và trả lời

    @Test
    @DisplayName("Không gửi được báo giá chưa có dòng nào — 409")
    void khongGuiDuocBaoGiaRong() {
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2, null))
                .getBody().getReference();

        ResponseEntity<ErrorResponse> phanHoi = dangNhap("tuvan@travel.test").goi(
                HttpMethod.POST, "/api/v1/admin/quotes/" + ma + "/status",
                "{\"toStatus\":\"SENT\"}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, phanHoi.getStatusCode());
        assertEquals("QUOTE_NOT_ACCEPTABLE", phanHoi.getBody().getCode());
        assertEquals(QuoteStatus.DRAFT.name(),
                jdbc.queryForObject("SELECT status FROM quote WHERE reference = ?",
                        String.class, ma));
    }

    /** Quy tắc 2: {@code valid_until = ngày gửi + quote_valid_days}, máy chủ tính. */
    @Test
    @DisplayName("Gửi đặt hạn = hôm nay + quoteValidDays, không nhận từ client")
    void guiDatHan() {
        Phien phien = dangNhap("tuvan@travel.test");
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2, null))
                .getBody().getReference();

        dungBangGia(phien, ma, "DKK", 1);
        AdminQuoteDetail sauKhiGui = doiTrangThai(phien, ma, "SENT");

        assertEquals(QuoteStatus.SENT, sauKhiGui.getStatus());
        assertEquals(LocalDate.now().plusDays(SO_NGAY_HIEU_LUC), sauKhiGui.getValidUntil());
    }

    @Test
    @DisplayName("Vòng đầy đủ: yêu cầu → dựng giá → gửi → khách nhận")
    void vongDayDu() {
        Phien phien = dangNhap("tuvan@travel.test");
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 4,
                LocalDate.now().plusDays(90))).getBody().getReference();

        assertEquals(QuoteStatus.DRAFT, chiTiet(phien, ma).getStatus());

        dungBangGia(phien, ma, "DKK", 2);
        doiTrangThai(phien, ma, "SENT");

        AdminQuoteDetail daNhan = doiTrangThai(phien, ma, "ACCEPTED");
        assertEquals(QuoteStatus.ACCEPTED, daNhan.getStatus());

        // Yêu cầu gốc của khách còn nguyên sau cả vòng — đó là thứ tư vấn viên
        // đọc lại khi khách gọi hỏi "tôi đã nói gì".
        assertEquals(4, daNhan.getPartySize());
        assertEquals("anne@example.dk", daNhan.getContactEmail());
        assertEquals("Hai người ăn chay.", daNhan.getMessage());
    }

    @Test
    @DisplayName("DRAFT không nhảy thẳng sang ACCEPTED — 409 kèm from và to")
    void draftKhongNhayThangSangAccepted() {
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2, null))
                .getBody().getReference();

        ResponseEntity<ErrorResponse> phanHoi = dangNhap("tuvan@travel.test").goi(
                HttpMethod.POST, "/api/v1/admin/quotes/" + ma + "/status",
                "{\"toStatus\":\"ACCEPTED\"}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, phanHoi.getStatusCode());
        assertEquals("QUOTE_NOT_ACCEPTABLE", phanHoi.getBody().getCode());
        assertEquals("DRAFT", phanHoi.getBody().getParams().get("from"));
        assertEquals("ACCEPTED", phanHoi.getBody().getParams().get("to"));
    }

    /**
     * Quy tắc 4 — không tự gia hạn.
     *
     * <p>Đẩy {@code valid_until} về quá khứ bằng SQL chứ không chờ đồng hồ: một
     * bài test chờ mười bốn ngày là một bài test không ai chạy.
     */
    @Test
    @DisplayName("Quá hạn thì không chấp nhận được — 409 QUOTE_EXPIRED")
    void quaHanThiKhongChapNhanDuoc() {
        Phien phien = dangNhap("tuvan@travel.test");
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2, null))
                .getBody().getReference();

        dungBangGia(phien, ma, "DKK", 1);
        doiTrangThai(phien, ma, "SENT");

        jdbc.update("UPDATE quote SET valid_until = ? WHERE reference = ?",
                java.sql.Date.valueOf(LocalDate.now().minusDays(1)), ma);

        ResponseEntity<ErrorResponse> phanHoi = phien.goi(HttpMethod.POST,
                "/api/v1/admin/quotes/" + ma + "/status",
                "{\"toStatus\":\"ACCEPTED\"}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, phanHoi.getStatusCode());
        assertEquals("QUOTE_EXPIRED", phanHoi.getBody().getCode());
    }

    @Test
    @DisplayName("Job quét hạn cho báo giá quá hạn sang EXPIRED, không đụng cái còn hạn")
    void jobQuetHan() {
        Phien phien = dangNhap("tuvan@travel.test");

        String quaHan = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2, null))
                .getBody().getReference();
        String conHan = guiYeuCau("da", thanYeuCau("privat-rundrejse", 3, null))
                .getBody().getReference();

        for (String ma : List.of(quaHan, conHan)) {
            dungBangGia(phien, ma, "DKK", 1);
            doiTrangThai(phien, ma, "SENT");
        }
        jdbc.update("UPDATE quote SET valid_until = ? WHERE reference = ?",
                java.sql.Date.valueOf(LocalDate.now().minusDays(1)), quaHan);

        assertEquals(1, quetHan.donDep());

        assertEquals("EXPIRED", trangThai(quaHan));
        assertEquals("SENT", trangThai(conHan));
    }

    @Test
    @DisplayName("Chi tiết một báo giá chưa dựng giá: không có tổng, không có hạn")
    void chuaDungGiaThiKhongCoTong() {
        String ma = guiYeuCau("da", thanYeuCau("privat-rundrejse", 2, null))
                .getBody().getReference();

        AdminQuoteDetail bg = chiTiet(dangNhap("tuvan@travel.test"), ma);

        // Trả 0 ở đây là bịa ra một con số mà màn hình sẽ hiển thị như một báo
        // giá miễn phí.
        assertNull(bg.getTotal());
        assertNull(bg.getValidUntil());
        assertTrue(bg.getLines().isEmpty());
    }

    // ------------------------------------------------------------ tiện ích

    private static String thanYeuCau(String slug, int soKhach, LocalDate ngay) {
        return """
                {"productSlug":"%s","partySize":%d,%s
                 "contactName":"Anne Sørensen","contactEmail":"anne@example.dk",
                 "contactPhone":"+4520000001","message":"Hai người ăn chay."}
                """.formatted(slug, soKhach,
                ngay == null ? "" : "\"requestedDate\":\"" + ngay + "\",");
    }

    private ResponseEntity<QuoteReceipt> guiYeuCau(String locale, String than) {
        return guiYeuCau(locale, than, UUID.randomUUID());
    }

    private ResponseEntity<QuoteReceipt> guiYeuCau(String locale, String than, UUID khoa) {
        return khach(locale, than, khoa, QuoteReceipt.class);
    }

    private ResponseEntity<ErrorResponse> guiYeuCauLoi(String locale, String than) {
        return khach(locale, than, UUID.randomUUID(), ErrorResponse.class);
    }

    private <T> ResponseEntity<T> khach(String locale, String than, UUID khoa, Class<T> kieu) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + cong)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build()
                .post()
                .uri("/api/v1/dk/quote-requests")
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .header("Idempotency-Key", khoa.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(than)
                .retrieve()
                .toEntity(kieu);
    }

    private AdminQuoteDetail chiTiet(Phien phien, String ma) {
        ResponseEntity<AdminQuoteDetail> phanHoi =
                phien.lay("/api/v1/admin/quotes/" + ma, AdminQuoteDetail.class);
        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
        return phanHoi.getBody();
    }

    private void dungBangGia(Phien phien, String ma, String tienTe, int soDong) {
        StringBuilder dong = new StringBuilder();
        for (int i = 1; i <= soDong; i++) {
            dong.append(i > 1 ? "," : "")
                    .append("{\"labelKey\":\"line.base\",\"amount\":\"%d000.00\"}".formatted(i));
        }
        ResponseEntity<AdminQuoteDetail> phanHoi = phien.goi(HttpMethod.PUT,
                "/api/v1/admin/quotes/" + ma + "/lines",
                "{\"currency\":\"%s\",\"lines\":[%s]}".formatted(tienTe, dong),
                AdminQuoteDetail.class);
        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
    }

    private AdminQuoteDetail doiTrangThai(Phien phien, String ma, String sang) {
        ResponseEntity<AdminQuoteDetail> phanHoi = phien.goi(HttpMethod.POST,
                "/api/v1/admin/quotes/" + ma + "/status",
                "{\"toStatus\":\"%s\"}".formatted(sang), AdminQuoteDetail.class);
        assertEquals(HttpStatus.OK, phanHoi.getStatusCode());
        return phanHoi.getBody();
    }

    private String trangThai(String ma) {
        return jdbc.queryForObject("SELECT status FROM quote WHERE reference = ?",
                String.class, ma);
    }

    private int dem(String sql) {
        Integer so = jdbc.queryForObject(sql, Integer.class);
        return so == null ? 0 : so;
    }

    private void themNhanVien(String id, String email, String ten, String bam, String vaiTro) {
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash, is_active)
                VALUES (CAST(? AS uuid), ?, ?, ?, TRUE)
                """, id, email, ten, bam);
        jdbc.update("""
                INSERT INTO staff_user_role (id, staff_user_id, role_code)
                VALUES (gen_random_uuid(), CAST(? AS uuid), ?)
                """, id, vaiTro);
    }

    private Phien dangNhap(String email) {
        Phien phien = new Phien();
        assertEquals(HttpStatus.NO_CONTENT, phien.dangNhap(email, MAT_KHAU).getStatusCode());
        return phien;
    }

    private final class Phien {

        private final List<String> cookies = new ArrayList<>();

        ResponseEntity<String> dangNhap(String email, String matKhau) {
            return goi(HttpMethod.POST, "/api/v1/admin/session",
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, matKhau),
                    String.class);
        }

        <T> ResponseEntity<T> lay(String duongDan, Class<T> kieu) {
            return goi(HttpMethod.GET, duongDan, null, kieu);
        }

        <T> ResponseEntity<T> goi(HttpMethod phuongThuc, String duongDan, String than, Class<T> kieu) {
            RestClient.RequestBodySpec yeuCau = RestClient.builder()
                    .baseUrl("http://localhost:" + cong)
                    .defaultStatusHandler(status -> true, (req, res) -> { })
                    .build()
                    .method(phuongThuc)
                    .uri(duongDan);

            for (String c : cookies) {
                yeuCau.header(HttpHeaders.COOKIE, c);
            }
            thecCsrf().ifPresent(t -> yeuCau.header("X-XSRF-TOKEN", t));

            if (than != null) {
                yeuCau.contentType(MediaType.APPLICATION_JSON).body(than);
            }

            ResponseEntity<T> phanHoi = yeuCau.retrieve().toEntity(kieu);
            nhoCookie(phanHoi);
            return phanHoi;
        }

        private void nhoCookie(ResponseEntity<?> phanHoi) {
            List<String> moi = phanHoi.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (moi == null) {
                return;
            }
            for (String c : moi) {
                String rutGon = c.split(";", 2)[0];
                String ten = rutGon.split("=", 2)[0];
                cookies.removeIf(cu -> cu.startsWith(ten + "="));
                cookies.add(rutGon);
            }
        }

        private Optional<String> thecCsrf() {
            return cookies.stream()
                    .filter(c -> c.startsWith("XSRF-TOKEN="))
                    .map(c -> c.substring("XSRF-TOKEN=".length()))
                    .findFirst();
        }
    }
}
