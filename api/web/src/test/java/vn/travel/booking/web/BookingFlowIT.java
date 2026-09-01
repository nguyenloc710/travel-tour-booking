package vn.travel.booking.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.travel.booking.infrastructure.job.SeatHoldSweeper;
import vn.travel.booking.web.generated.model.Booking;
import vn.travel.booking.web.generated.model.BookingStatus;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.PriceBreakdown;
import vn.travel.booking.web.generated.model.PriceLine;
import vn.travel.booking.web.generated.model.SeatHold;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Đợt 4 — tính giá, giữ chỗ, đặt tour.
 *
 * <p>Test {@code haiKhachGianhChoCuoiCung} là <b>test quan trọng nhất của cả dự
 * án</b> (docs/14 mục 9.3): nó chạy hai transaction thật song song, không mô phỏng.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BookingFlowIT {

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

    private static final String NGAY_DI = "bb100000-0000-4000-8000-000000000001";
    private static final String NGAY_DI_CHOT = "bb100000-0000-4000-8000-000000000002";
    private static final String DIEM_KHOI_HANH = "bb200000-0000-4000-8000-000000000001";

    @LocalServerPort
    int cong;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SeatHoldSweeper quetDon;

    /**
     * Một tour đoàn với <b>đúng 2 chỗ</b> còn lại: sức chứa 20, đã đặt 18. Con số
     * nhỏ để test giành chỗ cuối cùng nói đúng điều nó muốn nói.
     */
    @BeforeEach
    void chuanBiDuLieu() {
        jdbc.execute("""
                DELETE FROM idempotency_key;
                DELETE FROM booking_event;
                DELETE FROM booking_passenger;
                DELETE FROM booking_line;
                DELETE FROM booking;
                DELETE FROM seat_hold;
                DELETE FROM departure_price;
                DELETE FROM departure;
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM departure_origin;
                DELETE FROM pax_type;

                INSERT INTO region (id, code, sort_order) VALUES
                  ('bb000000-0000-4000-8000-0000000000f1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('bb000000-0000-4000-8000-0000000000f1','da','nordvietnam','Nordvietnam');
                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('bb000000-0000-4000-8000-0000000000f2',
                   'bb000000-0000-4000-8000-0000000000f1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('bb000000-0000-4000-8000-0000000000f2','da','hanoi','Hanoi');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('bb000000-0000-4000-8000-000000000001','GROUP_TOUR',
                        'bb000000-0000-4000-8000-0000000000f2',14,'/img/p.jpg');
                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES ('bb000000-0000-4000-8000-000000000001',12,20,12,'da',2);
                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status)
                VALUES ('bb000000-0000-4000-8000-000000000001','da','nord-til-syd','Nord til syd',
                        'Hele landet.', ARRAY['Et.','To.'], ARRAY['A','B','C'],'Rismarker','PUBLISHED');
                INSERT INTO product_market (product_id, market, is_published, price_from)
                VALUES ('bb000000-0000-4000-8000-000000000001','DK',TRUE,24990.00);

                INSERT INTO pax_type (id, market, code, min_age, max_age, discount_rate, sort_order) VALUES
                  ('bb300000-0000-4000-8000-000000000001','DK','ADULT',12,NULL,0,1),
                  ('bb300000-0000-4000-8000-000000000002','DK','CHILD',2,11,0,2);

                INSERT INTO departure_origin (id, market, city, iata_code, surcharge, is_default) VALUES
                  ('bb200000-0000-4000-8000-000000000001','DK','Billund','BLL',800.00,FALSE);

                INSERT INTO departure (id, product_id, market, depart_date, return_date, days,
                                       base_status, capacity, seats_booked) VALUES
                  ('bb100000-0000-4000-8000-000000000001','bb000000-0000-4000-8000-000000000001','DK',
                   '2027-03-01','2027-03-14',14,'OPEN',20,18),
                  ('bb100000-0000-4000-8000-000000000002','bb000000-0000-4000-8000-000000000001','DK',
                   '2027-05-01','2027-05-14',14,'SOLD_OUT',20,0);

                INSERT INTO departure_price (departure_id, pax_type_id, occupancy, amount, currency) VALUES
                  ('bb100000-0000-4000-8000-000000000001','bb300000-0000-4000-8000-000000000001',
                   'DOUBLE',24990.00,'DKK'),
                  ('bb100000-0000-4000-8000-000000000001','bb300000-0000-4000-8000-000000000001',
                   'SINGLE',29490.00,'DKK'),
                  ('bb100000-0000-4000-8000-000000000001','bb300000-0000-4000-8000-000000000002',
                   'DOUBLE',18740.00,'DKK'),
                  ('bb100000-0000-4000-8000-000000000002','bb300000-0000-4000-8000-000000000001',
                   'DOUBLE',24990.00,'DKK');
                """);
    }

    // ------------------------------------------------------------ tính giá

    @Test
    @DisplayName("Xem trước giá: bảng phân rã cộng lại bằng tổng, và không lưu gì")
    void xemTruocGia() {
        PriceBreakdown b = goi(HttpMethod.POST, "/api/v1/dk/pricing/preview", null,
                ("{\"departureId\":\"%s\",\"pax\":[{\"paxTypeCode\":\"ADULT\",\"count\":2}],"
                        + "\"singleTravellers\":0,\"departureOriginId\":\"%s\"}")
                        .formatted(NGAY_DI, DIEM_KHOI_HANH),
                PriceBreakdown.class).getBody();

        // 2 × 24.990 + 2 × 800 phụ thu điểm khởi hành + 295 phí xử lý
        assertEquals("51875.00", b.getTotal().getAmount());
        assertEquals("12968.75", b.getDeposit().getAmount(), "25% làm tròn xuống");
        assertEquals("38906.25", b.getBalance().getAmount());

        assertEquals(0, dem("SELECT count(*) FROM booking"), "Xem trước giá KHÔNG lưu gì");
        assertEquals(0, dem("SELECT count(*) FROM seat_hold"));
    }

    @Test
    @DisplayName("Phụ thu phòng đơn tính bằng chênh giá phòng đơn trừ phòng đôi")
    void phuThuPhongDon() {
        PriceBreakdown b = goi(HttpMethod.POST, "/api/v1/dk/pricing/preview", null,
                ("{\"departureId\":\"%s\",\"pax\":[{\"paxTypeCode\":\"ADULT\",\"count\":1}],"
                        + "\"singleTravellers\":1}").formatted(NGAY_DI),
                PriceBreakdown.class).getBody();

        // 24.990 + (29.490 − 24.990) + 295
        assertEquals("29785.00", b.getTotal().getAmount());
        assertTrue(b.getLines().stream()
                .anyMatch(d -> d.getKind() == PriceLine.KindEnum.SINGLE_SUPPLEMENT));
    }

    // ------------------------------------------------------------ giữ chỗ

    @Test
    @DisplayName("Giữ chỗ thành công trả về hạn giữ")
    void giuChoThanhCong() {
        SeatHold giu = giuCho(2).getBody();

        assertNotNull(giu.getExpiresAt());
        assertEquals(2, giu.getSeats());
        assertEquals(1, dem("SELECT count(*) FROM seat_hold WHERE released_at IS NULL"));
    }

    @Test
    @DisplayName("Giữ nhiều hơn số chỗ còn lại thì 409 DEPARTURE_SOLD_OUT")
    void giuQuaSoCho() {
        ResponseEntity<ErrorResponse> phanHoi = goi(HttpMethod.POST, "/api/v1/dk/seat-holds",
                UUID.randomUUID(), thanGiuCho(3), ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, phanHoi.getStatusCode());
        assertEquals("DEPARTURE_SOLD_OUT", phanHoi.getBody().getCode());
    }

    @Test
    @DisplayName("Ngày khởi hành đã đóng bán thì 409, dù còn nguyên sức chứa")
    void ngayDaDongBan() {
        ResponseEntity<ErrorResponse> phanHoi = goi(HttpMethod.POST, "/api/v1/dk/seat-holds",
                UUID.randomUUID(),
                "{\"departureId\":\"%s\",\"seats\":1}".formatted(NGAY_DI_CHOT), ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, phanHoi.getStatusCode());
        assertEquals("DEPARTURE_CLOSED", phanHoi.getBody().getCode());
    }

    @Test
    @DisplayName("Chỗ đang giữ bị trừ khỏi chỗ khả dụng của người sau")
    void choDangGiuBiTru() {
        assertEquals(HttpStatus.CREATED, giuCho(2).getStatusCode());

        ResponseEntity<ErrorResponse> nguoiSau = goi(HttpMethod.POST, "/api/v1/dk/seat-holds",
                UUID.randomUUID(), thanGiuCho(1), ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, nguoiSau.getStatusCode(),
                "Hai chỗ cuối đang bị giữ — người sau không được bán tiếp");
    }

    @Test
    @DisplayName("Bỏ giữ chỗ trả chỗ về kho ngay")
    void boGiuCho() {
        SeatHold giu = giuCho(2).getBody();

        assertEquals(HttpStatus.NO_CONTENT,
                goi(HttpMethod.DELETE, "/api/v1/dk/seat-holds/" + giu.getId(), null, null,
                        Void.class).getStatusCode());

        assertEquals(HttpStatus.CREATED, giuCho(2).getStatusCode(),
                "Chỗ đã về kho nên người sau giữ được");
    }

    // ------------------------------------------- TEST QUAN TRỌNG NHẤT

    @Test
    @DisplayName("HAI KHÁCH cùng giành hai chỗ cuối: ĐÚNG MỘT người thành công")
    void haiKhachGianhChoCuoiCung() throws Exception {
        ExecutorService hai = Executors.newFixedThreadPool(2);
        try {
            Callable<HttpStatusCode> mua = () -> goi(HttpMethod.POST, "/api/v1/dk/seat-holds",
                    UUID.randomUUID(), thanGiuCho(2), Object.class).getStatusCode();

            // Hai transaction THẬT chạy song song, không mô phỏng.
            List<Future<HttpStatusCode>> ketQua = hai.invokeAll(List.of(mua, mua));

            long thanhCong = 0;
            long thatBai = 0;
            for (Future<HttpStatusCode> f : ketQua) {
                if (f.get(30, TimeUnit.SECONDS).value() == 201) {
                    thanhCong++;
                } else {
                    thatBai++;
                }
            }

            assertEquals(1, thanhCong, "Hai khách cùng mua được chỗ cuối là bug hạng nhất");
            assertEquals(1, thatBai, "Người thua phải nhận câu trả lời rõ ràng, không phải lỗi 500");
            assertEquals(1, dem("SELECT count(*) FROM seat_hold WHERE released_at IS NULL"));
        } finally {
            hai.shutdownNow();
        }
    }

    // ------------------------------------------------------------ hết hạn

    @Test
    @DisplayName("Giữ chỗ quá hạn không còn được tính, KỂ CẢ trước khi job quét chạy")
    void quaHanKhongConDuocTinh() {
        SeatHold giu = giuCho(2).getBody();
        heHan(giu.getId());

        assertEquals(HttpStatus.CREATED, giuCho(2).getStatusCode(),
                "Công thức dùng expires_at > now(), nên chỗ về kho ngay khi hết hạn");
        assertEquals(1, dem("SELECT count(*) FROM seat_hold "
                        + "WHERE released_at IS NULL AND expires_at <= now()"),
                "released_at vẫn NULL — job chỉ dọn dẹp, không phải cơ chế trả chỗ");
    }

    @Test
    @DisplayName("Job quét chạy hai lần cho cùng kết quả")
    void jobQuetBatBien() {
        heHan(giuCho(1).getBody().getId());

        assertEquals(1, quetDon.donDep(), "Lần đầu dọn một dòng");
        assertEquals(0, quetDon.donDep(), "Lần hai không còn gì để dọn — lệnh bất biến khi lặp");
    }

    // ------------------------------------------------------------ đặt tour

    @Test
    @DisplayName("Đặt tour đầu-cuối: đơn PENDING_PAYMENT, chỗ vào seats_booked, có nhật ký")
    void datTourDauCuoi() {
        SeatHold giu = giuCho(2).getBody();
        Booking don = datTour(giu.getId(), UUID.randomUUID()).getBody();

        assertEquals(BookingStatus.PENDING_PAYMENT, don.getStatus());
        assertEquals("Nord til syd", don.getProductTitle(), "Tên sản phẩm CHỤP LẠI lúc đặt");
        assertTrue(don.getReference().startsWith("DK-"));

        assertEquals(20, dem("SELECT seats_booked FROM departure WHERE id = CAST('"
                + NGAY_DI + "' AS uuid)"));
        assertEquals(0, dem("SELECT count(*) FROM seat_hold WHERE released_at IS NULL"),
                "Giữ chỗ đã chuyển thành đơn");
        assertEquals(1, dem("SELECT count(*) FROM booking_event WHERE to_status = 'PENDING_PAYMENT'"),
                "Mọi lần đổi trạng thái ghi nhật ký, kể cả lần đầu");
        assertEquals(2, dem("SELECT count(*) FROM booking_passenger"));
        assertTrue(dem("SELECT count(*) FROM booking_line") >= 2);
    }

    @Test
    @DisplayName("deposit + balance = total trên đơn đã lưu")
    void batBienDatCoc() {
        Booking don = datTour(giuCho(2).getBody().getId(), UUID.randomUUID()).getBody();

        BigDecimal tong = new BigDecimal(don.getTotal().getAmount());
        BigDecimal coc = new BigDecimal(don.getDeposit().getAmount());
        BigDecimal conLai = new BigDecimal(don.getBalance().getAmount());

        assertEquals(0, tong.compareTo(coc.add(conLai)));
    }

    @Test
    @DisplayName("Giữ chỗ đã hết hạn thì đặt tour trả 409 SEAT_HOLD_EXPIRED")
    void giuChoHetHanKhiDatTour() {
        SeatHold giu = giuCho(2).getBody();
        heHan(giu.getId());

        ResponseEntity<ErrorResponse> phanHoi = goi(HttpMethod.POST, "/api/v1/dk/bookings",
                UUID.randomUUID(), thanDatTour(giu.getId()), ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, phanHoi.getStatusCode());
        assertEquals("SEAT_HOLD_EXPIRED", phanHoi.getBody().getCode());
        assertEquals(18, dem("SELECT seats_booked FROM departure WHERE id = CAST('"
                        + NGAY_DI + "' AS uuid)"),
                "Đơn không thành thì seats_booked không được đổi");
    }

    @Test
    @DisplayName("Loại có tồn kho mà thiếu seatHoldId thì bị chặn")
    void thieuGiuCho() {
        ResponseEntity<ErrorResponse> phanHoi = goi(HttpMethod.POST, "/api/v1/dk/bookings",
                UUID.randomUUID(), thanDatTour(null), ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, phanHoi.getStatusCode(),
                "Không có giữ chỗ nghĩa là chỗ chưa bao giờ được khoá");
    }

    // ------------------------------------------------------------ gọi lại

    @Test
    @DisplayName("Gọi lại cùng Idempotency-Key trả về CÙNG đơn, không tạo đơn thứ hai")
    void goiLaiCungKhoa() {
        SeatHold giu = giuCho(2).getBody();
        UUID khoa = UUID.randomUUID();

        Booking lanMot = datTour(giu.getId(), khoa).getBody();
        ResponseEntity<Booking> lanHai = datTour(giu.getId(), khoa);

        assertEquals(HttpStatus.CREATED, lanHai.getStatusCode());
        assertEquals(lanMot.getReference(), lanHai.getBody().getReference(),
                "Khách bấm nút hai lần không được thành hai đơn");
        assertEquals(1, dem("SELECT count(*) FROM booking"));
        assertEquals(20, dem("SELECT seats_booked FROM departure WHERE id = CAST('"
                        + NGAY_DI + "' AS uuid)"),
                "Chỗ không bị trừ hai lần");
    }

    @Test
    @DisplayName("Cùng khoá nhưng thân yêu cầu KHÁC thì 409, không trả kết quả cũ")
    void cungKhoaKhacThan() {
        SeatHold giu = giuCho(2).getBody();
        UUID khoa = UUID.randomUUID();

        assertEquals(HttpStatus.CREATED, datTour(giu.getId(), khoa).getStatusCode());

        ResponseEntity<ErrorResponse> khac = goi(HttpMethod.POST, "/api/v1/dk/bookings", khoa,
                thanDatTour(giu.getId()).replace("Anders Jensen", "Người khác hẳn"),
                ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, khac.getStatusCode());
        assertEquals("IDEMPOTENCY_KEY_REUSED", khac.getBody().getCode(),
                "Trả kết quả cũ ở đây là im lặng nuốt mất một đơn thật");
    }

    @Test
    @DisplayName("Lần thử THẤT BẠI không bị khoá vĩnh viễn vào Idempotency-Key")
    void thatBaiKhongDuocNho() {
        UUID khoa = UUID.randomUUID();

        assertEquals(HttpStatus.CONFLICT,
                goi(HttpMethod.POST, "/api/v1/dk/seat-holds", khoa, thanGiuCho(3), Object.class)
                        .getStatusCode());

        jdbc.update("UPDATE departure SET seats_booked = 17 WHERE id = CAST(? AS uuid)", NGAY_DI);

        assertEquals(HttpStatus.CREATED,
                goi(HttpMethod.POST, "/api/v1/dk/seat-holds", khoa, thanGiuCho(3), Object.class)
                        .getStatusCode(),
                "Chỉ nhớ kết quả THÀNH CÔNG — chỗ có thể về kho ngay sau lần thử hỏng");
    }

    // ------------------------------------------------------------ tra đơn

    @Test
    @DisplayName("Tra đơn cần mã VÀ email khớp; sai email trả cùng kết quả với sai mã")
    void traDon() {
        Booking don = datTour(giuCho(2).getBody().getId(), UUID.randomUUID()).getBody();

        assertEquals(HttpStatus.OK, goi(HttpMethod.GET,
                "/api/v1/dk/bookings/" + don.getReference() + "?email=anders@example.dk",
                null, null, Booking.class).getStatusCode());

        assertEquals(HttpStatus.NOT_FOUND, goi(HttpMethod.GET,
                "/api/v1/dk/bookings/" + don.getReference() + "?email=nguoi.khac@example.dk",
                null, null, ErrorResponse.class).getStatusCode());

        assertEquals(HttpStatus.NOT_FOUND, goi(HttpMethod.GET,
                        "/api/v1/dk/bookings/DK-2026-KHONGCO?email=anders@example.dk",
                        null, null, ErrorResponse.class).getStatusCode(),
                "Sai mã và sai email trả cùng một kết quả — nếu không thì đây là kênh dò mã đơn");
    }

    @Test
    @DisplayName("Đường ghi không bao giờ được cache")
    void khongCache() {
        String cache = giuCho(1).getHeaders().getCacheControl();
        assertNotNull(cache);
        assertTrue(cache.contains("no-store"));
    }

    // ------------------------------------------------------------ tiện ích

    private ResponseEntity<SeatHold> giuCho(int soCho) {
        return goi(HttpMethod.POST, "/api/v1/dk/seat-holds", UUID.randomUUID(),
                thanGiuCho(soCho), SeatHold.class);
    }

    private ResponseEntity<Booking> datTour(UUID seatHoldId, UUID khoa) {
        return goi(HttpMethod.POST, "/api/v1/dk/bookings", khoa, thanDatTour(seatHoldId), Booking.class);
    }

    private static String thanGiuCho(int soCho) {
        return "{\"departureId\":\"%s\",\"seats\":%d}".formatted(NGAY_DI, soCho);
    }

    private static String thanDatTour(UUID seatHoldId) {
        String giu = seatHoldId == null ? "" : "\"seatHoldId\":\"%s\",".formatted(seatHoldId);
        return ("{\"departureId\":\"%s\",%s"
                + "\"pax\":[{\"paxTypeCode\":\"ADULT\",\"count\":2}],\"singleTravellers\":0,"
                + "\"passengers\":[{\"paxTypeCode\":\"ADULT\",\"fullName\":\"Anders Jensen\"},"
                + "{\"paxTypeCode\":\"ADULT\",\"fullName\":\"Mette Jensen\"}],"
                + "\"contactEmail\":\"anders@example.dk\",\"contactPhone\":\"+4512345678\"}")
                .formatted(NGAY_DI, giu);
    }

    private void heHan(UUID seatHoldId) {
        jdbc.update("UPDATE seat_hold SET expires_at = now() - interval '1 minute' WHERE id = ?",
                seatHoldId);
    }

    private int dem(String sql) {
        Integer so = jdbc.queryForObject(sql, Integer.class);
        return so == null ? 0 : so;
    }

    private <T> ResponseEntity<T> goi(HttpMethod phuongThuc, String duongDan,
                                      UUID khoa, String than, Class<T> kieu) {
        RestClient.RequestBodySpec yeuCau = RestClient.builder()
                .baseUrl("http://localhost:" + cong)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build()
                .method(phuongThuc)
                .uri(duongDan)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "da");

        if (khoa != null) {
            yeuCau.header("Idempotency-Key", khoa.toString());
        }
        if (than != null) {
            yeuCau.contentType(MediaType.APPLICATION_JSON).body(than);
        }
        return yeuCau.retrieve().toEntity(kieu);
    }
}
