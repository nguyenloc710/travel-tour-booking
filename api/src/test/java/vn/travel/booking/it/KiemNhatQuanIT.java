package vn.travel.booking.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bộ kiểm tính nhất quán dữ liệu — {@code docs/12} mục 9, cài ở
 * {@code api/scripts/kiem-nhat-quan.sql}.
 *
 * <p><b>Vì sao là một bài test chứ không phải một dòng trong CI:</b> hai mươi
 * ba quy tắc ấy nằm trong tài liệu suốt từ đầu và chưa bao giờ có mã chạy được.
 * Đợt nạp dữ liệu mồi trước phải kiểm bằng SQL viết tại chỗ, và <b>ba quy tắc
 * bắt được lỗi thật</b> ngay lần đầu chạy. Một bảng trong tài liệu không bắt
 * được gì; một bài test thì bắt ở mỗi lần đẩy mã.
 *
 * <p>Bài test nạp <b>chính</b> {@code seed-dev.sql} bằng {@code psql} thật
 * trong container, nên nó kiểm hai thứ cùng lúc: tệp mồi chạy được từ đầu đến
 * cuối, và dữ liệu nó tạo ra thoả mọi quy tắc. Chạy hai lần để tệp mồi phải
 * <b>chạy lại được</b> — mọi lệnh {@code INSERT} có {@code ON CONFLICT}. Thiếu
 * một cái là lần chạy thứ hai đứt, và đó đúng là cách người ta dùng nó: sửa vài
 * dòng rồi nạp lại.
 *
 * <p>Mức {@code CANH_BAO} <b>không</b> làm đỏ. Quy tắc 13 đang cảnh báo có chủ
 * ý: giá trẻ em và em bé chờ Q-2, nên nếu nó làm đỏ thì cả nhánh đỏ vì một câu
 * hỏi chưa ai trả lời được.
 */
@SpringBootTest
@Testcontainers
class KiemNhatQuanIT {

    @org.testcontainers.junit.jupiter.Container
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

    @Autowired
    JdbcTemplate jdbc;

    private static boolean seeded;

    /**
     * Nạp dữ liệu mồi hai lần, bằng {@code psql} trong container.
     *
     * <p>Không dùng {@code ScriptUtils} của Spring: tệp mồi chạy trong <b>một</b>
     * transaction (trigger {@code ct_product_source_translation} là
     * {@code DEFERRABLE INITIALLY DEFERRED}), và nó chứa mảng chuỗi tiếng Đan có
     * dấu chấm phẩy bên trong. Tách câu lệnh bằng máy là chỗ để sai; gọi thẳng
     * {@code psql} thì đây đúng là lệnh mà người ta gõ.
     */
    @BeforeEach
    void loadSeedData() throws IOException, InterruptedException {
        // `@BeforeEach` chứ không `@BeforeAll`: Flyway chạy lúc Spring dựng
        // context, mà context chỉ dựng sau các callback lớp của JUnit. Nạp mồi
        // ở `@BeforeAll` là nạp vào một CSDL chưa có bảng nào.
        if (seeded) {
            return;
        }
        Path moi = find("scripts/seed-dev.sql");
        POSTGRES.copyFileToContainer(MountableFile.forHostPath(moi), "/tmp/seed-dev.sql");

        for (int lan = 1; lan <= 2; lan++) {
            Container.ExecResult kq = POSTGRES.execInContainer(
                    "psql", "-U", "travel", "-d", "travel",
                    "-v", "ON_ERROR_STOP=1", "-q", "-f", "/tmp/seed-dev.sql");
            assertEquals(0, kq.getExitCode(),
                    "seed-dev.sql đứt ở lần chạy thứ " + lan + ":\n" + kq.getStderr());
        }
        seeded = true;
    }

    @Test
    @DisplayName("dữ liệu mồi không vi phạm quy tắc nào ở mức LOI")
    void seedDataHasNoErrors() {
        assertTrue(seeded, "chưa nạp được dữ liệu mồi");

        List<Map<String, Object>> violation = run();
        List<Map<String, Object>> loi = violation.stream()
                .filter(v -> "LOI".equals(v.get("muc_do")))
                .toList();

        assertTrue(loi.isEmpty(), () -> "vi phạm mức LOI:\n" + description(loi));
    }

