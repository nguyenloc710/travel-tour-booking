package vn.travel.booking.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import vn.travel.booking.web.generated.model.AdminContentLocaleState;
import vn.travel.booking.web.generated.model.AdminDestinationDetail;
import vn.travel.booking.web.generated.model.AdminLectureDetail;
import vn.travel.booking.web.generated.model.AdminLecturePage;
import vn.travel.booking.web.generated.model.AdminPostDetail;
import vn.travel.booking.web.generated.model.AdminPostPage;
import vn.travel.booking.web.generated.model.AdminStaffUser;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.PostPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nội dung khác (docs/22 M13) và người dùng (M14).
 *
 * <p>Bốn bài đáng chú ý nhất:
 *
 * <ul>
 *   <li>{@code translatorCannotEditSourceLocale} — luật lệch nhau giữa hai vai trò
 *       là thứ duy nhất giữ cho bản nguồn và bản dịch không bị hai người sửa
 *       theo hai hướng.
 *   <li>{@code deletingDestinationWithProductsReturns409} — cứ cho xoá thì không có gì nổ,
 *       sản phẩm chỉ lặng lẽ biến mất khỏi listing.
 *   <li>{@code publishedViPostAppearsOnPublicSurface} — đi hết vòng: tạo ở quản
 *       trị, dịch, xuất bản, rồi kiểm bằng chính endpoint của khách.
 *   <li>{@code cannotRemoveRoleFromLastAdmin} — lưới an toàn giữ cho hệ
 *       thống còn mở được cửa.
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdminContentIT {

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

    /** Có sản phẩm trỏ tới — không xoá được. */
    private static final String DIEM_DEN_DANG_DUNG = "dd000000-0000-4000-8000-0000000000f2";
    /** Không ai dùng — xoá được. */
    private static final String DIEM_DEN_RANH = "dd000000-0000-4000-8000-0000000000f3";
    private static final String BAI_VIET = "dd200000-0000-4000-8000-000000000001";
    private static final String SU_KIEN_SAP_TOI = "dd300000-0000-4000-8000-000000000001";
    private static final String SU_KIEN_DA_QUA = "dd300000-0000-4000-8000-000000000002";
    private static final String THE_AM_THUC = "dd100000-0000-4000-8000-000000000001";
    private static final String THE_VAN_HOA = "dd100000-0000-4000-8000-000000000002";

    private static final String ADMIN_1 = "dd400000-0000-4000-8000-000000000001";
    private static final String ADMIN_2 = "dd400000-0000-4000-8000-000000000002";
    private static final String BIEN_TAP = "dd400000-0000-4000-8000-000000000003";
    private static final String NGUOI_DICH = "dd400000-0000-4000-8000-000000000004";

    @LocalServerPort
    int cong;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void prepareData() {
        jdbc.execute("""
                DELETE FROM staff_user_role;
                DELETE FROM post_tag;
                DELETE FROM post_translation;
                DELETE FROM post;
                DELETE FROM tag_translation;
                DELETE FROM tag;
                DELETE FROM lecture_translation;
                DELETE FROM lecture;
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
                  ('dd000000-0000-4000-8000-0000000000f1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('dd000000-0000-4000-8000-0000000000f1','da','nordvietnam','Nordvietnam'),
                  ('dd000000-0000-4000-8000-0000000000f1','vi','mien-bac','Miền Bắc');

                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('dd000000-0000-4000-8000-0000000000f2',
                   'dd000000-0000-4000-8000-0000000000f1','HANOI',1),
                  ('dd000000-0000-4000-8000-0000000000f3',
                   'dd000000-0000-4000-8000-0000000000f1','SAPA',2);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('dd000000-0000-4000-8000-0000000000f2','da','hanoi','Hanoi'),
                  ('dd000000-0000-4000-8000-0000000000f3','da','sapa','Sapa');

                -- Sản phẩm trỏ tới HANOI: điểm đến đó không xoá được.
                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('dd000000-0000-4000-8000-000000000001','GROUP_TOUR',
                        'dd000000-0000-4000-8000-0000000000f2',14,'/img/p.jpg');
                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES ('dd000000-0000-4000-8000-000000000001',12,20,12,'da',2);
                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status)
                VALUES ('dd000000-0000-4000-8000-000000000001','da','nord-til-syd','Nord til syd',
                        'Hele landet.', ARRAY['Et.','To.'], ARRAY['A','B','C'],
                        'Rismarker','PUBLISHED');
                INSERT INTO product_market (product_id, market, is_published) VALUES
                  ('dd000000-0000-4000-8000-000000000001','DK',TRUE);

                INSERT INTO tag (id, code, sort_order) VALUES
                  ('dd100000-0000-4000-8000-000000000001','FOOD',1),
                  ('dd100000-0000-4000-8000-000000000002','CULTURE',2);
                INSERT INTO tag_translation (tag_id, locale, slug, name) VALUES
                  ('dd100000-0000-4000-8000-000000000001','da','mad','Mad'),
                  ('dd100000-0000-4000-8000-000000000001','vi','am-thuc','Ẩm thực'),
                  ('dd100000-0000-4000-8000-000000000002','da','kultur','Kultur'),
                  ('dd100000-0000-4000-8000-000000000002','vi','van-hoa','Văn hoá');

                -- Bài viết CHỈ có bản `da`: cột trạng thái locale phải nói vi = MISSING.
                INSERT INTO post (id, hero_image, published_at) VALUES
                  ('dd200000-0000-4000-8000-000000000001','/img/b.jpg','2026-06-02T08:00:00Z');
                INSERT INTO post_translation (post_id, locale, slug, title, excerpt, body, status)
                VALUES ('dd200000-0000-4000-8000-000000000001','da','morgenmad',
                        'Morgenmad i Hanoi','Pho til morgenmad.',
                        ARRAY['Klokken seks.'],'PUBLISHED');
                INSERT INTO post_tag (post_id, tag_id) VALUES
                  ('dd200000-0000-4000-8000-000000000001','dd100000-0000-4000-8000-000000000001');

                -- Một buổi sắp tới và một buổi ĐÃ QUA: bề mặt khách chỉ thấy cái
                -- đầu, bề mặt quản trị thấy cả hai.
                INSERT INTO lecture (id, market, event_date, start_time, city, venue,
                                     seats, seats_taken) VALUES
                  ('dd300000-0000-4000-8000-000000000001','DK','2027-02-11','19:00',
                   'København','Kulturhuset',60,41),
                  ('dd300000-0000-4000-8000-000000000002','DK','2020-01-15','19:00',
                   'Aarhus','Dokk1',40,40);
                INSERT INTO lecture_translation (lecture_id, locale, title, description) VALUES
                  ('dd300000-0000-4000-8000-000000000001','da','Vietnam nord til syd','En aften.'),
                  ('dd300000-0000-4000-8000-000000000002','da','Mekong','Floden.');
                """);

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        addStaff(ADMIN_1, "admin1@travel.test", "Quản trị Một", hash, "ADMIN");
        addStaff(ADMIN_2, "admin2@travel.test", "Quản trị Hai", hash, "ADMIN");
        addStaff(BIEN_TAP, "bientap@travel.test", "Biên tập", hash, "EDITOR");
        addStaff(NGUOI_DICH, "nguoidich@travel.test", "Người dịch", hash, "TRANSLATOR");
    }

    // ==================================================== M13

    @Nested
    @DisplayName("M13 — nội dung khác")
    class NoiDung {

        /**
         * Luật quyền phụ thuộc <b>cả vai trò lẫn locale</b> — docs/22 mục 2.1
         * điều 2. Người dịch sửa bản nguồn là làm hai bản lệch nhau mà không ai
         * biết bản nào đúng.
         */
        @Test
        @DisplayName("Người dịch KHÔNG sửa được bản ngôn ngữ nguồn — 403")
        void translatorCannotEditSourceLocale() {
            ResponseEntity<ErrorResponse> response = login("nguoidich@travel.test").call(
                    HttpMethod.PUT,
                    "/api/v1/admin/destinations/" + DIEM_DEN_DANG_DUNG + "/translations/da",
                    "{\"slug\":\"hanoi-moi\",\"name\":\"Hanoi\"}", ErrorResponse.class);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("FORBIDDEN", response.getBody().getCode());
        }

        @Test
        @DisplayName("Biên tập KHÔNG sửa được bản dịch — 403")
        void editorCannotEditTranslation() {
            ResponseEntity<ErrorResponse> response = login("bientap@travel.test").call(
                    HttpMethod.PUT,
                    "/api/v1/admin/destinations/" + DIEM_DEN_DANG_DUNG + "/translations/vi",
                    "{\"slug\":\"ha-noi\",\"name\":\"Hà Nội\"}", ErrorResponse.class);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Mỗi vai trò sửa đúng bản của mình")
        void eachRoleEditsItsOwnLocale() {
            ResponseEntity<AdminDestinationDetail> source = login("bientap@travel.test").call(
                    HttpMethod.PUT,
                    "/api/v1/admin/destinations/" + DIEM_DEN_DANG_DUNG + "/translations/da",
                    "{\"slug\":\"hanoi\",\"name\":\"Hanoi by\",\"summary\":\"Hovedstaden.\"}",
                    AdminDestinationDetail.class);
            assertEquals(HttpStatus.OK, source.getStatusCode());

            ResponseEntity<AdminDestinationDetail> dich = login("nguoidich@travel.test").call(
                    HttpMethod.PUT,
                    "/api/v1/admin/destinations/" + DIEM_DEN_DANG_DUNG + "/translations/vi",
                    "{\"slug\":\"ha-noi\",\"name\":\"Hà Nội\"}",
                    AdminDestinationDetail.class);
            assertEquals(HttpStatus.OK, dich.getStatusCode());

            AdminDestinationDetail sau = dich.getBody();
            assertNotNull(sau);
            assertEquals(2, sau.getTranslations().size());

            // `isSource` phải đúng: đó là thứ màn hình dùng để biết ô nào chỉ đọc.
            assertTrue(sau.getTranslations().stream()
                    .anyMatch(t -> "da".equals(t.getLocale()) && Boolean.TRUE.equals(t.getIsSource())));
            assertTrue(sau.getTranslations().stream()
                    .anyMatch(t -> "vi".equals(t.getLocale()) && Boolean.FALSE.equals(t.getIsSource())));
        }

        /**
         * Cứ cho xoá thì <b>không có gì nổ</b>: sản phẩm chỉ lặng lẽ biến mất
         * khỏi listing, vì truy vấn của nó {@code INNER JOIN
         * destination_translation}. Đó là kiểu hỏng tệ nhất — đúng chính sách
         * không-fallback đang làm việc của nó, nên không có lỗi nào ghi ra.
         */
        @Test
        @DisplayName("Xoá điểm đến còn sản phẩm trỏ tới: 409, kèm số sản phẩm")
        void deletingDestinationWithProductsReturns409() {
            ResponseEntity<ErrorResponse> response = login("admin1@travel.test").call(
                    HttpMethod.DELETE, "/api/v1/admin/destinations/" + DIEM_DEN_DANG_DUNG,
                    null, ErrorResponse.class);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals("DESTINATION_IN_USE", response.getBody().getCode());
            assertEquals(1, ((Number) response.getBody().getParams().get("productCount")).intValue());
        }

        /**
         * Xoá mềm <b>không lan xuống dưới</b>: {@code ON DELETE CASCADE} chỉ
         * chạy khi xoá cứng. Bỏ sót bản dịch thì slug của nó vẫn chiếm chỗ trong
         * index duy nhất bộ phận, và người sau tạo lại cùng slug bị từ chối mà
         * thông báo lỗi không nói gì về nguyên nhân thật.
         */
        @Test
        @DisplayName("Xoá điểm đến rảnh: xoá mềm cả bản dịch của nó")
        void deletingFreeDestinationSoftDeletesTranslations() {
            ResponseEntity<String> response = login("admin1@travel.test").call(
                    HttpMethod.DELETE, "/api/v1/admin/destinations/" + DIEM_DEN_RANH,
                    null, String.class);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertEquals(1, count("SELECT count(*) FROM destination WHERE id = '"
                    + DIEM_DEN_RANH + "' AND soft_delete"));
            assertEquals(1, count("SELECT count(*) FROM destination_translation"
                    + " WHERE destination_id = '" + DIEM_DEN_RANH + "' AND soft_delete"));
        }

        @Test
        @DisplayName("Biên tập không xoá được điểm đến — chỉ ADMIN")
        void editorCannotDeleteDestination() {
            ResponseEntity<ErrorResponse> response = login("bientap@travel.test").call(
                    HttpMethod.DELETE, "/api/v1/admin/destinations/" + DIEM_DEN_RANH,
                    null, ErrorResponse.class);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        /**
         * Cột quan trọng nhất của danh sách M13: {@code MISSING} ở {@code vi}
         * nghĩa là bài này <b>không tồn tại</b> với khách đọc tiếng Việt.
         */
        @Test
        @DisplayName("Danh sách bài viết nói rõ locale nào còn thiếu")
        void postListNamesMissingLocales() {
            AdminPostPage trang = login("bientap@travel.test")
                    .get("/api/v1/admin/posts", AdminPostPage.class).getBody();

            assertNotNull(trang);
            assertEquals(1L, trang.getTotalItems());

            var post = trang.getItems().getFirst();
            assertEquals("Morgenmad i Hanoi", post.getTitle());
            assertEquals(AdminContentLocaleState.PUBLISHED, post.getLocales().get("da"));
            assertEquals(AdminContentLocaleState.MISSING, post.getLocales().get("vi"));
            assertEquals(1, post.getTags().size());
        }

        @Test
        @DisplayName("Tạo bài viết kèm luôn bản nguồn")
        void createPostWithSourceTranslation() {
            ResponseEntity<AdminPostDetail> response = login("bientap@travel.test").call(
                    HttpMethod.POST, "/api/v1/admin/posts",
                    """
                    {"heroImage":"/img/moi.jpg",
                     "tagIds":["%s"],
                     "translation":{"slug":"nyt-indlaeg","title":"Nyt indlæg",
                                    "excerpt":"Kort.","body":["Et.","To."],"status":"PUBLISHED"}}
                    """.formatted(THE_VAN_HOA),
                    AdminPostDetail.class);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            AdminPostDetail moi = response.getBody();
            assertNotNull(moi);
            assertEquals(1, moi.getTranslations().size());
            assertEquals("Nyt indlæg", moi.getTranslations().getFirst().getTitle());
            assertEquals(List.of("Et.", "To."), moi.getTranslations().getFirst().getBody());
            assertEquals(1, moi.getTags().size());
            assertEquals("CULTURE", moi.getTags().getFirst().getCode());
        }

        /**
         * Thẻ có endpoint riêng, và bài test này là lý do.
         *
         * <p>Bộ sinh mã dựng trường mảng vắng mặt thành danh sách <b>rỗng</b>,
         * nên khi thẻ còn nằm trong {@code PATCH} thì đổi mỗi cái ảnh bìa cũng
         * gỡ sạch thẻ của bài — và không có gì trên màn hình cho thấy điều đó
         * vừa xảy ra.
         */
        @Test
        @DisplayName("Sửa ảnh bìa KHÔNG đụng tới thẻ; endpoint thẻ thì thay sạch")
        void editingCoverImageDoesNotTouchTags() {
            Phien session = login("bientap@travel.test");

            AdminPostDetail afterImageChange = session.call(HttpMethod.PATCH,
                    "/api/v1/admin/posts/" + BAI_VIET,
                    "{\"heroImage\":\"/img/khac.jpg\"}", AdminPostDetail.class).getBody();
            assertNotNull(afterImageChange);
            assertEquals("/img/khac.jpg", afterImageChange.getHeroImage());
            assertEquals(1, afterImageChange.getTags().size(), "PATCH không được đụng tới thẻ");

            AdminPostDetail afterAssign = session.call(HttpMethod.PUT,
                    "/api/v1/admin/posts/" + BAI_VIET + "/tags",
                    "{\"tagIds\":[\"%s\",\"%s\"]}".formatted(THE_AM_THUC, THE_VAN_HOA),
                    AdminPostDetail.class).getBody();
            assertNotNull(afterAssign);
            assertEquals(2, afterAssign.getTags().size());

            AdminPostDetail afterRemove = session.call(HttpMethod.PUT,
                    "/api/v1/admin/posts/" + BAI_VIET + "/tags",
                    "{\"tagIds\":[]}", AdminPostDetail.class).getBody();
            assertNotNull(afterRemove);
            assertTrue(afterRemove.getTags().isEmpty(), "mảng rỗng thì gỡ hết");
        }

        /**
         * Vòng đầy đủ, và kiểm bằng <b>chính endpoint của khách</b>: một bài chỉ
         * "xong" khi khách đọc được nó.
         */
        @Test
        @DisplayName("Dịch rồi xuất bản `vi` thì bài hiện ở bề mặt khách")
        void publishedViPostAppearsOnPublicSurface() {
            assertEquals(0, publicPostCount("vi"), "trước khi dịch thì locale vi rỗng");

            ResponseEntity<AdminPostDetail> translated = login("nguoidich@travel.test").call(
                    HttpMethod.PUT, "/api/v1/admin/posts/" + BAI_VIET + "/translations/vi",
                    """
                    {"slug":"bua-sang-o-ha-noi","title":"Bữa sáng ở Hà Nội",
                     "excerpt":"Ăn phở buổi sáng.","body":["Sáu giờ sáng."],
                     "status":"PUBLISHED"}
                    """, AdminPostDetail.class);

            assertEquals(HttpStatus.OK, translated.getStatusCode());
            assertEquals(2, translated.getBody().getTranslations().size());

            assertEquals(1, publicPostCount("vi"), "dịch và xuất bản rồi thì khách đọc được");
        }

        /**
         * Bản dịch mới ở {@code DRAFT} thì khách <b>vẫn không</b> đọc được:
         * {@code status} là công tắc "lên website", không phải một nhãn trang trí.
         */
        @Test
        @DisplayName("Bản dịch DRAFT không lên website")
        void draftTranslationNotOnWebsite() {
            login("nguoidich@travel.test").call(
                    HttpMethod.PUT, "/api/v1/admin/posts/" + BAI_VIET + "/translations/vi",
                    """
                    {"slug":"ban-nhap","title":"Bản nháp","excerpt":"Chưa xong.",
                     "body":["Đang viết."],"status":"DRAFT"}
                    """, AdminPostDetail.class);

            assertEquals(0, publicPostCount("vi"));
        }

        @Test
        @DisplayName("Danh sách sự kiện quản trị có cả buổi đã qua, bề mặt khách thì không")
        void adminListIncludesPastLectures() {
            AdminLecturePage trang = login("bientap@travel.test")
                    .get("/api/v1/admin/lectures", AdminLecturePage.class).getBody();

            assertNotNull(trang);
            assertEquals(2L, trang.getTotalItems(), "nhân viên cần xem lại buổi cũ");

            // Bề mặt khách lọc `event_date >= hôm nay`: khách không đăng ký được
            // buổi hôm qua.
            assertEquals(1, publicLectureCount());
        }

        @Test
        @DisplayName("Hạ sức chứa xuống dưới số đã đăng ký: 409 CAPACITY_BELOW_BOOKED")
        void loweringCapacityBelowRegisteredReturns409() {
            ResponseEntity<ErrorResponse> response = login("bientap@travel.test").call(
                    HttpMethod.PATCH, "/api/v1/admin/lectures/" + SU_KIEN_SAP_TOI,
                    "{\"seats\":40}", ErrorResponse.class);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals("CAPACITY_BELOW_BOOKED", response.getBody().getCode());
            assertEquals(41, ((Number) response.getBody().getParams().get("seatsBooked")).intValue());
        }

        @Test
        @DisplayName("Hạ sức chứa xuống đúng số đã đăng ký thì được")
        void loweringCapacityToRegisteredCountAllowed() {
            AdminLectureDetail sau = login("bientap@travel.test").call(
                    HttpMethod.PATCH, "/api/v1/admin/lectures/" + SU_KIEN_SAP_TOI,
                    "{\"seats\":41,\"venue\":\"Ny sal\"}", AdminLectureDetail.class).getBody();

            assertNotNull(sau);
            assertEquals(41, sau.getSeats());
            assertEquals("Ny sal", sau.getVenue());
            // Trường vắng thì giữ nguyên — đó là điều PATCH nói.
            assertEquals("København", sau.getCity());
        }

        @Test
        @DisplayName("Xoá mềm buổi thuyết trình thì nó biến khỏi danh sách")
        void softDeletedLectureDisappearsFromList() {
            ResponseEntity<String> response = login("bientap@travel.test").call(
                    HttpMethod.DELETE, "/api/v1/admin/lectures/" + SU_KIEN_DA_QUA,
                    null, String.class);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

            AdminLecturePage trang = login("bientap@travel.test")
                    .get("/api/v1/admin/lectures", AdminLecturePage.class).getBody();
            assertNotNull(trang);
            assertEquals(1L, trang.getTotalItems());
        }
    }

    // ==================================================== M14

    @Nested
    @DisplayName("M14 — người dùng và vai trò")
    class NguoiDung {

        @Test
        @DisplayName("Chỉ ADMIN thấy màn hình người dùng")
        void onlyAdminSeesUserScreen() {
            for (String email : List.of("bientap@travel.test", "nguoidich@travel.test")) {
                assertEquals(HttpStatus.FORBIDDEN,
                        login(email).get("/api/v1/admin/users", String.class).getStatusCode(),
                        email + " không được thấy danh sách người dùng");
            }
            assertEquals(HttpStatus.OK, login("admin1@travel.test")
                    .get("/api/v1/admin/users", String.class).getStatusCode());
        }

        @Test
        @DisplayName("Danh sách trả vai trò, không trả gì về mật khẩu")
        void listReturnsRolesNeverPasswords() {
            ResponseEntity<String> tho = login("admin1@travel.test")
                    .get("/api/v1/admin/users", String.class);

            assertEquals(HttpStatus.OK, tho.getStatusCode());
            String body = tho.getBody();
            assertNotNull(body);
            // Đưa băm mật khẩu ra khỏi máy chủ là biến một lần rò rỉ log thành
            // một lần rò rỉ mật khẩu.
            assertFalse(body.contains("password"), "phản hồi không được nhắc tới mật khẩu");
            assertFalse(body.contains("$2a$"), "phản hồi không được chứa băm BCrypt");
            assertTrue(body.contains("TRANSLATOR"));
        }

        @Test
        @DisplayName("Gán vai trò thay SẠCH tập cũ")
        void assigningRolesReplacesWholeSet() {
            AdminStaffUser sau = login("admin1@travel.test").call(
                    HttpMethod.PUT, "/api/v1/admin/users/" + BIEN_TAP + "/roles",
                    "{\"roles\":[\"TRANSLATOR\",\"CONSULTANT\"]}",
                    AdminStaffUser.class).getBody();

            assertNotNull(sau);
            assertEquals(2, sau.getRoles().size());
            assertTrue(sau.getRoles().stream().anyMatch(r -> "TRANSLATOR".equals(r.getValue())));
            assertFalse(sau.getRoles().stream().anyMatch(r -> "EDITOR".equals(r.getValue())),
                    "vai trò cũ phải bị gỡ, không cộng dồn");
        }

        @Test
        @DisplayName("Tập vai trò rỗng là hợp lệ — người mới chờ phân việc")
        void emptyRoleSetIsValid() {
            AdminStaffUser sau = login("admin1@travel.test").call(
                    HttpMethod.PUT, "/api/v1/admin/users/" + NGUOI_DICH + "/roles",
                    "{\"roles\":[]}", AdminStaffUser.class).getBody();

            assertNotNull(sau);
            assertTrue(sau.getRoles().isEmpty());
        }

        /**
         * Lưới an toàn giữ cho hệ thống còn mở được cửa: không còn {@code ADMIN}
         * nào đang bật thì lối ra duy nhất là {@code UPDATE} tay trên cơ sở dữ
         * liệu lúc nửa đêm.
         */
        @Test
        @DisplayName("Không gỡ được vai trò của ADMIN cuối cùng")
        void cannotRemoveRoleFromLastAdmin() {
            Phien session = login("admin1@travel.test");

            // Gỡ ADMIN thứ hai thì được — vẫn còn admin1.
            assertEquals(HttpStatus.OK, session.call(HttpMethod.PUT,
                    "/api/v1/admin/users/" + ADMIN_2 + "/roles",
                    "{\"roles\":[\"EDITOR\"]}", AdminStaffUser.class).getStatusCode());

            // Giờ admin1 là người cuối cùng.
            ResponseEntity<ErrorResponse> response = session.call(HttpMethod.PUT,
                    "/api/v1/admin/users/" + ADMIN_1 + "/roles",
                    "{\"roles\":[\"EDITOR\"]}", ErrorResponse.class);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals("LAST_ADMIN", response.getBody().getCode());
        }

        @Test
        @DisplayName("Không tắt được ADMIN đang bật cuối cùng")
        void cannotDisableLastActiveAdmin() {
            Phien session = login("admin1@travel.test");

            assertEquals(HttpStatus.OK, session.call(HttpMethod.PATCH,
                    "/api/v1/admin/users/" + ADMIN_2,
                    "{\"isActive\":false}", AdminStaffUser.class).getStatusCode());

            ResponseEntity<ErrorResponse> response = session.call(HttpMethod.PATCH,
                    "/api/v1/admin/users/" + ADMIN_1,
                    "{\"isActive\":false}", ErrorResponse.class);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals("LAST_ADMIN", response.getBody().getCode());
        }

        @Test
        @DisplayName("Đổi tên hiển thị không đụng tới vai trò")
        void changingDisplayNameLeavesRolesAlone() {
            AdminStaffUser sau = login("admin1@travel.test").call(
                    HttpMethod.PATCH, "/api/v1/admin/users/" + BIEN_TAP,
                    "{\"displayName\":\"Biên tập viên\"}", AdminStaffUser.class).getBody();

            assertNotNull(sau);
            assertEquals("Biên tập viên", sau.getDisplayName());
            assertEquals(1, sau.getRoles().size());
            assertTrue(sau.getIsActive());
        }
    }

    // ------------------------------------------------------------ tiện ích

    /** Đếm qua chính endpoint của khách — thứ duy nhất chứng minh "khách đọc được". */
    private int publicPostCount(String locale) {
        PostPage trang = client("/api/v1/dk/posts", locale, PostPage.class).getBody();
        return trang == null ? 0 : trang.getItems().size();
    }

    private int publicLectureCount() {
        @SuppressWarnings("unchecked")
        List<Object> ds = client("/api/v1/dk/lectures", "da", List.class).getBody();
        return ds == null ? 0 : ds.size();
    }

    private <T> ResponseEntity<T> client(String path, String locale, Class<T> type) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + cong)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build()
                .get()
                .uri(path)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .retrieve()
                .toEntity(type);
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
