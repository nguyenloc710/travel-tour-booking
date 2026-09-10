package vn.travel.booking.it;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.travel.booking.web.generated.model.AdminMedia;
import vn.travel.booking.web.generated.model.ErrorCode;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.FieldError;
import vn.travel.booking.web.generated.model.FieldRule;
import vn.travel.booking.web.generated.model.MediaKind;
import vn.travel.booking.web.generated.model.MediaSource;
import vn.travel.booking.web.generated.model.MediaUploadUrl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Luồng tải ảnh hai bước — ADR-011 mục 4, ADR-013.
 *
 * <p>Bài này chạy <b>MinIO thật</b> trong container chứ không giả lập kho. Lý do
 * nằm ở chính thứ dễ sai nhất: chữ ký SigV4 bao gồm cả host, và một kho giả sẽ
 * nhận mọi chữ ký nên nó không chứng minh được gì. Ở đây trình duyệt được đóng
 * vai bằng {@link HttpClient} gọi thẳng vào URL đã ký.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MediaUploadIT {

    private static final String KHO_USER = "travel";
    private static final String KHO_PASS = "travel-dev-secret";
    private static final String BUCKET = "travel-media";

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("travel")
                    .withUsername("travel")
                    .withPassword("travel");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> MINIO =
            new GenericContainer<>("minio/minio:RELEASE.2025-04-22T22-12-26Z")
                    .withEnv("MINIO_ROOT_USER", KHO_USER)
                    .withEnv("MINIO_ROOT_PASSWORD", KHO_PASS)
                    .withCommand("server", "/data")
                    .withExposedPorts(9000)
                    .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

    private static String khoUrl() {
        return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
    }

    @DynamicPropertySource
    static void cauHinh(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Hai địa chỉ trùng nhau ở đây vì cả API lẫn "trình duyệt" đều chạy trên
        // máy này — đúng như dev. Chỗ chúng khác nhau là bản chạy thật.
        registry.add("travel.storage.endpoint-public", MediaUploadIT::khoUrl);
        registry.add("travel.storage.endpoint-internal", MediaUploadIT::khoUrl);
        registry.add("travel.storage.access-key", () -> KHO_USER);
        registry.add("travel.storage.secret-key", () -> KHO_PASS);
        registry.add("travel.storage.bucket", () -> BUCKET);
        registry.add("travel.storage.public-base-url", () -> khoUrl() + "/" + BUCKET);
    }

    private static final String PASSWORD = "mat-khau-rat-dai";
    private static final byte[] ANH = "khong-phai-anh-that-nhung-du-de-do-byte".getBytes(StandardCharsets.UTF_8);

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void chuanBi() throws Exception {
        MinioClient kho = MinioClient.builder().endpoint(khoUrl()).credentials(KHO_USER, KHO_PASS).build();
        if (!kho.bucketExists(io.minio.BucketExistsArgs.builder().bucket(BUCKET).build())) {
            kho.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }

        jdbc.execute("""
                DELETE FROM media_asset_translation;
                DELETE FROM media_asset;
                DELETE FROM staff_user_role;
                DELETE FROM staff_user;
                """);

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        themNhanVien("dd100000-0000-4000-8000-000000000001", "admin@travel.test", hash, "ADMIN");
        themNhanVien("dd100000-0000-4000-8000-000000000002", "dich@travel.test", hash, "TRANSLATOR");
    }

    // ------------------------------------------------------------ vòng khép kín

    @Test
    @DisplayName("Ký, tải lên bằng URL đã ký, rồi ghi bản ghi — ba bước, một tệp")
    void uploadEndToEnd() throws Exception {
        Session admin = login("admin@travel.test");

        ResponseEntity<MediaUploadUrl> ve = admin.call(HttpMethod.POST, "/api/v1/admin/media/upload-url",
                """
                {"fileName":"sapa.jpg","contentType":"image/jpeg","byteSize":%d}
                """.formatted(ANH.length),
                MediaUploadUrl.class);

        assertEquals(HttpStatus.OK, ve.getStatusCode());
        // Đường dẫn do MÁY CHỦ đặt: thư mục mặc định + UUID + đuôi theo contentType.
        assertTrue(ve.getBody().getPath().startsWith("tour/"), ve.getBody().getPath());
        assertTrue(ve.getBody().getPath().endsWith(".jpg"), ve.getBody().getPath());
        assertTrue(!ve.getBody().getPath().contains("sapa"),
                "tên tệp của client chỉ dùng để lấy đuôi, không đi vào đường dẫn");

        // Đóng vai trình duyệt: PUT thẳng lên kho, không đi qua API.
        HttpResponse<String> tai = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(ve.getBody().getUploadUrl()))
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(ANH))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, tai.statusCode(), tai.body());

        ResponseEntity<AdminMedia> anh = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"path":"%s","width":1600,"height":900,"byteSize":%d,
                 "source":"SELF","alt":"Rismarker i Sapa"}
                """.formatted(ve.getBody().getPath(), ANH.length),
                AdminMedia.class);

        assertEquals(HttpStatus.CREATED, anh.getStatusCode());
        assertNotNull(anh.getBody().getId());
        assertEquals(MediaSource.SELF, anh.getBody().getSource());
        // URL công khai ghép ở backend, không để frontend tự nối chuỗi.
        assertEquals(khoUrl() + "/" + BUCKET + "/" + ve.getBody().getPath(), anh.getBody().getUrl());
        // Mới chỉ có bản nguồn: ảnh này còn VÔ HÌNH ở trang tiếng Việt.
        assertEquals(List.of("da"), anh.getBody().getTranslatedLocales());
    }

    // ------------------------------------------------------------ luật

    @Test
    @DisplayName("Ghi bản ghi cho tệp chưa từng tải lên: 400, chỉ đúng ô path")
    void recordWithoutFileRejected() {
        ResponseEntity<ErrorResponse> loi = login("admin@travel.test").call(
                HttpMethod.POST, "/api/v1/admin/media",
                """
                {"path":"tour/khong-ton-tai.jpg","width":800,"height":600,"byteSize":123,
                 "source":"SELF","alt":"Ikke uploadet"}
                """,
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_FAILED, loi.getBody().getCode());

        FieldError f = loi.getBody().getFields().getFirst();
        assertEquals("path", f.getPath());
        assertEquals(FieldRule.INVALID, f.getCode());
    }

    @Test
    @DisplayName("Ảnh không tự chụp mà thiếu chứng từ giấy phép: 400, chỉ đúng ô licenceRef")
    void partnerImageNeedsLicence() throws Exception {
        Session admin = login("admin@travel.test");
        String path = taiLen(admin);

        ResponseEntity<ErrorResponse> loi = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"path":"%s","width":800,"height":600,"byteSize":%d,
                 "source":"PARTNER","alt":"Fra partner"}
                """.formatted(path, ANH.length),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_FAILED, loi.getBody().getCode());

        FieldError f = loi.getBody().getFields().getFirst();
        assertEquals("licenceRef", f.getPath());
        assertEquals(FieldRule.NOT_NULL, f.getCode());
        // Ràng buộc ck_media_licence của CSDL nói y hệt; bắt ở tầng nghiệp vụ để
        // nó thành 400 chỉ đúng ô, thay vì 500 kèm traceId.
        assertEquals("PARTNER", f.getParams().get("source"));
    }

    @Test
    @DisplayName("Người dịch đặt được alt bản vi, bị từ chối ở bản da")
    void translatorOwnsOnlyTheTranslation() throws Exception {
        Session admin = login("admin@travel.test");
        String path = taiLen(admin);

        ResponseEntity<AdminMedia> anh = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"path":"%s","width":800,"height":600,"byteSize":%d,
                 "source":"SELF","alt":"Rismarker"}
                """.formatted(path, ANH.length),
                AdminMedia.class);
        String id = anh.getBody().getId().toString();

        Session dich = login("dich@travel.test");

        ResponseEntity<ErrorResponse> bTuChoi = dich.call(HttpMethod.PUT,
                "/api/v1/admin/media/" + id + "/translations/da",
                "{\"alt\":\"Sửa bản nguồn\"}", ErrorResponse.class);
        assertEquals(HttpStatus.FORBIDDEN, bTuChoi.getStatusCode());

        ResponseEntity<AdminMedia> sau = dich.call(HttpMethod.PUT,
                "/api/v1/admin/media/" + id + "/translations/vi",
                "{\"alt\":\"Ruộng bậc thang\"}", AdminMedia.class);
        assertEquals(HttpStatus.OK, sau.getStatusCode());
        // Giờ mới đủ hai bản — ảnh hiện được ở cả hai locale.
        assertEquals(List.of("da", "vi"), sau.getBody().getTranslatedLocales());
    }

    @Test
    @DisplayName("Video: ảnh bìa trước, video sau — và thiếu bìa thì bị từ chối")
    void videoNeedsPoster() throws Exception {
        Session admin = login("admin@travel.test");

        // Ảnh bìa là một bản ghi IMAGE riêng: nó có giấy phép và alt của chính nó.
        String duongDanBia = taiLen(admin);
        ResponseEntity<AdminMedia> bia = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"path":"%s","width":1280,"height":720,"byteSize":%d,
                 "source":"SELF","alt":"Stillbillede fra videoen"}
                """.formatted(duongDanBia, ANH.length),
                AdminMedia.class);
        assertEquals(MediaKind.IMAGE, bia.getBody().getKind());

        String duongDanVideo = taiLenVideo(admin);

        // Thiếu ảnh bìa: ck_media_video của V9 cũng chặn, nhưng bắt ở tầng nghiệp
        // vụ để lỗi chỉ đúng ô thay vì thành 500.
        ResponseEntity<ErrorResponse> thieuBia = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"kind":"VIDEO","path":"%s","width":1920,"height":1080,"byteSize":%d,
                 "source":"SELF","alt":"Sapa fra oven","contentType":"video/mp4",
                 "durationSeconds":42}
                """.formatted(duongDanVideo, ANH.length),
                ErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, thieuBia.getStatusCode());
        assertEquals("posterAssetId", thieuBia.getBody().getFields().getFirst().getPath());
        assertEquals(FieldRule.NOT_NULL, thieuBia.getBody().getFields().getFirst().getCode());

        ResponseEntity<AdminMedia> video = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"kind":"VIDEO","path":"%s","width":1920,"height":1080,"byteSize":%d,
                 "source":"SELF","alt":"Sapa fra oven","contentType":"video/mp4",
                 "durationSeconds":42,"posterAssetId":"%s"}
                """.formatted(duongDanVideo, ANH.length, bia.getBody().getId()),
                AdminMedia.class);

        assertEquals(HttpStatus.CREATED, video.getStatusCode());
        assertEquals(MediaKind.VIDEO, video.getBody().getKind());
        assertEquals(42, video.getBody().getDurationSeconds());
        // Địa chỉ ảnh bìa ghép ở backend, cùng lý do với `url`.
        assertEquals(bia.getBody().getUrl(), video.getBody().getPosterUrl());
    }

    @Test
    @DisplayName("Ảnh bìa trỏ vào một video khác thì bị từ chối")
    void posterMustBeAnImage() throws Exception {
        Session admin = login("admin@travel.test");

        String biaPath = taiLen(admin);
        ResponseEntity<AdminMedia> bia = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"path":"%s","width":10,"height":10,"byteSize":%d,"source":"SELF","alt":"x"}
                """.formatted(biaPath, ANH.length), AdminMedia.class);

        ResponseEntity<AdminMedia> video = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"kind":"VIDEO","path":"%s","width":20,"height":20,"byteSize":%d,
                 "source":"SELF","alt":"y","contentType":"video/mp4",
                 "durationSeconds":5,"posterAssetId":"%s"}
                """.formatted(taiLenVideo(admin), ANH.length, bia.getBody().getId()),
                AdminMedia.class);

        ResponseEntity<ErrorResponse> loi = admin.call(HttpMethod.POST, "/api/v1/admin/media",
                """
                {"kind":"VIDEO","path":"%s","width":20,"height":20,"byteSize":%d,
                 "source":"SELF","alt":"z","contentType":"video/mp4",
                 "durationSeconds":5,"posterAssetId":"%s"}
                """.formatted(taiLenVideo(admin), ANH.length, video.getBody().getId()),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
        FieldError f = loi.getBody().getFields().getFirst();
        assertEquals("posterAssetId", f.getPath());
        // Luật này KHÔNG diễn đạt được bằng CHECK: nó là điều kiện trên dòng khác.
        assertEquals("khongPhaiAnh", f.getParams().get("reason"));
    }

    @Test
    @DisplayName("Trần byte khác nhau theo loại, và kiểm ngay ở bước ký")
    void byteLimitDependsOnKind() {
        Session admin = login("admin@travel.test");
        long muoiMotMB = 11L * 1024 * 1024;

        ResponseEntity<ErrorResponse> anhQuaTo = admin.call(HttpMethod.POST,
                "/api/v1/admin/media/upload-url",
                """
                {"fileName":"to.jpg","contentType":"image/jpeg","byteSize":%d}
                """.formatted(muoiMotMB),
                ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, anhQuaTo.getStatusCode());
        FieldError f = anhQuaTo.getBody().getFields().getFirst();
        assertEquals("byteSize", f.getPath());
        assertEquals(FieldRule.MAX, f.getCode());
        assertEquals(10L * 1024 * 1024, ((Number) f.getParams().get("max")).longValue());

        // Cùng số byte đó, nhưng là video thì lọt — trần video là 200 MB.
        assertEquals(HttpStatus.OK, admin.call(HttpMethod.POST, "/api/v1/admin/media/upload-url",
                """
                {"fileName":"to.mp4","contentType":"video/mp4","byteSize":%d}
                """.formatted(muoiMotMB),
                MediaUploadUrl.class).getStatusCode());
    }

    @Test
    @DisplayName("Kiểu tệp ngoài danh mục bị chặn ngay ở bước xin URL")
    void contentTypeOutsideAllowList() {
        ResponseEntity<ErrorResponse> loi = login("admin@travel.test").call(
                HttpMethod.POST, "/api/v1/admin/media/upload-url",
                """
                {"fileName":"x.svg","contentType":"image/svg+xml","byteSize":10}
                """,
                ErrorResponse.class);

        // Kho mở quyền đọc ẩn danh, nên mọi thứ lên đó là tệp công khai. SVG là
        // tài liệu chạy được script — danh mục đóng ở hợp đồng chặn từ cửa.
        assertEquals(HttpStatus.BAD_REQUEST, loi.getStatusCode());
    }

    // ------------------------------------------------------------ tiện ích

    /** Xin URL rồi tải lên, trả về đường dẫn trong kho. */
    private String taiLen(Session session) throws Exception {
        ResponseEntity<MediaUploadUrl> ve = session.call(HttpMethod.POST, "/api/v1/admin/media/upload-url",
                """
                {"fileName":"a.jpg","contentType":"image/jpeg","byteSize":%d}
                """.formatted(ANH.length),
                MediaUploadUrl.class);

        HttpResponse<String> tai = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(ve.getBody().getUploadUrl()))
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(ANH))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, tai.statusCode(), tai.body());

        return ve.getBody().getPath();
    }

    /** Xin URL cho một video rồi tải lên, trả về đường dẫn trong kho. */
    private String taiLenVideo(Session session) throws Exception {
        ResponseEntity<MediaUploadUrl> ve = session.call(HttpMethod.POST, "/api/v1/admin/media/upload-url",
                """
                {"fileName":"a.mp4","contentType":"video/mp4","byteSize":%d}
                """.formatted(ANH.length),
                MediaUploadUrl.class);

        HttpResponse<String> tai = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(ve.getBody().getUploadUrl()))
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(ANH))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, tai.statusCode(), tai.body());

        return ve.getBody().getPath();
    }

    private void themNhanVien(String id, String email, String hash, String role) {
        jdbc.update("""
                INSERT INTO staff_user (id, email, display_name, password_hash, is_active)
                VALUES (?::uuid, ?, ?, ?, TRUE)
                """, id, email, email, hash);
        jdbc.update("""
                INSERT INTO staff_user_role (id, staff_user_id, role_code)
                VALUES (gen_random_uuid(), ?::uuid, ?)
                """, id, role);
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

        <T> ResponseEntity<T> call(HttpMethod httpMethod, String path, String body, Class<T> type) {
            final RestClient.RequestBodySpec request = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultStatusHandler(status -> true, (req, res) -> { })
                    .build()
                    .method(httpMethod)
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON);

            if (!cookies.isEmpty()) {
                request.header(HttpHeaders.COOKIE, String.join("; ", cookies));
                csrf().ifPresent(token -> request.header("X-XSRF-TOKEN", token));
            }
            if (body != null) {
                request.body(body);
            }

            ResponseEntity<T> response = request.retrieve().toEntity(type);
            response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE)
                    .forEach(c -> cookies.add(c.split(";", 2)[0]));
            return response;
        }

        private java.util.Optional<String> csrf() {
            return cookies.stream()
                    .filter(c -> c.startsWith("XSRF-TOKEN="))
                    .map(c -> c.substring("XSRF-TOKEN=".length()))
                    .reduce((a, b) -> b);
        }
    }
}