    /**
     * Quy tắc 13 phải còn <b>đúng một</b> mức cảnh báo, không im lặng.
     *
     * <p>Nếu nó bỗng sạch thì hoặc Q-2 đã có câu trả lời và ai đó đã nhập giá
     * trẻ em — lúc ấy quy tắc phải nâng lên {@code LOI} — hoặc truy vấn của quy
     * tắc đã hỏng và không còn đo gì. Cả hai đều cần người nhìn vào, nên bài
     * test này đỏ ở cả hai trường hợp.
     */
    @Test
    @DisplayName("quy tắc 13 vẫn cảnh báo — nó đỏ có chủ ý cho tới khi Q-2 xong")
    void rule13StillWarns() {
        long count = run().stream()
                .filter(v -> Integer.valueOf(13).equals(v.get("quy_tac")))
                .filter(v -> "CANH_BAO".equals(v.get("muc_do")))
                .count();

        assertTrue(count > 0,
                "quy tắc 13 không còn cảnh báo nào: hoặc Q-2 đã xong và phải nâng nó "
                        + "lên LOI, hoặc truy vấn của nó đã hỏng");
    }

    /**
     * Phá đúng một dòng dữ liệu rồi kiểm rằng quy tắc 23 bắt được.
     *
     * <p>Không có bài này thì một quy tắc gõ sai vẫn "xanh" mãi mãi, và cái xanh
     * ấy nguy hiểm hơn không có quy tắc nào: nó nói rằng đã kiểm rồi.
     */
    @Test
    @DisplayName("quy tắc 23 bắt được ngày khởi hành mất giá phòng đơn")
    void rule23CatchesMissingSingleRoomPrice() {
        String ngay = jdbc.queryForObject("""
                SELECT d.id::text
                FROM departure d
                JOIN product p ON p.id = d.product_id
                JOIN departure_price dp ON dp.departure_id = d.id AND dp.occupancy = 'SINGLE'
                WHERE p.product_type <> 'DAY_TOUR' AND NOT d.soft_delete
                LIMIT 1
                """, String.class);

        jdbc.update("DELETE FROM departure_price WHERE departure_id = ?::uuid "
                + "AND occupancy = 'SINGLE'", ngay);
        try {
            List<Map<String, Object>> bat = run().stream()
                    .filter(v -> Integer.valueOf(23).equals(v.get("quy_tac")))
                    .filter(v -> String.valueOf(v.get("doi_tuong")).contains(ngay))
                    .toList();

            assertEquals(1, bat.size(),
                    "quy tắc 23 phải bắt đúng ngày vừa gỡ giá, nhận: " + description(bat));
        } finally {
            // Trả dữ liệu về như cũ để hai bài test kia không phụ thuộc thứ tự chạy.
            napLaiGiaPhongDon();
        }
    }

    private void napLaiGiaPhongDon() {
        try {
            Container.ExecResult kq = POSTGRES.execInContainer(
                    "psql", "-U", "travel", "-d", "travel",
                    "-v", "ON_ERROR_STOP=1", "-q", "-f", "/tmp/seed-dev.sql");
            assertEquals(0, kq.getExitCode(), kq.getStderr());
        } catch (IOException | InterruptedException ex) {
            throw new IllegalStateException("không nạp lại được dữ liệu mồi", ex);
        }
    }

    private List<Map<String, Object>> run() {
        try {
            return jdbc.queryForList(Files.readString(find("scripts/kiem-nhat-quan.sql")));
        } catch (IOException ex) {
            throw new IllegalStateException("không đọc được bộ kiểm", ex);
        }
    }

    private static String description(List<Map<String, Object>> v) {
        return v.stream()
                .map(d -> "  [%s] quy tắc %s · %s — %s"
                        .formatted(d.get("muc_do"), d.get("quy_tac"),
                                d.get("doi_tuong"), d.get("chi_tiet")))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Gradle chạy test với thư mục làm việc là {@code api/}, còn IDE thì hay đặt
     * ở gốc repo. Thử cả hai thay vì bắt người chạy phải nhớ.
     */
    private static Path find(String path) {
        for (Path origin : List.of(Path.of(""), Path.of("api"), Path.of(".."))) {
            Path p = origin.resolve(path);
            if (Files.exists(p)) {
                return p.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("không tìm thấy " + path
                + " từ " + Path.of("").toAbsolutePath());
    }
}
