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
import vn.travel.booking.web.generated.model.ErrorCode;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.FieldError;
import vn.travel.booking.web.generated.model.FieldRule;
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
 * <p>Bài quan trọng nhất là {@link #launchNewTourEndToEnd()}: nó đi đúng
 * bảy bước của checklist mở bán (docs/22 mục 5) qua API thật, rồi kiểm bằng
 * <b>bề mặt khách</b> — tour vừa nhập phải hiện ra ở
 * {@code GET /api/v1/dk/products} kèm giá. Đó là tiêu chí ra số 7 của cổng G4,
 * và không bài test nào khác trong dự án chứng minh được nó.
 *
 * <p>Bài quan trọng thứ hai là {@link #duplicateScheduleWithoutPrices()}: nhân
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

    private static final String PASSWORD = "mat-khau-rat-dai";
    private static final String DESTINATION_ID = "cc000000-0000-4000-8000-0000000000f2";
    private static final LocalDate DEPARTURE_DATE = LocalDate.of(2027, 3, 14);

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void prepareData() {
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

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        addStaff("cc100000-0000-4000-8000-000000000001", "admin@travel.test", "Quản trị", hash, "ADMIN");
        addStaff("cc100000-0000-4000-8000-000000000002", "editor@travel.test", "Biên tập", hash, "EDITOR");
    }

    // ------------------------------------------------------------ vòng khép kín

    @Test
    @DisplayName("Mở bán một tour mới từ đầu đến cuối, rồi thấy nó ở bề mặt khách")
    void launchNewTourEndToEnd() {
        Session admin = login("admin@travel.test");

        // Bước 1–4 của docs/22 mục 5: tạo, viết bản `da`, điền phần riêng của
        // loại, xuất bản bản nguồn.
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("halong-rundrejse"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);
        assertEquals(1, countRows("SELECT count(*) FROM product_group_tour WHERE product_id = ?", product.getId()));

        // Bước 5: dịch sang `vi` và xuất bản.
        assertEquals(HttpStatus.OK, admin.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/translations/vi",
                translationBody("ha-long-tron-goi", "Hạ Long trọn gói"), String.class).getStatusCode());

        // Chưa gán thị trường thì bề mặt khách KHÔNG thấy gì, dù đã dịch xong.
        // Đây là cổng chặn của docs/01 mục 4.4, và nó phải đóng ở đúng đây.
        assertEquals(0, publicSees("da").getTotalItems());

        // Bước 6: gán thị trường và bật bán.
        AdminProductMarketState assign = admin.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/markets/DK",
                "{\"published\":true}", AdminProductMarketState.class).getBody();
        assertNotNull(assign);
        assertTrue(assign.getPublished());

        // Bước 7: ngày khởi hành, rồi bảng giá.
        AdminDeparture departure = admin.toJson(
                "/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(departure);
        assertEquals(LocalDate.of(2027, 3, 27), departure.getReturnDate(),
                "returnDate suy ra từ departDate + days - 1, không nhận từ client");
        assertTrue(departure.getPrices().isEmpty(), "ngày mới chưa có giá");

        admin.call(HttpMethod.PUT, "/api/v1/admin/departures/" + departure.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"},
                 {"paxTypeCode":"CHILD_5_11","occupancy":"DOUBLE","amount":"17490.00"}]
                """, String.class);

        // Và bây giờ khách thấy nó — đủ giá, đúng thị trường, đúng ngôn ngữ.
        ProductPage page = publicSees("da");
        assertEquals(1L, page.getTotalItems());
        assertEquals("Halong rundrejse", page.getItems().get(0).getTitle());

        // priceFrom là giá phòng ĐÔI thấp nhất, không phải giá thấp nhất nói
        // chung: 17490 là giá trẻ em, và "giá từ" là giá cho một người lớn khi
        // hai người ở phòng đôi (docs/03). Trigger của V5 phải hiểu đúng chỗ này.
        assertEquals("24990.00", page.getItems().get(0).getPriceFrom().getAmount());
        assertEquals("DKK", page.getItems().get(0).getPriceFrom().getCurrency());
    }

    // ------------------------------------------------------------ tạo: luật liên trường

    @Test
    @DisplayName("Thiếu khối riêng của loại thì báo rõ thiếu khối nào")
    void missingTypeBlockNamesTheMissingOne() {
        ResponseEntity<ErrorResponse> error = login("admin@travel.test").call(
                HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s","durationDays":14,
                 "heroImage":"/img/x.jpg","source":%s}
                """.formatted(DESTINATION_ID, source("halong", "Halong")),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(ErrorCode.PRODUCT_TYPE_BLOCK_MISMATCH, error.getBody().getCode());
        // Tham số là thứ biến "sai dữ liệu" thành "sửa được ngay".
        assertEquals("groupTour", error.getBody().getParams().get("expectedBlock"));
    }

    @Test
    @DisplayName("Gửi khối của loại khác cũng bị từ chối")
    void blockOfAnotherTypeRejected() {
        ResponseEntity<ErrorResponse> error = login("admin@travel.test").call(
                HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s","durationDays":14,
                 "heroImage":"/img/x.jpg","source":%s,
                 "cruise":{"shipName":"Bhaya","portCount":4}}
                """.formatted(DESTINATION_ID, source("halong", "Halong")),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(ErrorCode.PRODUCT_TYPE_BLOCK_MISMATCH, error.getBody().getCode());
    }

    @Test
    @DisplayName("Luật một trường trả về đường dẫn và tham số của chính luật đó")
    void validationFailedNamesEveryBadField() {
        // Hai lỗi cùng lúc, ở hai tầng lồng nhau khác nhau: một trong bản dịch
        // nguồn, một trong khối riêng của loại. Trả một lỗi rồi dừng là bắt người
        // nhập sửa từng cái một và gửi lại từng lần.
        ResponseEntity<ErrorResponse> error = login("admin@travel.test").call(
                HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s","durationDays":14,
                 "heroImage":"/img/x.jpg",
                 "source":{"slug":"halong","title":"Halong","shortDescription":"Kort.",
                           "longDescription":["Kun et afsnit."],
                           "whyChooseThis":["A","B","C"],
                           "heroImageAlt":"Rismarker","status":"DRAFT"},
                 "groupTour":{"minPax":123,"maxPax":20,"guaranteedThreshold":8,
                              "tourLeaderLanguage":"da","fitnessLevel":1}}
                """.formatted(DESTINATION_ID),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_FAILED, error.getBody().getCode());

        List<FieldError> fields = error.getBody().getFields();
        assertEquals(2, fields.size(), "cả hai trường sai đều phải có mặt");

        FieldError doan = fields.stream()
                .filter(f -> "source.longDescription".equals(f.getPath()))
                .findFirst().orElseThrow();
        assertEquals(FieldRule.SIZE, doan.getCode());
        // Con số nằm trong tham số, không nằm trong một câu tiếng người: frontend
        // dựng câu, và đổi ràng buộc ở openapi.yaml thì nó tự đúng theo.
        assertEquals(2, doan.getParams().get("min"));
        // `max` của @Size khi không đặt là Integer.MAX_VALUE — trả nó ra là đẩy
        // 2147483647 tới tận màn hình người nhập.
        assertNull(doan.getParams().get("max"));

        FieldError khach = fields.stream()
                .filter(f -> "groupTour.minPax".equals(f.getPath()))
                .findFirst().orElseThrow();
        assertEquals(FieldRule.MAX, khach.getCode());
        assertEquals(25, khach.getParams().get("max"));
    }

    @Test
    @DisplayName("Luật liên trường cũng chỉ được vào ô nhập, không thành 500")
    void crossFieldRulesPointAtOneInput() {
        // guaranteedThreshold > minPax là ràng buộc ck_pgt_guar của V1. Trước khi
        // có hàm kiểm ở service, nó nổi lên thành 500 kèm traceId — người nhập
        // liệu đọc được đúng con số đó và không gì khác.
        ResponseEntity<ErrorResponse> error = login("admin@travel.test").call(
                HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s","durationDays":14,
                 "heroImage":"/img/x.jpg","source":%s,
                 "groupTour":{"minPax":10,"maxPax":20,"guaranteedThreshold":13,
                              "tourLeaderLanguage":"da","fitnessLevel":1}}
                """.formatted(DESTINATION_ID, source("halong-guar", "Halong")),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        // Cùng mã và cùng hình dạng với luật một trường: biểu mẫu không phải biết
        // lỗi đến từ Bean Validation hay từ một hàm kiểm trong service.
        assertEquals(ErrorCode.VALIDATION_FAILED, error.getBody().getCode());

        FieldError nguong = error.getBody().getFields().getFirst();
        assertEquals("groupTour.guaranteedThreshold", nguong.getPath());
        assertEquals(FieldRule.MAX, nguong.getCode());
        // Giới hạn là minPax VỪA NHẬP, không phải hằng số trong lược đồ.
        assertEquals(10, nguong.getParams().get("max"));
    }

    @Test
    @DisplayName("Ràng buộc trên tham số truy vấn cũng nói rõ tham số nào")
    void queryParameterConstraintNamesTheParameter() {
        ResponseEntity<ErrorResponse> error = login("admin@travel.test").call(
                HttpMethod.GET, "/api/v1/admin/products?size=999", null, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_FAILED, error.getBody().getCode());

        FieldError size = error.getBody().getFields().getFirst();
        // Tên phương thức Java bị cắt khỏi đường dẫn: Hibernate Validator ghi
        // "listAdminProducts.size", mà frontend không biết và không nên biết.
        assertEquals("size", size.getPath());
        assertEquals(FieldRule.MAX, size.getCode());
        assertEquals(100, size.getParams().get("max"));
    }

    @Test
    @DisplayName("Lỗi không định vị được tới trường thì không kèm fields rỗng")
    void errorsWithoutAFieldCarryNoFieldList() {
        // Kiểm trên CHUỖI JSON thô, không qua ErrorResponse: lớp sinh ra khởi tạo
        // `fields` bằng danh sách rỗng, nên sau khi giải mã thì "vắng mặt" và
        // "rỗng" trông giống hệt nhau — đúng cái mà bài này cần phân biệt.
        ResponseEntity<String> error = login("admin@travel.test").call(
                HttpMethod.GET, "/api/v1/admin/products/" + UUID.randomUUID(), null,
                String.class);

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertFalse(error.getBody().contains("fields"), "trường rỗng bỏ hẳn khỏi JSON — docs/13 mục 4");
        assertFalse(error.getBody().contains("params"), error.getBody());
    }

    @Test
    @DisplayName("DAY_TOUR không được có durationDays, loại khác thì bắt buộc")
    void durationDaysRuleByProductType() {
        Session admin = login("admin@travel.test");

        ResponseEntity<ErrorResponse> extra = admin.call(HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"DAY_TOUR","primaryDestinationId":"%s","durationDays":1,
                 "heroImage":"/img/x.jpg","source":%s,
                 "dayTour":{"durationHours":6,"cutoffHours":24}}
                """.formatted(DESTINATION_ID, source("dagstur", "Dagstur")),
                ErrorResponse.class);
        assertEquals(ErrorCode.DURATION_DAYS_RULE_VIOLATED, extra.getBody().getCode());

        ResponseEntity<ErrorResponse> missing = admin.call(HttpMethod.POST, "/api/v1/admin/products",
                """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s",
                 "heroImage":"/img/x.jpg","source":%s,
                 "groupTour":{"minPax":12,"maxPax":20,"guaranteedThreshold":12,
                              "tourLeaderLanguage":"da","fitnessLevel":2}}
                """.formatted(DESTINATION_ID, source("rundrejse", "Rundrejse")),
                ErrorResponse.class);
        assertEquals(ErrorCode.DURATION_DAYS_RULE_VIOLATED, missing.getBody().getCode());
    }

    // ------------------------------------------------------------ CSRF

    @Test
    @DisplayName("Ghi quản trị mà không kèm thẻ CSRF thì bị từ chối — MỌI đường dẫn")
    void adminWriteWithoutCsrfRejectedEverywhere() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("csrf"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);
        AdminDeparture departure = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(departure);

        // Cùng phiên, cùng cookie — chỉ THIẾU thẻ CSRF. Đây đúng là thứ một trang
        // khác làm được: nó lừa trình duyệt gửi cookie, nhưng không đọc được
        // cookie XSRF-TOKEN để đặt vào header.
        for (String[] callSpec : new String[][] {
                {"PUT", "/api/v1/admin/products/" + product.getId() + "/markets/DK", "{\"published\":true}"},
                {"PATCH", "/api/v1/admin/products/" + product.getId(), "{\"isNew\":true}"},
                {"DELETE", "/api/v1/admin/products/" + product.getId(), null},
                {"PATCH", "/api/v1/admin/departures/" + departure.getId(), "{\"capacity\":22}"},
        }) {
            assertEquals(HttpStatus.FORBIDDEN,
                    admin.callWithoutCsrf(HttpMethod.valueOf(callSpec[0]), callSpec[1], callSpec[2],
                            String.class).getStatusCode(),
                    callSpec[0] + " " + callSpec[1] + " phải bị CSRF chặn");
        }
    }

    @Test
    @DisplayName("Bề mặt công khai vẫn ghi được không cần thẻ CSRF")
    void publicSurfaceWritesNeedNoCsrf() {
        // Khách không đăng nhập nên không có cookie phiên để ai lừa gửi; chống
        // gọi lại là việc của Idempotency-Key. Miễn CSRF ở đây là có chủ ý, và
        // bài test này giữ cho lần sửa CSRF không vô tình chặn luôn đường đặt tour.
        //
        // 400 chứ không phải 403: thiếu header Accept-Language nên nó dừng ở bước
        // kiểm dữ liệu vào — tức là đã ĐI QUA được bộ lọc CSRF.
        assertEquals(HttpStatus.BAD_REQUEST, new Session()
                .callWithoutCsrf(HttpMethod.POST, "/api/v1/dk/pricing/preview", "{}", String.class)
                .getStatusCode());
    }

    // ------------------------------------------------------------ quyền

    @Test
    @DisplayName("Biên tập viên tạo được sản phẩm nhưng KHÔNG gán được thị trường")
    void editorCanCreateProductButNotAssignMarket() {
        Session editor = login("editor@travel.test");

        AdminProductDetail product = editor.toJson("/api/v1/admin/products", groupTour("editor-tour"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        // docs/22 mục 2.1: chỉ ADMIN gán được sản phẩm vào thị trường. Đó là
        // công tắc doanh thu, không phải một trường nội dung.
        assertEquals(HttpStatus.FORBIDDEN, editor.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/markets/DK",
                "{\"published\":true}", ErrorResponse.class).getStatusCode());

        assertEquals(HttpStatus.FORBIDDEN, editor.call(HttpMethod.DELETE,
                "/api/v1/admin/products/" + product.getId(), null, ErrorResponse.class).getStatusCode());
    }

    // ------------------------------------------------------------ xoá mềm

    @Test
    @DisplayName("Xoá mềm lan xuống cả chùm, và trả lại slug cho tour sau")
    void softDeleteCascadesAndFreesSlug() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("sap-xoa"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class);

        assertEquals(HttpStatus.NO_CONTENT, admin.call(HttpMethod.DELETE,
                "/api/v1/admin/products/" + product.getId(), null, String.class).getStatusCode());

        // Xoá mềm KHÔNG lan như ON DELETE CASCADE — nó chỉ lan vì service tự
        // lan. Bỏ sót một bảng ở đó là để lại dữ liệu mồ côi không truy vấn nào
        // lọc ra, nên phải kiểm từng bảng một.
        assertEquals(0, countRows(
                "SELECT count(*) FROM product_translation WHERE product_id = ? AND NOT soft_delete",
                product.getId()));
        assertEquals(0, countRows(
                "SELECT count(*) FROM departure WHERE product_id = ? AND NOT soft_delete",
                product.getId()));

        assertEquals(HttpStatus.NOT_FOUND, admin.call(HttpMethod.GET,
                "/api/v1/admin/products/" + product.getId(), null, ErrorResponse.class).getStatusCode());

        // Và slug được trả lại: khoá duy nhất là index BỘ PHẬN theo soft_delete,
        // nên tour mới dùng lại được slug cũ. Không có tính chất đó thì biên tập
        // viên bị từ chối với một thông báo chẳng nói gì về nguyên nhân thật.
        assertEquals(HttpStatus.CREATED, admin.call(HttpMethod.POST, "/api/v1/admin/products",
                groupTour("sap-xoa"), String.class).getStatusCode());
    }

    @Test
    @DisplayName("Không xoá được sản phẩm còn đơn chưa kết thúc")
    void cannotDeleteWithOpenBookings() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("co-booking"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminDeparture departure = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(departure);

        jdbc.update("""
                INSERT INTO booking (id, reference, market, locale, product_id, departure_id,
                                     status, product_title, total, deposit, currency,
                                     contact_email, contact_phone)
                VALUES (gen_random_uuid(), 'TEST-0001', 'DK', 'da', ?, ?,
                        'CONFIRMED', 'Halong rundrejse', 24990.00, 6247.00, 'DKK',
                        'client@example.com', '+4512345678')
                """, product.getId(), departure.getId());

        ResponseEntity<ErrorResponse> error = admin.call(HttpMethod.DELETE,
                "/api/v1/admin/products/" + product.getId(), null, ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(ErrorCode.PRODUCT_HAS_ACTIVE_BOOKINGS, error.getBody().getCode());
    }

    // ------------------------------------------------------------ ngày khởi hành

    @Test
    @DisplayName("Không hạ sức chứa xuống dưới số chỗ đã bán")
    void cannotLowerCapacityBelowSeatsSold() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("suc-fits"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminDeparture departure = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(departure);
        jdbc.update("UPDATE departure SET seats_booked = 8 WHERE id = ?", departure.getId());

        ResponseEntity<ErrorResponse> error = admin.call(HttpMethod.PATCH,
                "/api/v1/admin/departures/" + departure.getId(), "{\"capacity\":5}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(ErrorCode.CAPACITY_BELOW_BOOKED, error.getBody().getCode());
        assertEquals(8, error.getBody().getParams().get("seatsBooked"));
    }

    @Test
    @DisplayName("Hạng cabin chỉ dùng được với CRUISE")
    void cabinTiersOnlyForCruise() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("khong-cabin"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        ResponseEntity<ErrorResponse> error = admin.call(HttpMethod.POST,
                "/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20,
                 "cabinCategory":"BALCONY"}
                """, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(ErrorCode.CABIN_CATEGORY_NOT_ALLOWED, error.getBody().getCode());
    }

    // ------------------------------------------------------------ nhân bản lịch

    @Test
    @DisplayName("Nhân bản lịch sang thị trường kia — nhưng KHÔNG nhân bản giá")
    void duplicateScheduleWithoutPrices() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("hai-thi-truong"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        admin.call(HttpMethod.PUT, "/api/v1/admin/products/" + product.getId() + "/markets/DK",
                "{\"published\":true}", String.class);
        admin.call(HttpMethod.PUT, "/api/v1/admin/products/" + product.getId() + "/markets/VN",
                "{\"published\":true}", String.class);

        AdminDeparture dk = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(dk);
        admin.call(HttpMethod.PUT, "/api/v1/admin/departures/" + dk.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"}]
                """, String.class);

        AdminDepartureCopyResult result = admin.call(HttpMethod.POST,
                "/api/v1/admin/products/" + product.getId() + "/departures/copy",
                "{\"fromMarket\":\"DK\",\"toMarket\":\"VN\"}",
                AdminDepartureCopyResult.class).getBody();

        assertNotNull(result);
        assertEquals(1, result.getCreated());
        assertEquals(0, result.getSkipped());

        // ĐÂY là điều bài test này tồn tại để canh. Chép giá sang thị trường kia
        // là vi phạm điều 4 của CLAUDE.md: tour bán cho khách Đan gồm vé bay
        // quốc tế, bán cho khách Việt thì không — hai sản phẩm khác nhau. Và nó
        // trông rất giống một tiện ích tử tế, nên nó sẽ được ai đó "sửa" một ngày.
        List<AdminDeparture> vn = List.of(admin.call(HttpMethod.GET,
                "/api/v1/admin/products/" + product.getId() + "/departures?market=VN",
                null, AdminDeparture[].class).getBody());
        assertEquals(1, vn.size());
        assertEquals(DEPARTURE_DATE, vn.get(0).getDepartDate(), "lịch thì chép");
        assertEquals(20, vn.get(0).getCapacity(), "sức chứa thì chép");
        assertTrue(vn.get(0).getPrices().isEmpty(), "GIÁ THÌ KHÔNG");

        // Hệ quả nhìn thấy được: thị trường VN chưa có giá từ, nên website VN
        // hiện "Liên hệ" chứ không hiện một con số chép từ Đan Mạch.
        assertNull(jdbc.queryForObject(
                "SELECT price_from FROM product_market WHERE product_id = ? AND market = 'VN'",
                BigDecimal.class, product.getId()));
        assertEquals(0, new BigDecimal("24990.00").compareTo(jdbc.queryForObject(
                "SELECT price_from FROM product_market WHERE product_id = ? AND market = 'DK'",
                BigDecimal.class, product.getId())));

        // Gọi lại lần hai: không tạo bản sao thứ hai, không xoá gì.
        AdminDepartureCopyResult secondAttempt = admin.call(HttpMethod.POST,
                "/api/v1/admin/products/" + product.getId() + "/departures/copy",
                "{\"fromMarket\":\"DK\",\"toMarket\":\"VN\"}",
                AdminDepartureCopyResult.class).getBody();
        assertNotNull(secondAttempt);
        assertEquals(0, secondAttempt.getCreated());
        assertEquals(1, secondAttempt.getSkipped());
    }

    // ------------------------------------------------------------ bảng giá

    @Test
    @DisplayName("Tiền tệ lấy từ thị trường của ngày khởi hành, không nhận từ client")
    void currencyComesFromDepartureMarket() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("tien-te"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminDeparture vn = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"VN","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(vn);

        List<AdminDeparturePrice> price = List.of(admin.call(HttpMethod.PUT,
                "/api/v1/admin/departures/" + vn.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"18500000"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"22000000"}]
                """, AdminDeparturePrice[].class).getBody());

        assertEquals(2, price.size());
        assertTrue(price.stream().allMatch(g -> "VND".equals(g.getAmount().getCurrency())),
                "thị trường quyết định tiền tệ — client không có tiếng nói ở đây");
    }

    @Test
    @DisplayName("Mã loại khách không có ở thị trường này thì báo đúng mã nào")
    void unknownPaxTypeCodeIsNamed() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("pax-la"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminDeparture vn = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"VN","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(vn);

        // CHILD_5_11 có ở DK nhưng KHÔNG có ở VN — pax_type là dữ liệu riêng
        // từng thị trường, không phải danh sách chung.
        ResponseEntity<ErrorResponse> error = admin.call(HttpMethod.PUT,
                "/api/v1/admin/departures/" + vn.getId() + "/prices",
                """
                [{"paxTypeCode":"CHILD_5_11","occupancy":"DOUBLE","amount":"1000000"}]
                """, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(ErrorCode.UNKNOWN_PAX_TYPE, error.getBody().getCode());
        assertEquals("CHILD_5_11", error.getBody().getParams().get("paxTypeCode"));
        assertEquals("VN", error.getBody().getParams().get("market"));
    }

    // ------------------------------------------------------- giá phòng đơn

    @Test
    @DisplayName("Bảng giá của tour có lưu trú mà thiếu dòng phòng đơn thì bị từ chối")
    void priceTierMissingSingleRoomRejected() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("thieu-phong-booking"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminDeparture departure = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(departure);

        // Một bảng giá đầy đủ trước đã, để câu kiểm cuối bài có thứ mà mất.
        admin.call(HttpMethod.PUT, "/api/v1/admin/departures/" + departure.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"}]
                """, String.class);

        // Và đây là bảng giá mà dữ liệu mồi từng có ở 36/40 ngày khởi hành. Nó
        // không làm gì hỏng cả: máy tính giá lấy `phòng đơn − phòng đôi`, không
        // thấy dòng nào thì phụ thu bằng 0, và khách đi MỘT MÌNH đặt được nguyên
        // chuyến ở giá chia đôi phòng.
        ResponseEntity<ErrorResponse> error = admin.call(HttpMethod.PUT,
                "/api/v1/admin/departures/" + departure.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"23990.00"}]
                """, ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertNotNull(error.getBody());
        assertEquals(ErrorCode.SINGLE_PRICE_MISSING, error.getBody().getCode());

        // `savePrices` thay TOÀN BỘ bảng giá — xoá rồi ghi lại. Luật phải chặn TRƯỚC
        // khi xoá, nếu không thì một lần bấm nhầm là mất sạch giá của ngày đó và
        // ngày ấy tụt xuống trạng thái tệ hơn hẳn cái mà luật vừa từ chối.
        assertEquals(2, countRows("SELECT count(*) FROM departure_price WHERE departure_id = ?",
                departure.getId()));
        assertEquals("24990.00", jdbc.queryForObject(
                "SELECT amount::text FROM departure_price WHERE departure_id = ? "
                        + "AND occupancy = 'DOUBLE'", String.class, departure.getId()),
                "giá cũ phải còn nguyên, không bị ghi đè một nửa");
    }

    @Test
    @DisplayName("Giá phòng đơn không cao hơn phòng đôi cũng bị từ chối — phụ thu vẫn ra 0")
    void singleRoomPriceNotHigherRejected() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("phong-booking-table-price"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminDeparture departure = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(departure);

        ResponseEntity<ErrorResponse> error = admin.call(HttpMethod.PUT,
                "/api/v1/admin/departures/" + departure.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"24990.00"}]
                """, ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertNotNull(error.getBody());
        assertEquals(ErrorCode.SINGLE_PRICE_MISSING, error.getBody().getCode());
        assertEquals("ADULT", error.getBody().getParams().get("paxTypeCode"));
    }

    @Test
    @DisplayName("Tour trong ngày KHÔNG bị đòi giá phòng đơn — nó không có đêm nào")
    void dayTourDoesNotRequireSingleRoomPrice() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", dayTour("mot-ngay-o-hoi-an"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminDeparture departure = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":1,"capacity":16}
                """, AdminDeparture.class).getBody();
        assertNotNull(departure);

        ResponseEntity<AdminDeparturePrice[]> price = admin.call(HttpMethod.PUT,
                "/api/v1/admin/departures/" + departure.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"645.00"}]
                """, AdminDeparturePrice[].class);

        assertEquals(HttpStatus.OK, price.getStatusCode());
        assertNotNull(price.getBody());
        assertEquals(1, price.getBody().length);
    }

    @Test
    @DisplayName("Bật bán khi còn ngày khởi hành thiếu giá phòng đơn thì bị chặn")
    void publishBlockedWhenDepartureLacksSingleRoomPrice() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("bat-ban-thieu-price"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminDeparture departure = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(departure);

        // Ngày mới chưa có giá nào — kể cả giá phòng đôi. Bật bán ở trạng thái
        // này là đưa lên web một ngày khởi hành mà khách đi một mình đặt được ở
        // giá chia đôi phòng.
        ResponseEntity<ErrorResponse> error = admin.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/markets/DK",
                "{\"published\":true}", ErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertNotNull(error.getBody());
        assertEquals(ErrorCode.SINGLE_PRICE_MISSING, error.getBody().getCode());
        assertEquals(1, error.getBody().getParams().get("departureCount"));
        assertEquals("2027-03-14", error.getBody().getParams().get("firstDepartureDate"));

        // TẮT bán thì không kiểm: chặn cả đường ra là giam sản phẩm dữ liệu sai
        // ở trạng thái đang bán, tức là làm điều ngược hẳn với ý định.
        assertEquals(HttpStatus.OK, admin.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/markets/DK",
                "{\"published\":false}", String.class).getStatusCode());

        // Nhập đủ bảng giá rồi bật lại thì qua.
        admin.call(HttpMethod.PUT, "/api/v1/admin/departures/" + departure.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"}]
                """, String.class);

        assertEquals(HttpStatus.OK, admin.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/markets/DK",
                "{\"published\":true}", String.class).getStatusCode());
    }

    @Test
    @DisplayName("priceFrom theo giá phòng đôi rẻ nhất, và tụt theo khi giá giảm")
    void priceFromTracksCheapestDoubleRoom() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("price-tu"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);
        admin.call(HttpMethod.PUT, "/api/v1/admin/products/" + product.getId() + "/markets/DK",
                "{\"published\":true}", String.class);

        AdminDeparture early = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-03-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        AdminDeparture late = admin.toJson("/api/v1/admin/products/" + product.getId() + "/departures",
                """
                {"market":"DK","departDate":"2027-05-14","days":14,"capacity":20}
                """, AdminDeparture.class).getBody();
        assertNotNull(early);
        assertNotNull(late);

        // Dòng phòng đơn ở cả hai ngày: bảng giá của sản phẩm có lưu trú bắt
        // buộc phải có (quy tắc kiểm 23). Nó cũng làm bài test này mạnh hơn —
        // 26990 là mức giá thấp hơn 29990 nhưng KHÔNG được thành "giá từ", vì
        // giá từ chỉ đọc dòng phòng đôi.
        admin.call(HttpMethod.PUT, "/api/v1/admin/departures/" + early.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"24990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"29990.00"}]
                """, String.class);
        admin.call(HttpMethod.PUT, "/api/v1/admin/departures/" + late.getId() + "/prices",
                """
                [{"paxTypeCode":"ADULT","occupancy":"DOUBLE","amount":"21990.00"},
                 {"paxTypeCode":"ADULT","occupancy":"SINGLE","amount":"26990.00"}]
                """, String.class);

        assertEquals("21990.00", publicSees("da").getItems().get(0).getPriceFrom().getAmount());

        // Gỡ ngày rẻ nhất khỏi lịch: giá từ phải TĂNG lên. Không tính lại thì
        // website quảng cáo một mức giá không còn đặt được, và đó là chuyện pháp
        // lý chứ không phải chuyện hiển thị.
        jdbc.update("UPDATE departure SET soft_delete = TRUE WHERE id = ?", late.getId());
        assertEquals("24990.00", publicSees("da").getItems().get(0).getPriceFrom().getAmount());
    }

    // ------------------------------------------------------------ thang giá

    @Test
    @DisplayName("Thang giá phải liền mạch")
    void priceTiersMustBeContiguous() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products",
                """
                {"productType":"PRIVATE_TOUR","primaryDestinationId":"%s","durationDays":10,
                 "heroImage":"/img/p.jpg","source":%s,
                 "privateTour":{"leadTimeDays":30,"quoteValidDays":14}}
                """.formatted(DESTINATION_ID, source("privat-rejse", "Privat rejse")),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        // Hở giữa 4 và 6: nhóm đúng 5 người không có giá, và không ai phát hiện
        // cho tới khi đúng nhóm đó hỏi.
        ResponseEntity<ErrorResponse> gapError = admin.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/price-tiers?market=DK",
                """
                [{"minPax":2,"maxPax":4,"pricePerPerson":"30000.00"},
                 {"minPax":6,"pricePerPerson":"25000.00"}]
                """, ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, gapError.getStatusCode());
        assertEquals(ErrorCode.PRICE_TIER_NOT_CONTIGUOUS, gapError.getBody().getCode());

        List<AdminPriceTier> tiers = List.of(admin.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/price-tiers?market=DK",
                """
                [{"minPax":2,"maxPax":4,"pricePerPerson":"30000.00"},
                 {"minPax":5,"pricePerPerson":"25000.00"}]
                """, AdminPriceTier[].class).getBody());

        assertEquals(2, tiers.size());
        assertEquals("DKK", tiers.get(0).getPricePerPerson().getCurrency());
        assertNull(tiers.get(1).getMaxPax(), "bậc cuối không có trần");
    }

    @Test
    @DisplayName("Thang giá chỉ dành cho PRIVATE_TOUR")
    void priceTiersOnlyForPrivateTour() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("khong-bac-price"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        ResponseEntity<ErrorResponse> error = admin.call(HttpMethod.PUT,
                "/api/v1/admin/products/" + product.getId() + "/price-tiers?market=DK",
                """
                [{"minPax":2,"pricePerPerson":"30000.00"}]
                """, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(ErrorCode.PRODUCT_TYPE_BLOCK_MISMATCH, error.getBody().getCode());
    }

    // ------------------------------------------------------------ sửa

    @Test
    @DisplayName("PATCH chỉ đụng trường được gửi, và sửa được phần riêng của loại")
    void patchTouchesOnlySentFields() {
        Session admin = login("admin@travel.test");
        AdminProductDetail product = admin.toJson("/api/v1/admin/products", groupTour("sua-dan"),
                AdminProductDetail.class).getBody();
        assertNotNull(product);

        AdminProductDetail edited = admin.call(HttpMethod.PATCH, "/api/v1/admin/products/" + product.getId(),
                """
                {"isNew":true,"groupTour":{"minPax":10,"maxPax":18,"guaranteedThreshold":10,
                                           "tourLeaderLanguage":"vi","fitnessLevel":3}}
                """, AdminProductDetail.class).getBody();

        assertNotNull(edited);
        assertTrue(edited.getIsNew());
        assertEquals(18, edited.getGroupTour().getMaxPax());
        assertEquals(3, edited.getGroupTour().getFitnessLevel());
        // Trường không gửi thì giữ nguyên — không có cách xoá một giá trị qua
        // endpoint này, và đó là chủ ý.
        assertEquals("/img/hero.jpg", edited.getHeroImage());
        assertEquals(14, edited.getDurationDays());
    }

    @Test
    @DisplayName("Sản phẩm vừa tạo hiện ngay ở danh sách quản trị, chưa gán thị trường nào")
    void newProductAppearsInAdminListUnassigned() {
        Session admin = login("admin@travel.test");
        admin.toJson("/api/v1/admin/products", groupTour("moi-tinh"), AdminProductDetail.class);

        AdminProductPage page = admin.call(HttpMethod.GET, "/api/v1/admin/products", null,
                AdminProductPage.class).getBody();

        assertNotNull(page);
        assertEquals(1L, page.getTotalItems());
        assertTrue(page.getItems().get(0).getMarkets().isEmpty());
        assertEquals(1, page.getItems().get(0).getTranslations().size(),
                "mới tạo thì chỉ có bản ngôn ngữ nguồn");
        assertTrue(page.getItems().get(0).getTranslations().get(0).getIsSource());
    }

    // ------------------------------------------------------------ tiện ích

    private static String groupTour(String slug) {
        return """
                {"productType":"GROUP_TOUR","primaryDestinationId":"%s","durationDays":14,
                 "heroImage":"/img/hero.jpg","source":%s,
                 "groupTour":{"minPax":12,"maxPax":20,"guaranteedThreshold":12,
                              "tourLeaderLanguage":"da","fitnessLevel":2}}
                """.formatted(DESTINATION_ID, source(slug, "Halong rundrejse"));
    }

    /** Không có {@code durationDays} — {@code ck_product_duration} đòi đúng thế. */
    private static String dayTour(String slug) {
        return """
                {"productType":"DAY_TOUR","primaryDestinationId":"%s",
                 "heroImage":"/img/hero.jpg","source":%s,
                 "dayTour":{"durationHours":8,"cutoffHours":24}}
                """.formatted(DESTINATION_ID, source(slug, "Hoi An paa en dag"));
    }

    private static String source(String slug, String title) {
        return ("{\"slug\":\"%s\",\"title\":\"%s\",\"shortDescription\":\"Hele landet.\","
                + "\"longDescription\":[\"Et.\",\"To.\"],"
                + "\"whyChooseThis\":[\"A\",\"B\",\"C\"],"
                + "\"heroImageAlt\":\"Rismarker\",\"status\":\"PUBLISHED\"}")
                .formatted(slug, title);
    }

    private static String translationBody(String slug, String title) {
        return ("{\"slug\":\"%s\",\"title\":\"%s\",\"shortDescription\":\"Cả nước.\","
                + "\"longDescription\":[\"Một.\",\"Hai.\"],"
                + "\"whyChooseThis\":[\"A\",\"B\",\"C\"],"
                + "\"heroImageAlt\":\"Ruộng bậc thang\",\"status\":\"PUBLISHED\"}")
                .formatted(slug, title);
    }

    /** Bề mặt khách — không đăng nhập, không cookie. */
    private ProductPage publicSees(String locale) {
        ProductPage page = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build()
                .get()
                .uri("/api/v1/dk/products")
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .body(ProductPage.class);
        assertNotNull(page);
        return page;
    }

    private int countRows(String sql, Object... params) {
        Integer count = jdbc.queryForObject(sql, Integer.class, params);
        return count == null ? 0 : count;
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

    private Session login(String email) {
        Session session = new Session();
        assertEquals(HttpStatus.NO_CONTENT, session.login(email, PASSWORD).getStatusCode());
        return session;
    }

    /** Giữ cookie phiên và thẻ CSRF giữa các lời gọi — đúng như trình duyệt làm. */
    private final class Session {

        private final List<String> cookies = new ArrayList<>();

        ResponseEntity<String> login(String email, String password) {
            return call(HttpMethod.POST, "/api/v1/admin/session",
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password),
                    String.class);
        }

        <T> ResponseEntity<T> toJson(String path, String body, Class<T> type) {
            ResponseEntity<T> response = call(HttpMethod.POST, path, body, type);
            assertEquals(HttpStatus.CREATED, response.getStatusCode(), path);
            return response;
        }

        /** Cố tình KHÔNG gửi thẻ CSRF — dùng để kiểm bộ lọc CSRF có chạy không. */
        <T> ResponseEntity<T> callWithoutCsrf(HttpMethod httpMethod, String path, String body,
                                           Class<T> type) {
            return callWithTags(httpMethod, path, body, type, false);
        }

        <T> ResponseEntity<T> call(HttpMethod httpMethod, String path, String body, Class<T> type) {
            return callWithTags(httpMethod, path, body, type, true);
        }

        private <T> ResponseEntity<T> callWithTags(HttpMethod httpMethod, String path, String body,
                                               Class<T> type, boolean withTags) {
            RestClient.RequestBodySpec request = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultStatusHandler(status -> true, (req, res) -> { })
                    .build()
                    .method(httpMethod)
                    .uri(path);

            for (String c : cookies) {
                request.header(HttpHeaders.COOKIE, c);
            }
            if (withTags) {
                csrfToken().ifPresent(t -> request.header("X-XSRF-TOKEN", t));
            }

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
