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
import vn.travel.booking.web.generated.model.AdminBookingDetail;
import vn.travel.booking.web.generated.model.AdminBookingEvent;
import vn.travel.booking.web.generated.model.AdminBookingPage;
import vn.travel.booking.web.generated.model.AdminBookingPassenger;
import vn.travel.booking.web.generated.model.AdminBookingSummary;
import vn.travel.booking.web.generated.model.BookingStatus;
import vn.travel.booking.web.generated.model.ErrorResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vận hành đơn — danh sách (docs/22 M6) và chi tiết (M7).
 *
 * <p>Hai bài quan trọng nhất của lớp này:
 *
 * <ul>
 *   <li>{@link #totalItemsCountsBookingsNotPassengers()} — {@code totalItems} phải đếm
 *       <b>đơn</b>, không đếm hành khách. Viết truy vấn bằng {@code JOIN
 *       booking_passenger} thì bài này đỏ còn mọi bài khác vẫn xanh; đó đúng là
 *       cái bẫy đã dính một lần với thẻ và chủ đề.
 *   <li>{@link #detailShowsFullAuditLogOldestFirst()} — nhật ký hiện <b>đủ</b>, cũ nhất
 *       trước. Một nhật ký kiểm toán hiện một nửa là một nhật ký không dùng được
 *       khi có tranh chấp, và không có gì trên màn hình cho thấy nó thiếu.
 * </ul>
 *
 * <h2>Bộ dữ liệu</h2>
 *
 * <pre>
 * B1 DK-2026-AAAA11  DK · da · PENDING_CONFIRMATION · tạo 01/09 · 2 khách
 * B2 DK-2026-BBBB22  DK · vi · CONFIRMED            · tạo 02/09 · 1 khách · 3 dòng nhật ký
 * B3 VN-2026-CCCC33  VN · vi · PENDING_PAYMENT      · tạo 03/09 · 3 khách
 * B4 DK-2026-DDDD44  DK · da · COMPLETED            · tạo 01/08 · 1 khách
 * </pre>
 *
 * <p>B2 mang locale {@code vi} ở thị trường {@code DK} <b>có chủ ý</b>: khách
 * Việt sống ở Đan Mạch mua ở {@code DK} mà đọc {@code vi} (docs/02). Nhân viên
 * gọi lại cần biết nói tiếng gì, và đó không suy ra được từ thị trường.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdminBookingIT {

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
    private static final String TU_VAN = "bb100000-0000-4000-8000-000000000002";
    private static final String DEPARTURE_DK = "bb200000-0000-4000-8000-000000000001";

    @LocalServerPort
    int cong;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void prepareData() {
        // MỘT câu nhiều lệnh: ràng buộc ct_product_source_translation là
        // DEFERRABLE INITIALLY DEFERRED, nên product và bản dịch nguồn của nó
        // phải nằm cùng một transaction.
        jdbc.execute("""
                DELETE FROM staff_user_role;
                DELETE FROM booking_event;
                DELETE FROM booking_passenger;
                DELETE FROM booking_line;
                DELETE FROM seat_hold;
                DELETE FROM booking;
                DELETE FROM departure_price;
                DELETE FROM departure;
                DELETE FROM departure_origin;
                DELETE FROM pax_type;
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM slug_history;
                DELETE FROM staff_user;

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
                INSERT INTO product_market (product_id, market, is_published) VALUES
                  ('bb000000-0000-4000-8000-000000000001','DK',TRUE),
                  ('bb000000-0000-4000-8000-000000000001','VN',TRUE);

                INSERT INTO pax_type (id, market, code, min_age, max_age, discount_rate, sort_order) VALUES
                  ('bb300000-0000-4000-8000-000000000001','DK','ADULT',12,NULL,0,1),
                  ('bb300000-0000-4000-8000-000000000002','VN','ADULT',12,NULL,0,1);

                INSERT INTO departure (id, product_id, market, depart_date, return_date, days,
                                       base_status, capacity, seats_booked) VALUES
                  ('bb200000-0000-4000-8000-000000000001','bb000000-0000-4000-8000-000000000001','DK',
                   '2027-03-01','2027-03-14',14,'OPEN',20,4),
                  ('bb200000-0000-4000-8000-000000000002','bb000000-0000-4000-8000-000000000001','VN',
                   '2027-04-01','2027-04-14',14,'OPEN',20,3);

                -- created_at đặt TAY: cột này có DEFAULT now(), và một bài test
                -- lọc theo khoảng ngày mà dựa vào "hôm nay" thì sẽ đỏ vào một
                -- ngày nào đó trong tương lai mà không ai hiểu vì sao.
                INSERT INTO booking (id, reference, market, locale, product_id, departure_id, status,
                                     product_title, total, deposit, currency,
                                     contact_email, contact_phone, created_at) VALUES
                  ('bb400000-0000-4000-8000-000000000001','DK-2026-AAAA11','DK','da',
                   'bb000000-0000-4000-8000-000000000001','bb200000-0000-4000-8000-000000000001',
                   'PENDING_CONFIRMATION','Nord til syd',49980.00,12495.00,'DKK',
                   'anne@example.dk','+4520000001','2026-09-01T10:00:00Z'),
                  ('bb400000-0000-4000-8000-000000000002','DK-2026-BBBB22','DK','vi',
                   'bb000000-0000-4000-8000-000000000001','bb200000-0000-4000-8000-000000000001',
                   'CONFIRMED','Nord til syd',24990.00,6247.00,'DKK',
                   'linh@example.dk','+4520000002','2026-09-02T11:00:00Z'),
                  ('bb400000-0000-4000-8000-000000000003','VN-2026-CCCC33','VN','vi',
                   'bb000000-0000-4000-8000-000000000001','bb200000-0000-4000-8000-000000000002',
                   'PENDING_PAYMENT','Việt Nam từ bắc vào nam',45000000,0,'VND',
                   'hoa@example.vn','+84900000003','2026-09-03T12:00:00Z'),
                  ('bb400000-0000-4000-8000-000000000004','DK-2026-DDDD44','DK','da',
                   'bb000000-0000-4000-8000-000000000001','bb200000-0000-4000-8000-000000000001',
                   'COMPLETED','Nord til syd',24990.00,24990.00,'DKK',
                   'bo@example.dk','+4520000004','2026-08-01T09:00:00Z');

                -- Hai dòng cho B1: đủ để phân biệt "phân rã giá" với "một con số
                -- tổng". Dòng bằng 0 không xuất hiện (docs/14 mục 2.2).
                INSERT INTO booking_line (booking_id, seq, line_key, label_key,
                                          quantity, unit_amount, amount) VALUES
                  ('bb400000-0000-4000-8000-000000000001',1,'BASE','line.base',
                   2,22500.00,45000.00),
                  ('bb400000-0000-4000-8000-000000000001',2,'SINGLE_SUPPLEMENT','line.single',
                   1,4980.00,4980.00),
                  ('bb400000-0000-4000-8000-000000000002',1,'BASE','line.base',
                   1,24990.00,24990.00),
                  ('bb400000-0000-4000-8000-000000000003',1,'BASE','line.base',
                   3,15000000,45000000),
                  ('bb400000-0000-4000-8000-000000000004',1,'BASE','line.base',
                   1,24990.00,24990.00);

                INSERT INTO booking_passenger (booking_id, seq, pax_type_id, full_name,
                                               date_of_birth, passport_no, passport_expiry, nationality) VALUES
                  ('bb400000-0000-4000-8000-000000000001',1,'bb300000-0000-4000-8000-000000000001',
                   'Anne Sørensen','1980-04-02',NULL,NULL,'DK'),
                  ('bb400000-0000-4000-8000-000000000001',2,'bb300000-0000-4000-8000-000000000001',
                   'Ole Sørensen','1978-11-30',NULL,NULL,'DK'),
                  ('bb400000-0000-4000-8000-000000000002',1,'bb300000-0000-4000-8000-000000000001',
                   'Nguyễn Mỹ Linh','1990-06-15','P1234567','2030-01-01','VN'),
                  ('bb400000-0000-4000-8000-000000000003',1,'bb300000-0000-4000-8000-000000000002',
                   'Trần Thị Hoa','1985-02-20',NULL,NULL,'VN'),
                  ('bb400000-0000-4000-8000-000000000003',2,'bb300000-0000-4000-8000-000000000002',
                   'Trần Văn Nam','1983-07-07',NULL,NULL,'VN'),
                  ('bb400000-0000-4000-8000-000000000003',3,'bb300000-0000-4000-8000-000000000002',
                   'Trần Bảo An','2015-09-09',NULL,NULL,'VN'),
                  ('bb400000-0000-4000-8000-000000000004',1,'bb300000-0000-4000-8000-000000000001',
                   'Bo Jensen','1975-01-01',NULL,NULL,'DK');
                """);

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        addStaff("bb100000-0000-4000-8000-000000000001", "bientap@travel.test", "Biên tập", hash, "EDITOR");
        addStaff(TU_VAN, "tuvan@travel.test", "Trần Tư Vấn", hash, "CONSULTANT");
        addStaff("bb100000-0000-4000-8000-000000000003", "admin@travel.test", "Quản trị", hash, "ADMIN");

        // Nhật ký của B2 — ba dòng, và dòng cuối do NHÂN VIÊN làm. Đặt created_at
        // tay để thứ tự kiểm được; dựa vào now() thì ba dòng chèn trong cùng một
        // câu lệnh có thể trùng mốc thời gian tới từng micro giây.
        jdbc.update("""
                INSERT INTO booking_event (id, booking_id, from_status, to_status,
                                           actor_type, actor_id, note, created_at) VALUES
                  ('bb500000-0000-4000-8000-000000000001','bb400000-0000-4000-8000-000000000002',
                   NULL,'PENDING_PAYMENT','CUSTOMER',NULL,NULL,'2026-09-02T11:00:00Z'),
                  ('bb500000-0000-4000-8000-000000000002','bb400000-0000-4000-8000-000000000002',
                   'PENDING_PAYMENT','PENDING_CONFIRMATION','SYSTEM',NULL,NULL,'2026-09-02T11:05:00Z'),
                  ('bb500000-0000-4000-8000-000000000003','bb400000-0000-4000-8000-000000000002',
                   'PENDING_CONFIRMATION','CONFIRMED','STAFF',CAST(? AS uuid),
                   'Đã gọi xác nhận','2026-09-02T14:30:00Z'),
                  ('bb500000-0000-4000-8000-000000000004','bb400000-0000-4000-8000-000000000001',
                   NULL,'PENDING_PAYMENT','CUSTOMER',NULL,NULL,'2026-09-01T10:00:00Z')
                """, TU_VAN);
    }

    // ------------------------------------------------------------ M6

    @Test
    @DisplayName("Mặc định chỉ trả đơn cần xử lý, không trả tất cả")
    void defaultReturnsOnlyActionableBookings() {
        AdminBookingPage trang = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings", AdminBookingPage.class).getBody();

        assertNotNull(trang);
        assertEquals(2L, trang.getTotalItems());

        // Một danh sách vận hành mở ra mặc định "tất cả" là danh sách không dùng
        // được sau sáu tháng: việc cần làm hôm nay chìm giữa đơn đã xong.
        List<BookingStatus> status = trang.getItems().stream()
                .map(AdminBookingSummary::getStatus).toList();
        assertTrue(status.contains(BookingStatus.PENDING_CONFIRMATION));
        assertTrue(status.contains(BookingStatus.PENDING_PAYMENT));
        assertFalse(status.contains(BookingStatus.CONFIRMED));
        assertFalse(status.contains(BookingStatus.COMPLETED));
    }

    @Test
    @DisplayName("scope=ALL trả hết, mới nhất trước")
    void scopeAllReturnsEverythingNewestFirst() {
        AdminBookingPage trang = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings?scope=ALL", AdminBookingPage.class).getBody();

        assertNotNull(trang);
        assertEquals(4L, trang.getTotalItems());
        assertEquals(List.of("VN-2026-CCCC33", "DK-2026-BBBB22", "DK-2026-AAAA11", "DK-2026-DDDD44"),
                trang.getItems().stream().map(AdminBookingSummary::getReference).toList());
    }

    @Test
    @DisplayName("status tường minh thắng scope mặc định")
    void explicitStatusOverridesDefaultScope() {
        // COMPLETED không nằm trong NEEDS_ACTION. Không truyền scope, nhưng
        // truyền status: lựa chọn người dùng nhìn thấy thắng mặc định họ không
        // nhìn thấy.
        AdminBookingPage trang = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings?status=COMPLETED", AdminBookingPage.class).getBody();

        assertNotNull(trang);
        assertEquals(1L, trang.getTotalItems());
        assertEquals("DK-2026-DDDD44", trang.getItems().getFirst().getReference());
    }

    @Test
    @DisplayName("Lọc theo thị trường, và thị trường đã tắt vẫn thấy đơn cũ")
    void filterByMarketKeepsDisabledMarketHistory() {
        // VN có is_active = FALSE trong dữ liệu tra cứu. Đơn đã đặt ở đó vẫn
        // phải nhìn thấy được: tắt một thị trường là ngừng bán, không phải xoá
        // những người đã mua.
        AdminBookingPage trang = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings?scope=ALL&market=VN", AdminBookingPage.class).getBody();

        assertNotNull(trang);
        assertEquals(1L, trang.getTotalItems());
        AdminBookingSummary b3 = trang.getItems().getFirst();
        assertEquals("VN-2026-CCCC33", b3.getReference());
        assertEquals("VND", b3.getTotal().getCurrency());
    }

    @Test
    @DisplayName("Khoảng ngày lọc theo ngày TẠO đơn, và bao gồm cả ngày cuối")
    void dateRangeFiltersByCreatedAtInclusive() {
        AdminBookingPage trang = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings?scope=ALL&from=2026-09-01&to=2026-09-02",
                        AdminBookingPage.class).getBody();

        assertNotNull(trang);
        // B4 tạo 01/08 nằm ngoài; B3 tạo 03/09 nằm ngoài. B2 tạo đúng ngày cuối
        // của khoảng và PHẢI nằm trong — "tới 02/09" nghĩa là hết ngày 02/09.
        assertEquals(2L, trang.getTotalItems());
        assertEquals(List.of("DK-2026-BBBB22", "DK-2026-AAAA11"),
                trang.getItems().stream().map(AdminBookingSummary::getReference).toList());
    }

    @Test
    @DisplayName("Tìm được bằng mã tra cứu và bằng email liên hệ")
    void searchByReferenceOrContactEmail() {
        var session = login("tuvan@travel.test");

        AdminBookingPage byCode = session
                .get("/api/v1/admin/bookings?scope=ALL&q=BBBB", AdminBookingPage.class).getBody();
        assertNotNull(byCode);
        assertEquals(1L, byCode.getTotalItems());
        assertEquals("DK-2026-BBBB22", byCode.getItems().getFirst().getReference());

        // Tổng đài có trong tay một trong hai, và không biết mình đang cầm cái
        // nào cho tới khi gõ xong.
        AdminBookingPage byEmail = session
                .get("/api/v1/admin/bookings?scope=ALL&q=hoa@example.vn", AdminBookingPage.class).getBody();
        assertNotNull(byEmail);
        assertEquals(1L, byEmail.getTotalItems());
        assertEquals("VN-2026-CCCC33", byEmail.getItems().getFirst().getReference());
    }

    @Test
    @DisplayName("totalItems đếm ĐƠN, không đếm hành khách")
    void totalItemsCountsBookingsNotPassengers() {
        AdminBookingPage trang = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings?scope=ALL", AdminBookingPage.class).getBody();

        assertNotNull(trang);
        // Bốn đơn mang tổng cộng bảy hành khách. Viết truy vấn bằng
        // JOIN booking_passenger thì con số này ra 7 và không ai để ý, vì 7 vẫn
        // là một con số hợp lý.
        assertEquals(4L, trang.getTotalItems());
        assertEquals(4, trang.getItems().size());
        assertEquals(3, find(trang, "VN-2026-CCCC33").getPaxCount());
        assertEquals(2, find(trang, "DK-2026-AAAA11").getPaxCount());
    }

    @Test
    @DisplayName("Locale của đơn độc lập với thị trường")
    void bookingLocaleIndependentOfMarket() {
        AdminBookingPage trang = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings?scope=ALL", AdminBookingPage.class).getBody();

        assertNotNull(trang);
        AdminBookingSummary b2 = find(trang, "DK-2026-BBBB22");
        assertEquals(AdminBookingSummary.MarketEnum.DK, b2.getMarket());
        assertEquals("vi", b2.getLocale());
    }

    // ------------------------------------------------------------ M7

    @Test
    @DisplayName("Chi tiết trả đúng giá đã chụp lại lúc đặt")
    void detailReturnsPriceSnapshotFromBooking() {
        AdminBookingDetail booking = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings/DK-2026-AAAA11", AdminBookingDetail.class).getBody();

        assertNotNull(booking);
        assertEquals(2, booking.getBreakdown().getLines().size());
        assertEquals("49980.00", booking.getBreakdown().getTotal().getAmount());
        assertEquals("12495.00", booking.getBreakdown().getDeposit().getAmount());

        // deposit + balance = total, tuyệt đối. docs/14 mục 3 quy tắc 4.
        assertEquals(new BigDecimal("49980.00"),
                new BigDecimal(booking.getBreakdown().getDeposit().getAmount())
                        .add(new BigDecimal(booking.getBreakdown().getBalance().getAmount())));

        // Nhãn là KHOÁ CHUỖI, không phải câu tiếng người: cùng một đơn in ra
        // được ở cả hai ngôn ngữ.
        assertEquals("line.base", booking.getBreakdown().getLines().getFirst().getLabelKey());
    }

    @Test
    @DisplayName("Chi tiết hiện TOÀN BỘ nhật ký, cũ nhất trước")
    void detailShowsFullAuditLogOldestFirst() {
        AdminBookingDetail booking = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings/DK-2026-BBBB22", AdminBookingDetail.class).getBody();

        assertNotNull(booking);
        List<AdminBookingEvent> auditLog = booking.getEvents();
        assertEquals(3, auditLog.size());

        // Dòng đầu không đến từ trạng thái nào — đơn vừa sinh ra.
        assertNull(auditLog.getFirst().getFromStatus());
        assertEquals(BookingStatus.PENDING_PAYMENT, auditLog.getFirst().getToStatus());

        assertEquals(List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.PENDING_CONFIRMATION,
                        BookingStatus.CONFIRMED),
                auditLog.stream().map(AdminBookingEvent::getToStatus).toList());
    }

    @Test
    @DisplayName("Tên nhân viên chỉ có ở dòng do nhân viên làm")
    void staffNameOnlyOnStaffRows() {
        AdminBookingDetail booking = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings/DK-2026-BBBB22", AdminBookingDetail.class).getBody();

        assertNotNull(booking);
        AdminBookingEvent last = booking.getEvents().getLast();
        assertEquals(AdminBookingEvent.ActorTypeEnum.STAFF, last.getActorType());
        assertEquals("Trần Tư Vấn", last.getActorName());
        assertEquals("Đã gọi xác nhận", last.getNote());

        // Dòng của khách và của hệ thống không có nhân viên nào — và đó là phần
        // lớn nhật ký của một đơn bình thường.
        assertEquals(AdminBookingEvent.ActorTypeEnum.CUSTOMER, booking.getEvents().getFirst().getActorType());
        assertNull(booking.getEvents().getFirst().getActorName());
        assertNull(booking.getEvents().get(1).getActorName());
    }

    @Test
    @DisplayName("Chi tiết trả hộ chiếu — thứ bề mặt công khai không trả")
    void detailReturnsPassportDataPublicDoesNot() {
        AdminBookingDetail booking = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings/DK-2026-BBBB22", AdminBookingDetail.class).getBody();

        assertNotNull(booking);
        assertEquals(1, booking.getPassengers().size());
        AdminBookingPassenger client = booking.getPassengers().getFirst();
        assertEquals("Nguyễn Mỹ Linh", client.getFullName());
        assertEquals("P1234567", client.getPassportNo());
        assertEquals("ADULT", client.getPaxTypeCode());
    }

    @Test
    @DisplayName("Mã tra cứu không tồn tại trả 404")
    void unknownReferenceReturns404() {
        assertEquals(HttpStatus.NOT_FOUND, login("tuvan@travel.test")
                .get("/api/v1/admin/bookings/DK-2026-KHONGCO", String.class).getStatusCode());
    }

    // ------------------------------------------------------------ M7 đường ghi

    @Test
    @DisplayName("Xác nhận đơn: trạng thái đổi và nhật ký có dòng mang tên nhân viên")
    void confirmingBookingWritesAuditRow() {
        AdminBookingDetail sau = changeStatus("tuvan@travel.test", "DK-2026-AAAA11",
                "CONFIRMED", "Khách đã chuyển khoản");

        assertNotNull(sau);
        assertEquals(BookingStatus.CONFIRMED, sau.getStatus());

        // Dòng nhật ký là bằng chứng, không phải hiệu ứng phụ: docs/23 mục 4
        // quy tắc 1 không có ngoại lệ nào cho thao tác của nhân viên.
        AdminBookingEvent moi = sau.getEvents().getLast();
        assertEquals(BookingStatus.PENDING_CONFIRMATION, moi.getFromStatus());
        assertEquals(BookingStatus.CONFIRMED, moi.getToStatus());
        assertEquals(AdminBookingEvent.ActorTypeEnum.STAFF, moi.getActorType());
        assertEquals("Trần Tư Vấn", moi.getActorName());
        assertEquals("Khách đã chuyển khoản", moi.getNote());
    }

    @Test
    @DisplayName("Huỷ đơn trả chỗ về kho NGAY")
    void cancellingReturnsSeatsImmediately() {
        // B2 đang CONFIRMED với 1 hành khách, trên ngày khởi hành có 4 chỗ đã bán.
        assertEquals(4, seatsSold(DEPARTURE_DK));

        changeStatus("tuvan@travel.test", "DK-2026-BBBB22", "CANCELLED", "Khách đổi ý");

        // docs/14 mục 6.5: không chờ hoàn tiền xong. Giữ chỗ trống trong lúc chờ
        // ngân hàng là mất doanh thu vô ích.
        assertEquals(3, seatsSold(DEPARTURE_DK));
    }

    @Test
    @DisplayName("Hoàn tiền KHÔNG trả chỗ lần thứ hai")
    void refundDoesNotReturnSeatsTwice() {
        var session = login("tuvan@travel.test");
        callChange(session, "DK-2026-BBBB22", "CANCELLED", null);
        assertEquals(3, seatsSold(DEPARTURE_DK));

        // CANCELLED → REFUNDED: chỗ đã về kho từ bước trước. Trừ thêm lần nữa là
        // bán được nhiều hơn sức chứa, và không ai phát hiện cho tới lúc lên xe.
        callChange(session, "DK-2026-BBBB22", "REFUNDED", "Đã hoàn qua ngân hàng");
        assertEquals(3, seatsSold(DEPARTURE_DK));
    }

    @Test
    @DisplayName("Bước chuyển ngược trả 409 kèm from và to")
    void backwardTransitionReturns409() {
        // B4 đã COMPLETED — trạng thái đã chốt, không còn đường đi tiếp.
        ResponseEntity<ErrorResponse> response = callChange(
                login("tuvan@travel.test"), "DK-2026-DDDD44", "CONFIRMED", null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse loi = response.getBody();
        assertNotNull(loi);
        assertEquals("BOOKING_TRANSITION_NOT_ALLOWED", loi.getCode());
        // Tham số, không phải câu tiếng người — frontend dựng câu.
        assertEquals("COMPLETED", loi.getParams().get("from"));
        assertEquals("CONFIRMED", loi.getParams().get("to"));
    }

    @Test
    @DisplayName("Gọi lại lần hai rơi vào máy trạng thái, không cần Idempotency-Key")
    void secondCallHitsStateMachineNotIdempotency() {
        var session = login("tuvan@travel.test");
        assertEquals(HttpStatus.OK,
                callChange(session, "DK-2026-AAAA11", "CONFIRMED", null).getStatusCode());

        // Bấm hai lần, hoặc trình duyệt gửi lại: lần thứ hai PENDING_CONFIRMATION
        // không còn là trạng thái hiện tại nữa. Bản thân máy trạng thái đã là cơ
        // chế chống gọi lại ở đây.
        assertEquals(HttpStatus.CONFLICT,
                callChange(session, "DK-2026-AAAA11", "CONFIRMED", null).getStatusCode());
    }

    @Test
    @DisplayName("Nhân viên không đặt tay được EXPIRED")
    void staffCannotSetExpiredManually() {
        // EXPIRED do job quét hạn sinh ra. Đặt tay được nghĩa là nhật ký có thể
        // ghi một việc chưa từng xảy ra — spec để nó ngoài AdminBookingTargetStatus.
        assertEquals(HttpStatus.BAD_REQUEST, callChange(
                login("tuvan@travel.test"), "DK-2026-AAAA11", "EXPIRED", null)
                .getStatusCode());
    }

    @Test
    @DisplayName("Biên tập viên không đổi được trạng thái đơn")
    void editorCannotChangeBookingStatus() {
        assertEquals(HttpStatus.FORBIDDEN, callChange(
                login("bientap@travel.test"), "DK-2026-AAAA11", "CONFIRMED", null)
                .getStatusCode());
    }

    @Test
    @DisplayName("Đổi trạng thái đơn không tồn tại trả 404")
    void statusChangeOnUnknownBookingReturns404() {
        assertEquals(HttpStatus.NOT_FOUND, callChange(
                login("tuvan@travel.test"), "DK-2026-KHONGCO", "CONFIRMED", null)
                .getStatusCode());
    }

    // ------------------------------------------------------------ quyền

    @Test
    @DisplayName("Biên tập viên không xem được đơn")
    void editorCannotViewBookings() {
        // Ma trận docs/22 mục 2.1 để EDITOR ở "–" cho dòng "Đơn đặt: xem". Đơn
        // mang email, điện thoại, ngày sinh và số hộ chiếu của khách; người viết
        // nội dung không có việc gì với dữ liệu đó.
        var session = login("bientap@travel.test");
        assertEquals(HttpStatus.FORBIDDEN,
                session.get("/api/v1/admin/bookings", String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                session.get("/api/v1/admin/bookings/DK-2026-AAAA11", String.class).getStatusCode());
    }

    @Test
    @DisplayName("Quản trị viên xem được đơn")
    void adminCanViewBookings() {
        AdminBookingPage trang = login("admin@travel.test")
                .get("/api/v1/admin/bookings?scope=ALL", AdminBookingPage.class).getBody();

        assertNotNull(trang);
        assertEquals(4L, trang.getTotalItems());
    }

    @Test
    @DisplayName("Chưa đăng nhập trả 401")
    void notLoggedInReturns401() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                new Phien().get("/api/v1/admin/bookings", String.class).getStatusCode());
    }

    @Test
    @DisplayName("Phản hồi không bao giờ được cache")
    void responsesAreNeverCached() {
        ResponseEntity<AdminBookingPage> response = login("tuvan@travel.test")
                .get("/api/v1/admin/bookings", AdminBookingPage.class);

        // docs/22 mục 7: nội dung quản trị không được nằm trong bất kỳ cache nào.
        assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
    }

    // ------------------------------------------------------------ tiện ích

    /** Gọi đường ghi và trả về đơn sau khi đổi. Đỏ ngay nếu không phải 200. */
    private AdminBookingDetail changeStatus(String email, String reference,
                                            String toStatus, String note) {
        ResponseEntity<AdminBookingDetail> response = login(email)
                .call(HttpMethod.POST, "/api/v1/admin/bookings/" + reference + "/status",
                        body(toStatus, note), AdminBookingDetail.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<ErrorResponse> callChange(Phien session, String reference,
                                                 String toStatus, String note) {
        return session.call(HttpMethod.POST, "/api/v1/admin/bookings/" + reference + "/status",
                body(toStatus, note), ErrorResponse.class);
    }

    private static String body(String toStatus, String note) {
        return note == null
                ? "{\"toStatus\":\"%s\"}".formatted(toStatus)
                : "{\"toStatus\":\"%s\",\"note\":\"%s\"}".formatted(toStatus, note);
    }

    private int seatsSold(String departureId) {
        Integer count = jdbc.queryForObject(
                "SELECT seats_booked FROM departure WHERE id = CAST(? AS uuid)",
                Integer.class, departureId);
        return count == null ? -1 : count;
    }

    private static AdminBookingSummary find(AdminBookingPage trang, String reference) {
        return trang.getItems().stream()
                .filter(d -> reference.equals(d.getReference()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("không có đơn " + reference + " trong trang"));
    }

    private void addStaff(String id, String email, String name, String hash, String roles) {
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash)
                VALUES (CAST(? AS uuid), ?, ?, ?)
                """, id, email, name, hash);
        jdbc.update("""
                INSERT INTO staff_user_role (id, staff_user_id, role_code)
                VALUES (gen_random_uuid(), CAST(? AS uuid), ?)
                """, id, roles);
    }

    private Phien login(String email) {
        Phien session = new Phien();
        assertEquals(HttpStatus.NO_CONTENT, session.login(email, PASSWORD).getStatusCode());
        return session;
    }

    private final class Phien {

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
                    .baseUrl("http://localhost:" + cong)
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
            nhoCookie(response);
            return response;
        }

        private void nhoCookie(ResponseEntity<?> response) {
            List<String> moi = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (moi == null) {
                return;
            }
            for (String c : moi) {
                String summary = c.split(";", 2)[0];
                String name = summary.split("=", 2)[0];
                cookies.removeIf(cu -> cu.startsWith(name + "="));
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
