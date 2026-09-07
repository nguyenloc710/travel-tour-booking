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
import vn.travel.booking.web.generated.model.AdminDeparture;
import vn.travel.booking.web.generated.model.AdminDepartureCopyResult;
import vn.travel.booking.web.generated.model.AdminDeparturePrice;
import vn.travel.booking.web.generated.model.AdminPriceTier;
import vn.travel.booking.web.generated.model.AdminProductDetail;
import vn.travel.booking.web.generated.model.AdminProductMarketState;
import vn.travel.booking.web.generated.model.AdminProductPage;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.ProductPage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Đợt 5b — đường <b>ghi</b> của trang quản trị: tạo và sửa sản phẩm, gán thị
 * trường, ngày khởi hành, bảng giá, thang giá.
 *
 * <p>Bài quan trọng nhất là {@link #moBanMotTourMoiTuDauDenCuoi()}: nó đi đúng
 * bảy bước của checklist mở bán (docs/22 mục 5) qua API thật, rồi kiểm bằng
 * <b>bề mặt khách</b> — tour vừa nhập phải hiện ra ở
 * {@code GET /api/v1/dk/products} kèm giá. Đó là tiêu chí ra số 7 của cổng G4,
 * và không bài test nào khác trong dự án chứng minh được nó.
 *
 * <p>Bài quan trọng thứ hai là {@link #nhanBanLichNhungKhongNhanBanGia()}: nhân
 * bản lịch mà chép cả giá sang thị trường kia là vi phạm điều 4 của
 * {@code CLAUDE.md}, và đó là loại lỗi trông rất giống một tiện ích tử tế.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdminWriteIT {

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
    private static final String DIEM_DEN = "cc000000-0000-4000-8000-0000000000f2";
    private static final LocalDate NGAY_DI = LocalDate.of(2027, 3, 14);

    @LocalServerPort
    int cong;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void chuanBiDuLieu() {
        jdbc.execute("""
                DELETE FROM booking_event;
                DELETE FROM booking_line;
                DELETE FROM booking;
                DELETE FROM seat_hold;
                DELETE FROM departure_price;
                DELETE FROM departure;
                DELETE FROM price_tier;
                DELETE FROM product_market;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product_cruise;
                DELETE FROM product_private;
                DELETE FROM product_day_tour;
                DELETE FROM product;
                DELETE FROM pax_type;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM staff_user_role;
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

                -- Thị trường VN bật lên CHO TEST. Migration R__ để nó tắt vì sáu
                -- con số nghiệp vụ của VN chưa ai chốt (Q-2), và nạp số bịa vào
                -- migration là biến phỏng đoán thành sự thật của hệ thống. Ở đây
                -- thì khác: bài test kiểm CƠ CHẾ nhân bản lịch, không kiểm con số.
                UPDATE market SET is_active = TRUE WHERE code = 'VN';

                -- pax_type cũng không có trong migration, cùng lý do.
                INSERT INTO pax_type (id, market, code, min_age, max_age, discount_rate, sort_order) VALUES
                  ('cc000000-0000-4000-8000-00000000a001','DK','ADULT',      18, NULL, 0.0000, 1),
                  ('cc000000-0000-4000-8000-00000000a002','DK','CHILD_5_11',  5,   11, 0.3000, 2),
                  ('cc000000-0000-4000-8000-00000000a003','VN','ADULT',      18, NULL, 0.0000, 1);
                """);

        String bam = new BCryptPasswordEncoder().encode(MAT_KHAU);
        themNhanVien("cc100000-0000-4000-8000-000000000001", "admin@travel.test", "Quản trị", bam, "ADMIN");
        themNhanVien("cc100000-0000-4000-8000-000000000002", "editor@travel.test", "Biên tập", bam, "EDITOR");
    }

    // ------------------------------------------------------------ vòng khép kín

    @Test
    @DisplayName("Mở bán một tour mới từ đầu đến cuối, rồi thấy nó ở bề mặt khách")
    void moBanMotTourMoiTuDauDenCuoi() {
        Phien admin = dangNhap("admin@travel.test");

        // Bước 1–4 của docs/22 mục 5: tạo, viết bản `da`, điền phần riêng của
        // loại, xuất bản bản nguồn.
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("halong-rundrejse"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);
        assertEquals(1, demDong("SELECT count(*) FROM product_group_tour WHERE product_id = ?", sp.getId()));

        // Bước 5: dịch sang `vi` và xuất bản.
        assertEquals(HttpStatus.OK, admin.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/translations/vi",
                banDich("ha-long-tron-goi", "Hạ Long trọn gói"), String.class).getStatusCode());

        // Chưa gán thị trường thì bề mặt khách KHÔNG thấy gì, dù đã dịch xong.
        // Đây là cổng chặn của docs/01 mục 4.4, và nó phải đóng ở đúng đây.
        assertEquals(0, khachThay("da").getTotalItems());

        // Bước 6: gán thị trường và bật bán.
        AdminProductMarketState gan = admin.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/markets/DK",
                "{\"published\":true}", AdminProductMarketState.class).getBody();
        assertNotNull(gan);
        assertTrue(gan.getPublished());

        // Bước 7: ngày khởi hành, rồi bảng giá.
        AdminDeparture ngay = admin.taoJson(
                "/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(ngay);
        assertEquals(LocalDate.of(2027, 3, 27), ngay.getReturnDate(),
                "returnDate suy ra từ departDate + days - 1, không nhận từ client");
        assertTrue(ngay.getPrices().isEmpty(), "ngày mới chưa có giá");

        admin.goi(HttpMethod.PUT, "/api/v1/admin/departures/" + ngay.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"},
                 {"paxTypeCode":"CHILD_5_11","occupancy":"DOUBLE","amount":"17490.00"}]
                """, String.class);

        // Và bây giờ khách thấy nó — đủ giá, đúng thị trường, đúng ngôn ngữ.
        ProductPage trang = khachThay("da");
        assertEquals(1L, trang.getTotalItems());
        assertEquals("Halong rundrejse", trang.getItems().get(0).getTitle());

        // priceFrom là giá phòng ĐÔI thấp nhất, không phải giá thấp nhất nói
        // chung: 17490 là giá trẻ em, và "giá từ" là giá cho một người lớn khi
        // hai người ở phòng đôi (docs/03). Trigger của V5 phải hiểu đúng chỗ này.
        assertEquals("24990.00", trang.getItems().get(0).getPriceFrom().getAmount());
        assertEquals("DKK", trang.getItems().get(0).getPriceFrom().getCurrency());
    }

    // ------------------------------------------------------------ tạo: luật liên trường

    @Test
    @DisplayName("Thiếu khối riêng của loại thì báo rõ thiếu khối nào")
    void thieuKhoiRiengCuaLoai() {
        ResponseEntity<ErrorResponse> loi = dangNhap("admin@travel.test").goi(
                HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s","durationDays":14,
                 "heroImage":"/img/x.jpg","source":%s}
                """.formatted(DIEM_DEN, nguon("halong", "Halong")),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals("PRODUCT_TYPE_BLOCK_MISMATCH", loi.getBody().getCode());
        // Tham số là thứ biến "sai dữ liệu" thành "sửa được ngay".
        assertEquals("groupTour", loi.getBody().getParams().get("expectedBlock"));
    }

    @Test
    @DisplayName("Gửi khối của loại khác cũng bị từ chối")
    void guiKhoiCuaLoaiKhac() {
        ResponseEntity<ErrorResponse> loi = dangNhap("admin@travel.test").goi(
                HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s","durationDays":14,
                 "heroImage":"/img/x.jpg","source":%s,
                 "cruise":{"shipName":"Bhaya","portCount":4}}
                """.formatted(DIEM_DEN, nguon("halong", "Halong")),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals("PRODUCT_TYPE_BLOCK_MISMATCH", loi.getBody().getCode());
    }

    @Test
    @DisplayName("DAY_TOUR không được có durationDays, loại khác thì bắt buộc")
    void luatSoNgayTheoLoai() {
        Phien admin = dangNhap("admin@travel.test");

        ResponseEntity<ErrorResponse> thua = admin.goi(HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"DAY_TOUR","primaryDestinationId":"%s","durationDays":1,
                 "heroImage":"/img/x.jpg","source":%s,
                 "dayTour":{"durationHours":6,"cutoffHours":24}}
                """.formatted(DIEM_DEN, nguon("dagstur", "Dagstur")),
                ErrorResponse.class);
        assertEquals("DURATION_DAYS_RULE_VIOLATED", thua.getBody().getCode());

        ResponseEntity<ErrorResponse> thieu = admin.goi(HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s",
                 "heroImage":"/img/x.jpg","source":%s,
                 "groupTour":{"minPax":12,"maxPax":20,"guaranteedThreshold":12,
                              "tourLeaderLanguage":"da","fitnessLevel":2}}
                """.formatted(DIEM_DEN, nguon("rundrejse", "Rundrejse")),
                ErrorResponse.class);
        assertEquals("DURATION_DAYS_RULE_VIOLATED", thieu.getBody().getCode());
    }

    // ------------------------------------------------------------ CSRF

    @Test
    @DisplayName("Ghi quản trị mà không kèm thẻ CSRF thì bị từ chối — MỌI đường dẫn")
    void ghiQuanTriKhongTheCsrfThiBiTuChoi() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("csrf"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);
        AdminDeparture ngay = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(ngay);

        // Cùng phiên, cùng cookie — chỉ THIẾU thẻ CSRF. Đây đúng là thứ một trang
        // khác làm được: nó lừa trình duyệt gửi cookie, nhưng không đọc được
        // cookie XSRF-TOKEN để đặt vào header.
        for (String[] loiGoi : new String[][] {
                {"PUT", "/api/v1/admin/products/" + sp.getId() + "/markets/DK", "{\"published\":true}"},
                {"PATCH", "/api/v1/admin/products/" + sp.getId(), "{\"isNew\":true}"},
                {"DELETE", "/api/v1/admin/products/" + sp.getId(), null},
                {"PATCH", "/api/v1/admin/departures/" + ngay.getId(), "{\"capacity\":22}"},
        }) {
            assertEquals(HttpStatus.FORBIDDEN,
                    admin.goiKhongCsrf(HttpMethod.valueOf(loiGoi[0]), loiGoi[1], loiGoi[2],
                            String.class).getStatusCode(),
                    loiGoi[0] + " " + loiGoi[1] + " phải bị CSRF chặn");
        }
    }

    @Test
    @DisplayName("Bề mặt công khai vẫn ghi được không cần thẻ CSRF")
    void beMatCongKhaiKhongCanCsrf() {
        // Khách không đăng nhập nên không có cookie phiên để ai lừa gửi; chống
        // gọi lại là việc của Idempotency-Key. Miễn CSRF ở đây là có chủ ý, và
        // bài test này giữ cho lần sửa CSRF không vô tình chặn luôn đường đặt tour.
        //
        // 400 chứ không phải 403: thiếu header Accept-Language nên nó dừng ở bước
        // kiểm dữ liệu vào — tức là đã ĐI QUA được bộ lọc CSRF.
        assertEquals(HttpStatus.BAD_REQUEST, new Phien()
                .goiKhongCsrf(HttpMethod.POST, "/api/v1/dk/pricing/preview", "{}", String.class)
                .getStatusCode());
    }

    // ------------------------------------------------------------ quyền

    @Test
    @DisplayName("Biên tập viên tạo được sản phẩm nhưng KHÔNG gán được thị trường")
    void bienTapVienKhongGanDuocThiTruong() {
        Phien editor = dangNhap("editor@travel.test");

        AdminProductDetail sp = editor.taoJson("/api/v1/admin/products", tourDoan("editor-tour"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        // docs/22 mục 2.1: chỉ ADMIN gán được sản phẩm vào thị trường. Đó là
        // công tắc doanh thu, không phải một trường nội dung.
        assertEquals(HttpStatus.FORBIDDEN, editor.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/markets/DK",
                "{\"published\":true}", ErrorResponse.class).getStatusCode());

        assertEquals(HttpStatus.FORBIDDEN, editor.goi(HttpMethod.DELETE,
                "/api/v1/admin/products/" + sp.getId(), null, ErrorResponse.class).getStatusCode());
    }

    // ------------------------------------------------------------ xoá mềm

    @Test
    @DisplayName("Xoá mềm lan xuống cả chùm, và trả lại slug cho tour sau")
    void xoaMemLanCaChum() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("sap-xoa"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class);

        assertEquals(HttpStatus.NO_CONTENT, admin.goi(HttpMethod.DELETE,
                "/api/v1/admin/products/" + sp.getId(), null, String.class).getStatusCode());

        // Xoá mềm KHÔNG lan như ON DELETE CASCADE — nó chỉ lan vì service tự
        // lan. Bỏ sót một bảng ở đó là để lại dữ liệu mồ côi không truy vấn nào
        // lọc ra, nên phải kiểm từng bảng một.
        assertEquals(0, demDong(
                "SELECT count(*) FROM product_translation WHERE product_id = ? AND NOT soft_delete",
                sp.getId()));
        assertEquals(0, demDong(
                "SELECT count(*) FROM departure WHERE product_id = ? AND NOT soft_delete",
                sp.getId()));

        assertEquals(HttpStatus.NOT_FOUND, admin.goi(HttpMethod.GET,
                "/api/v1/admin/products/" + sp.getId(), null, ErrorResponse.class).getStatusCode());

        // Và slug được trả lại: khoá duy nhất là index BỘ PHẬN theo soft_delete,
        // nên tour mới dùng lại được slug cũ. Không có tính chất đó thì biên tập
        // viên bị từ chối với một thông báo chẳng nói gì về nguyên nhân thật.
        assertEquals(HttpStatus.CREATED, admin.goi(HttpMethod.POST, "/api/v1/admin/products",
                tourDoan("sap-xoa"), String.class).getStatusCode());
    }

    @Test
    @DisplayName("Không xoá được sản phẩm còn đơn chưa kết thúc")
    void khongXoaDuocKhiConDonChuaKetThuc() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("co-don"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminDeparture ngay = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(ngay);

        jdbc.update("""
                INSERT INTO booking (id, reference, market, locale, product_id, departure_id,
                                     status, product_title, total, deposit, currency,
                                     contact_email, contact_phone)
                VALUES (gen_random_uuid(), 'TEST-0001', 'DK', 'da', ?, ?,
                        'CONFIRMED', 'Halong rundrejse', 24990.00, 6247.00, 'DKK',
                        'khach@example.com', '+4512345678')
                """, sp.getId(), ngay.getId());

        ResponseEntity<ErrorResponse> loi = admin.goi(HttpMethod.DELETE,
                "/api/v1/admin/products/" + sp.getId(), null, ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, loi.getStatusCode());
        assertEquals("PRODUCT_HAS_ACTIVE_BOOKINGS", loi.getBody().getCode());
    }

    // ------------------------------------------------------------ ngày khởi hành

    @Test
    @DisplayName("Không hạ sức chứa xuống dưới số chỗ đã bán")
    void khongHaSucChuaDuoiSoDaBan() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("suc-chua"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminDeparture ngay = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(ngay);
        jdbc.update("UPDATE departure SET seats_booked = 8 WHERE id = ?", ngay.getId());

        ResponseEntity<ErrorResponse> loi = admin.goi(HttpMethod.PATCH,
                "/api/v1/admin/departures/" + ngay.getId(), "{\"capacity\":5}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, loi.getStatusCode());
        assertEquals("CAPACITY_BELOW_BOOKED", loi.getBody().getCode());
        assertEquals(8, loi.getBody().getParams().get("seatsBooked"));
    }

    @Test
    @DisplayName("Hạng cabin chỉ dùng được với CRUISE")
    void cabinChiChoCruise() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("khong-cabin"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        ResponseEntity<ErrorResponse> loi = admin.goi(HttpMethod.POST,
                "/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20,
                 "cabinCategory":"BALCONY"}
                """, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals("CABIN_CATEGORY_NOT_ALLOWED", loi.getBody().getCode());
    }

    // ------------------------------------------------------------ nhân bản lịch

    @Test
    @DisplayName("Nhân bản lịch sang thị trường kia — nhưng KHÔNG nhân bản giá")
    void nhanBanLichNhungKhongNhanBanGia() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("hai-thi-truong"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        admin.goi(HttpMethod.PUT, "/api/v1/admin/products/" + sp.getId() + "/markets/DK",
                "{\"published\":true}", String.class);
        admin.goi(HttpMethod.PUT, "/api/v1/admin/products/" + sp.getId() + "/markets/VN",
                "{\"published\":true}", String.class);

        AdminDeparture dk = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(dk);
        admin.goi(HttpMethod.PUT, "/api/v1/admin/departures/" + dk.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"}]
                """, String.class);

        AdminDepartureCopyResult ket_qua = admin.goi(HttpMethod.POST,
                "/api/v1/admin/products/" + sp.getId() + "/departures/copy",
                "{\"fromMarket\":\"DK\",\"toMarket\":\"VN\"}",
                AdminDepartureCopyResult.class).getBody();

        assertNotNull(ket_qua);
        assertEquals(1, ket_qua.getCreated());
        assertEquals(0, ket_qua.getSkipped());

        // ĐÂY là điều bài test này tồn tại để canh. Chép giá sang thị trường kia
        // là vi phạm điều 4 của CLAUDE.md: tour bán cho khách Đan gồm vé bay
        // quốc tế, bán cho khách Việt thì không — hai sản phẩm khác nhau. Và nó
        // trông rất giống một tiện ích tử tế, nên nó sẽ được ai đó "sửa" một ngày.
        List<AdminDeparture> vn = List.of(admin.goi(HttpMethod.GET,
                "/api/v1/admin/products/" + sp.getId() + "/departures?market=VN",
                null, AdminDeparture[].class).getBody());
        assertEquals(1, vn.size());
        assertEquals(NGAY_DI, vn.get(0).getDepartDate(), "lịch thì chép");
        assertEquals(20, vn.get(0).getCapacity(), "sức chứa thì chép");
        assertTrue(vn.get(0).getPrices().isEmpty(), "GIÁ THÌ KHÔNG");

        // Hệ quả nhìn thấy được: thị trường VN chưa có giá từ, nên website VN
        // hiện "Liên hệ" chứ không hiện một con số chép từ Đan Mạch.
        assertNull(jdbc.queryForObject(
                "SELECT price_from FROM product_market WHERE product_id = ? AND market = 'VN'",
                BigDecimal.class, sp.getId()));
        assertEquals(0, new BigDecimal("24990.00").compareTo(jdbc.queryForObject(
                "SELECT price_from FROM product_market WHERE product_id = ? AND market = 'DK'",
                BigDecimal.class, sp.getId())));

        // Gọi lại lần hai: không tạo bản sao thứ hai, không xoá gì.
        AdminDepartureCopyResult lanHai = admin.goi(HttpMethod.POST,
                "/api/v1/admin/products/" + sp.getId() + "/departures/copy",
                "{\"fromMarket\":\"DK\",\"toMarket\":\"VN\"}",
                AdminDepartureCopyResult.class).getBody();
        assertNotNull(lanHai);
        assertEquals(0, lanHai.getCreated());
        assertEquals(1, lanHai.getSkipped());
    }

    // ------------------------------------------------------------ bảng giá

    @Test
    @DisplayName("Tiền tệ lấy từ thị trường của ngày khởi hành, không nhận từ client")
    void tienTeLayTuThiTruong() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("tien-te"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminDeparture vn = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"VN","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(vn);

        List<AdminDeparturePrice> gia = List.of(admin.goi(HttpMethod.PUT,
                "/api/v1/admin/departures/" + vn.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"18500000"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"22000000"}]
                """, AdminDeparturePrice[].class).getBody());

        assertEquals(2, gia.size());
        assertTrue(gia.stream().allMatch(g -> "VND".equals(g.getAmount().getCurrency())),
                "thị trường quyết định tiền tệ — client không có tiếng nói ở đây");
    }

    @Test
    @DisplayName("Mã loại khách không có ở thị trường này thì báo đúng mã nào")
    void maLoaiKhachLa() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("pax-la"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminDeparture vn = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"VN","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(vn);

        // CHILD_5_11 có ở DK nhưng KHÔNG có ở VN — pax_type là dữ liệu riêng
        // từng thị trường, không phải danh sách chung.
        ResponseEntity<ErrorResponse> loi = admin.goi(HttpMethod.PUT,
                "/api/v1/admin/departures/" + vn.getId() + "/prices",
                """
                [{"paxTypeCode":"CHILD_5_11","occupancy":"DOUBLE","amount":"1000000"}]
                """, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals("UNKNOWN_PAX_TYPE", loi.getBody().getCode());
        assertEquals("CHILD_5_11", loi.getBody().getParams().get("paxTypeCode"));
        assertEquals("VN", loi.getBody().getParams().get("market"));
    }

    // ------------------------------------------------------- giá phòng đơn

    @Test
    @DisplayName("Bảng giá của tour có lưu trú mà thiếu dòng phòng đơn thì bị từ chối")
    void bangGiaThieuPhongDonBiTuChoi() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("thieu-phong-don"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminDeparture ngay = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(ngay);

        // Một bảng giá đầy đủ trước đã, để câu kiểm cuối bài có thứ mà mất.
        admin.goi(HttpMethod.PUT, "/api/v1/admin/departures/" + ngay.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"}]
                """, String.class);

        // Và đây là bảng giá mà dữ liệu mồi từng có ở 36/40 ngày khởi hành. Nó
        // không làm gì hỏng cả: máy tính giá lấy `phòng đơn − phòng đôi`, không
        // thấy dòng nào thì phụ thu bằng 0, và khách đi MỘT MÌNH đặt được nguyên
        // chuyến ở giá chia đôi phòng.
        ResponseEntity<ErrorResponse> loi = admin.goi(HttpMethod.PUT,
                "/api/v1/admin/departures/" + ngay.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"23990.00"}]
                """, ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, loi.getStatusCode());
        assertNotNull(loi.getBody());
        assertEquals("SINGLE_PRICE_MISSING", loi.getBody().getCode());

        // `luuGia` thay TOÀN BỘ bảng giá — xoá rồi ghi lại. Luật phải chặn TRƯỚC
        // khi xoá, nếu không thì một lần bấm nhầm là mất sạch giá của ngày đó và
        // ngày ấy tụt xuống trạng thái tệ hơn hẳn cái mà luật vừa từ chối.
        assertEquals(2, demDong("SELECT count(*) FROM departure_price WHERE departure_id = ?",
                ngay.getId()));
        assertEquals("24990.00", jdbc.queryForObject(
                "SELECT amount::text FROM departure_price WHERE departure_id = ? "
                        + "AND occupancy = 'DOUBLE'", String.class, ngay.getId()),
                "giá cũ phải còn nguyên, không bị ghi đè một nửa");
    }

    @Test
    @DisplayName("Giá phòng đơn không cao hơn phòng đôi cũng bị từ chối — phụ thu vẫn ra 0")
    void giaPhongDonKhongCaoHonThiBiTuChoi() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("phong-don-bang-gia"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminDeparture ngay = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(ngay);

        ResponseEntity<ErrorResponse> loi = admin.goi(HttpMethod.PUT,
                "/api/v1/admin/departures/" + ngay.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"24990.00"}]
                """, ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, loi.getStatusCode());
        assertNotNull(loi.getBody());
        assertEquals("SINGLE_PRICE_MISSING", loi.getBody().getCode());
        assertEquals("ADULT", loi.getBody().getParams().get("paxTypeCode"));
    }

    @Test
    @DisplayName("Tour trong ngày KHÔNG bị đòi giá phòng đơn — nó không có đêm nào")
    void tourTrongNgayKhongBiDoiGiaPhongDon() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourTrongNgay("mot-ngay-o-hoi-an"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminDeparture ngay = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":1,"capacity":16}
                """, AdminDeparture.class).getBody();
        assertNotNull(ngay);

        ResponseEntity<AdminDeparturePrice[]> gia = admin.goi(HttpMethod.PUT,
                "/api/v1/admin/departures/" + ngay.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"645.00"}]
                """, AdminDeparturePrice[].class);

        assertEquals(HttpStatus.OK, gia.getStatusCode());
        assertNotNull(gia.getBody());
        assertEquals(1, gia.getBody().length);
    }

    @Test
    @DisplayName("Bật bán khi còn ngày khởi hành thiếu giá phòng đơn thì bị chặn")
    void batBanKhiConNgayThieuGiaPhongDon() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("bat-ban-thieu-gia"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminDeparture ngay = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(ngay);

        // Ngày mới chưa có giá nào — kể cả giá phòng đôi. Bật bán ở trạng thái
        // này là đưa lên web một ngày khởi hành mà khách đi một mình đặt được ở
        // giá chia đôi phòng.
        ResponseEntity<ErrorResponse> loi = admin.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/markets/DK",
                "{\"published\":true}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, loi.getStatusCode());
        assertNotNull(loi.getBody());
        assertEquals("SINGLE_PRICE_MISSING", loi.getBody().getCode());
        assertEquals(1, loi.getBody().getParams().get("departureCount"));
        assertEquals("2027-03-14", loi.getBody().getParams().get("firstDepartureDate"));

        // TẮT bán thì không kiểm: chặn cả đường ra là giam sản phẩm dữ liệu sai
        // ở trạng thái đang bán, tức là làm điều ngược hẳn với ý định.
        assertEquals(HttpStatus.OK, admin.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/markets/DK",
                "{\"published\":false}", String.class).getStatusCode());

        // Nhập đủ bảng giá rồi bật lại thì qua.
        admin.goi(HttpMethod.PUT, "/api/v1/admin/departures/" + ngay.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"}]
                """, String.class);

        assertEquals(HttpStatus.OK, admin.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/markets/DK",
                "{\"published\":true}", String.class).getStatusCode());
    }

    @Test
    @DisplayName("priceFrom theo giá phòng đôi rẻ nhất, và tụt theo khi giá giảm")
    void priceFromChayTheoGiaReNhat() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("gia-tu"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);
        admin.goi(HttpMethod.PUT, "/api/v1/admin/products/" + sp.getId() + "/markets/DK",
                "{\"published\":true}", String.class);

        AdminDeparture som = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        AdminDeparture muon = admin.taoJson("/api/v1/admin/products/" + sp.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-05-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(som);
        assertNotNull(muon);

        // Dòng phòng đơn ở cả hai ngày: bảng giá của sản phẩm có lưu trú bắt
        // buộc phải có (quy tắc kiểm 23). Nó cũng làm bài test này mạnh hơn —
        // 26990 là mức giá thấp hơn 29990 nhưng KHÔNG được thành "giá từ", vì
        // giá từ chỉ đọc dòng phòng đôi.
        admin.goi(HttpMethod.PUT, "/api/v1/admin/departures/" + som.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"}]
                """, String.class);
        admin.goi(HttpMethod.PUT, "/api/v1/admin/departures/" + muon.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"21990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"26990.00"}]
                """, String.class);

        assertEquals("21990.00", khachThay("da").getItems().get(0).getPriceFrom().getAmount());

        // Gỡ ngày rẻ nhất khỏi lịch: giá từ phải TĂNG lên. Không tính lại thì
        // website quảng cáo một mức giá không còn đặt được, và đó là chuyện pháp
        // lý chứ không phải chuyện hiển thị.
        jdbc.update("UPDATE departure SET soft_delete = TRUE WHERE id = ?", muon.getId());
        assertEquals("24990.00", khachThay("da").getItems().get(0).getPriceFrom().getAmount());
    }

    // ------------------------------------------------------------ thang giá

    @Test
    @DisplayName("Thang giá phải liền mạch")
    void thangGiaPhaiLienMach() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products",
                """
                {"productType":"PRIVATE_TOUR","primaryDestinationId":"%s","durationDays":10,
                 "heroImage":"/img/p.jpg","source":%s,
                 "privateTour":{"leadTimeDays":30,"quoteValidDays":14}}
                """.formatted(DIEM_DEN, nguon("privat-rejse", "Privat rejse")),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        // Hở giữa 4 và 6: nhóm đúng 5 người không có giá, và không ai phát hiện
        // cho tới khi đúng nhóm đó hỏi.
        ResponseEntity<ErrorResponse> ho = admin.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/price-tiers?market=DK",
                """
                [{"minPax":2,"maxPax":4,"pricePerPerson":"30000.00"},
                 {"minPax":6,"pricePerPerson":"25000.00"}]
                """, ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, ho.getStatusCode());
        assertEquals("PRICE_TIER_NOT_CONTIGUOUS", ho.getBody().getCode());

        List<AdminPriceTier> thang = List.of(admin.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/price-tiers?market=DK",
                """
                [{"minPax":2,"maxPax":4,"pricePerPerson":"30000.00"},
                 {"minPax":5,"pricePerPerson":"25000.00"}]
                """, AdminPriceTier[].class).getBody());

        assertEquals(2, thang.size());
        assertEquals("DKK", thang.get(0).getPricePerPerson().getCurrency());
        assertNull(thang.get(1).getMaxPax(), "bậc cuối không có trần");
    }

    @Test
    @DisplayName("Thang giá chỉ dành cho PRIVATE_TOUR")
    void thangGiaChiChoTourRieng() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("khong-bac-gia"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        ResponseEntity<ErrorResponse> loi = admin.goi(HttpMethod.PUT,
                "/api/v1/admin/products/" + sp.getId() + "/price-tiers?market=DK",
                """
                [{"minPax":2,"pricePerPerson":"30000.00"}]
                """, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals("PRODUCT_TYPE_BLOCK_MISMATCH", loi.getBody().getCode());
    }

    // ------------------------------------------------------------ sửa

    @Test
    @DisplayName("PATCH chỉ đụng trường được gửi, và sửa được phần riêng của loại")
    void patchChiDungTruongDuocGui() {
        Phien admin = dangNhap("admin@travel.test");
        AdminProductDetail sp = admin.taoJson("/api/v1/admin/products", tourDoan("sua-dan"),
                AdminProductDetail.class).getBody();
        assertNotNull(sp);

        AdminProductDetail daSua = admin.goi(HttpMethod.PATCH, "/api/v1/admin/products/" + sp.getId(),
                """
                {"isNew":true,"groupTour":{"minPax":10,"maxPax":18,"guaranteedThreshold":10,
                                           "tourLeaderLanguage":"vi","fitnessLevel":3}}
                """, AdminProductDetail.class).getBody();

        assertNotNull(daSua);
        assertTrue(daSua.getIsNew());
        assertEquals(18, daSua.getGroupTour().getMaxPax());
        assertEquals(3, daSua.getGroupTour().getFitnessLevel());
        // Trường không gửi thì giữ nguyên — không có cách xoá một giá trị qua
        // endpoint này, và đó là chủ ý.
        assertEquals("/img/hero.jpg", daSua.getHeroImage());
        assertEquals(14, daSua.getDurationDays());
    }

    @Test
    @DisplayName("Sản phẩm vừa tạo hiện ngay ở danh sách quản trị, chưa gán thị trường nào")
    void hienNgayODanhSachQuanTri() {
        Phien admin = dangNhap("admin@travel.test");
        admin.taoJson("/api/v1/admin/products", tourDoan("moi-tinh"), AdminProductDetail.class);

        AdminProductPage trang = admin.goi(HttpMethod.GET, "/api/v1/admin/products", null,
                AdminProductPage.class).getBody();

        assertNotNull(trang);
        assertEquals(1L, trang.getTotalItems());
        assertTrue(trang.getItems().get(0).getMarkets().isEmpty());
        assertEquals(1, trang.getItems().get(0).getTranslations().size(),
                "mới tạo thì chỉ có bản ngôn ngữ nguồn");
        assertTrue(trang.getItems().get(0).getTranslations().get(0).getIsSource());
    }

    // ------------------------------------------------------------ tiện ích

    private static String tourDoan(String slug) {
        return """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s","durationDays":14,
                 "heroImage":"/img/hero.jpg","source":%s,
                 "groupTour":{"minPax":12,"maxPax":20,"guaranteedThreshold":12,
                              "tourLeaderLanguage":"da","fitnessLevel":2}}
                """.formatted(DIEM_DEN, nguon(slug, "Halong rundrejse"));
    }

    /** Không có {@code durationDays} — {@code ck_product_duration} đòi đúng thế. */
    private static String tourTrongNgay(String slug) {
        return """
                {"productType":"DAY_TOUR","primaryDestinationId":"%s",
                 "heroImage":"/img/hero.jpg","source":%s,
                 "dayTour":{"durationHours":8,"cutoffHours":24}}
                """.formatted(DIEM_DEN, nguon(slug, "Hoi An paa en dag"));
    }

    private static String nguon(String slug, String tieuDe) {
        return ("{\"slug\":\"%s\",\"title\":\"%s\",\"shortDescription\":\"Hele landet.\","
                + "\"longDescription\":[\"Et.\",\"To.\"],"
                + "\"whyChooseThis\":[\"A\",\"B\",\"C\"],"
                + "\"heroImageAlt\":\"Rismarker\",\"status\":\"PUBLISHED\"}")
                .formatted(slug, tieuDe);
    }

    private static String banDich(String slug, String tieuDe) {
        return ("{\"slug\":\"%s\",\"title\":\"%s\",\"shortDescription\":\"Cả nước.\","
                + "\"longDescription\":[\"Một.\",\"Hai.\"],"
                + "\"whyChooseThis\":[\"A\",\"B\",\"C\"],"
                + "\"heroImageAlt\":\"Ruộng bậc thang\",\"status\":\"PUBLISHED\"}")
                .formatted(slug, tieuDe);
    }

    /** Bề mặt khách — không đăng nhập, không cookie. */
    private ProductPage khachThay(String locale) {
        ProductPage trang = RestClient.builder()
                .baseUrl("http://localhost:" + cong)
                .build()
                .get()
                .uri("/api/v1/dk/products")
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .body(ProductPage.class);
        assertNotNull(trang);
        return trang;
    }

    private int demDong(String sql, Object... thamSo) {
        Integer so = jdbc.queryForObject(sql, Integer.class, thamSo);
        return so == null ? 0 : so;
    }

    private void themNhanVien(String id, String email, String ten, String bam, String vaiTro) {
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash)
                VALUES (CAST(? AS uuid), ?, ?, ?)
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

    /** Giữ cookie phiên và thẻ CSRF giữa các lời gọi — đúng như trình duyệt làm. */
    private final class Phien {

        private final List<String> cookies = new ArrayList<>();

        ResponseEntity<String> dangNhap(String email, String matKhau) {
            return goi(HttpMethod.POST, "/api/v1/admin/session",
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, matKhau),
                    String.class);
        }

        <T> ResponseEntity<T> taoJson(String duongDan, String than, Class<T> kieu) {
            ResponseEntity<T> phanHoi = goi(HttpMethod.POST, duongDan, than, kieu);
            assertEquals(HttpStatus.CREATED, phanHoi.getStatusCode(), duongDan);
            return phanHoi;
        }

        /** Cố tình KHÔNG gửi thẻ CSRF — dùng để kiểm bộ lọc CSRF có chạy không. */
        <T> ResponseEntity<T> goiKhongCsrf(HttpMethod phuongThuc, String duongDan, String than,
                                           Class<T> kieu) {
            return goiCoThe(phuongThuc, duongDan, than, kieu, false);
        }

        <T> ResponseEntity<T> goi(HttpMethod phuongThuc, String duongDan, String than, Class<T> kieu) {
            return goiCoThe(phuongThuc, duongDan, than, kieu, true);
        }

        private <T> ResponseEntity<T> goiCoThe(HttpMethod phuongThuc, String duongDan, String than,
                                               Class<T> kieu, boolean kemThe) {
            RestClient.RequestBodySpec yeuCau = RestClient.builder()
                    .baseUrl("http://localhost:" + cong)
                    .defaultStatusHandler(status -> true, (req, res) -> { })
                    .build()
                    .method(phuongThuc)
                    .uri(duongDan);

            for (String c : cookies) {
                yeuCau.header(HttpHeaders.COOKIE, c);
            }
            if (kemThe) {
                thecCsrf().ifPresent(t -> yeuCau.header("X-XSRF-TOKEN", t));
            }

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
