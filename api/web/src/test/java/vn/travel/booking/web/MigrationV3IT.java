package vn.travel.booking.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tầng nội dung biên tập thêm ở {@code V3} — docs/12 mục 4.8 tới 4.11.
 */
@SpringBootTest
@Testcontainers
class MigrationV3IT {

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

    private static final String SAN_PHAM = "d3000000-0000-4000-8000-000000000001";
    private static final String DIEM_DEN = "d3000000-0000-4000-8000-000000000002";
    private static final String KHACH_SAN = "d3000000-0000-4000-8000-000000000003";

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void chuanBiDuLieu() {
        jdbc.execute("""
                DELETE FROM slug_history;
                DELETE FROM post_tag;
                DELETE FROM post_translation;
                DELETE FROM post;
                DELETE FROM tag_translation;
                DELETE FROM tag;
                DELETE FROM lecture_translation;
                DELETE FROM lecture;
                DELETE FROM itinerary_day_translation;
                DELETE FROM itinerary_day;
                DELETE FROM product_hotel_stay;
                DELETE FROM product_theme;
                DELETE FROM theme_translation;
                DELETE FROM theme;
                DELETE FROM hotel_translation;
                DELETE FROM hotel;
                DELETE FROM excursion_translation;
                DELETE FROM excursion;
                DELETE FROM product_translation;
                DELETE FROM product_group_tour;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;

                INSERT INTO region (id, code, sort_order)
                VALUES ('d3000000-0000-4000-8000-0000000000f1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name)
                VALUES ('d3000000-0000-4000-8000-0000000000f1','da','nordvietnam','Nordvietnam');

                INSERT INTO destination (id, region_id, code, sort_order)
                VALUES ('d3000000-0000-4000-8000-000000000002',
                        'd3000000-0000-4000-8000-0000000000f1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name)
                VALUES ('d3000000-0000-4000-8000-000000000002','da','hanoi','Hanoi');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('d3000000-0000-4000-8000-000000000001','GROUP_TOUR',
                        'd3000000-0000-4000-8000-000000000002',3,'/img/p.jpg');
                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES ('d3000000-0000-4000-8000-000000000001',12,20,10,'da',2);
                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status)
                VALUES ('d3000000-0000-4000-8000-000000000001','da','nord-til-syd','Nord til syd',
                        'Hele landet.', ARRAY['Et.','To.'], ARRAY['A','B','C'],'Rismarker','PUBLISHED');

                INSERT INTO hotel (id, destination_id, name, stars)
                VALUES ('d3000000-0000-4000-8000-000000000003',
                        'd3000000-0000-4000-8000-000000000002','Sofitel Legend Metropole',5);
                """);
    }

    @Test
    @DisplayName("Lịch trình: một sản phẩm không có hai ngày cùng số thứ tự")
    void trungSoNgay() {
        themNgay("d3100000-0000-4000-8000-000000000001", 1);

        assertThrows(DataIntegrityViolationException.class,
                () -> themNgay("d3100000-0000-4000-8000-000000000002", 1));
    }

    @Test
    @DisplayName("Xoá mềm một ngày rồi thêm lại ngày khác cùng số — index bộ phận cho phép")
    void themLaiNgayDaXoa() {
        themNgay("d3100000-0000-4000-8000-000000000003", 2);
        jdbc.update("UPDATE itinerary_day SET soft_delete = TRUE WHERE id = CAST(? AS uuid)",
                "d3100000-0000-4000-8000-000000000003");

        // Biên tập viên dựng lại ngày 2 sau khi xoá nhầm. Với CONSTRAINT UNIQUE
        // thường thì câu này đỏ và thông báo lỗi không nói gì về nguyên nhân thật.
        themNgay("d3100000-0000-4000-8000-000000000004", 2);

        assertEquals(1, dem("SELECT count(*) FROM itinerary_day WHERE NOT soft_delete"));
    }

    @Test
    @DisplayName("Mô tả ngày KHÔNG bị chặn bởi CHECK độ dài — đó là kiểm chất lượng, không phải toàn vẹn")
    void moTaNganVanLuuDuoc() {
        themNgay("d3100000-0000-4000-8000-000000000005", 3);
        jdbc.update("""
                INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description)
                VALUES (CAST(? AS uuid),'da','Ankomst','Kort.')
                """, "d3100000-0000-4000-8000-000000000005");

        assertEquals(1, dem("SELECT count(*) FROM itinerary_day_translation"),
                "Chặn bằng CHECK là chặn biên tập viên lưu bản nháp giữa chừng");
    }

    @Test
    @DisplayName("Tên khách sạn không nằm trong bảng dịch, mô tả thì có")
    void tenKhachSanKhongDich() {
        List<String> cotDich = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = 'hotel_translation' AND column_name IN ('name','description')",
                String.class);

        assertEquals(List.of("description"), cotDich,
                "Tên riêng của khách sạn không dịch — docs/24 mục 5");
    }

    @Test
    @DisplayName("Không xoá cứng được khách sạn còn nằm trong lịch trình hoặc chặng nghỉ")
    void khongXoaDuocKhachSanDangDung() {
        themNgay("d3100000-0000-4000-8000-000000000006", 1);
        jdbc.update("UPDATE itinerary_day SET hotel_id = CAST(? AS uuid) WHERE id = CAST(? AS uuid)",
                KHACH_SAN, "d3100000-0000-4000-8000-000000000006");

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("DELETE FROM hotel WHERE id = CAST(? AS uuid)", KHACH_SAN));
    }

    @Test
    @DisplayName("Số đêm của một chặng nghỉ phải từ 1 trở lên")
    void soDemPhaiDuong() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO product_hotel_stay (product_id, hotel_id, nights)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), 0)
                """, SAN_PHAM, KHACH_SAN));
    }

    @Test
    @DisplayName("Tham quan thuộc điểm đến, không thuộc sản phẩm")
    void thamQuanThuocDiemDen() {
        Boolean coCotSanPham = jdbc.queryForObject(
                "SELECT count(*) = 0 FROM information_schema.columns "
                        + "WHERE table_name = 'excursion' AND column_name = 'product_id'",
                Boolean.class);

        assertEquals(Boolean.TRUE, coCotSanPham,
                "Gắn tham quan vào sản phẩm là chép mô tả ra nhiều bản rồi để chúng lệch nhau");
    }

    @Test
    @DisplayName("Đổi slug bài viết thì trigger tự ghi slug cũ, đúng loại POST")
    void doiSlugBaiViet() {
        jdbc.execute("""
                INSERT INTO post (id) VALUES ('d3200000-0000-4000-8000-000000000001');
                INSERT INTO post_translation (post_id, locale, slug, title, excerpt, body, status)
                VALUES ('d3200000-0000-4000-8000-000000000001','da','gammel-slug','Titel',
                        'Uddrag.', ARRAY['Afsnit.'],'PUBLISHED');
                """);

        jdbc.update("UPDATE post_translation SET slug = 'ny-slug' WHERE post_id = CAST(? AS uuid)",
                "d3200000-0000-4000-8000-000000000001");

        assertEquals("POST", jdbc.queryForObject(
                "SELECT entity_type FROM slug_history", String.class));
        assertEquals("gammel-slug", jdbc.queryForObject(
                "SELECT old_slug FROM slug_history", String.class));
    }

    @Test
    @DisplayName("Buổi thuyết trình thuộc đúng một thị trường, và số chỗ đã nhận không vượt sức chứa")
    void buoiThuyetTrinh() {
        jdbc.update("""
                INSERT INTO lecture (id, market, event_date, city, seats, seats_taken)
                VALUES ('d3300000-0000-4000-8000-000000000001','DK','2027-03-14','Odense',60,12)
                """);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE lecture SET seats_taken = 61 WHERE id = CAST(? AS uuid)",
                "d3300000-0000-4000-8000-000000000001"));

        // Số chỗ còn lại là giá trị TÍNH RA, không lưu thành cột thứ ba.
        assertEquals(48, dem("SELECT seats - seats_taken FROM lecture"));

        Integer coCotConLai = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_name = 'lecture' AND column_name LIKE '%available%'",
                Integer.class);
        assertEquals(0, coCotConLai);
    }

    @Test
    @DisplayName("Cột là event_date chứ không phải date — cùng loại bẫy với collation ở V1")
    void tenCotNgay() {
        Integer coCotDate = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_name = 'lecture' AND column_name = 'date'", Integer.class);
        assertEquals(0, coCotDate);

        assertEquals(1, dem("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_name = 'lecture' AND column_name = 'event_date'"));
    }

    @Test
    @DisplayName("Ba bảng nối thuộc nhóm C: không cột kiểm toán, không xoá mềm")
    void bangNoiKhongCoCotKiemToan() {
        Integer so = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_name IN ('product_theme','product_hotel_stay','post_tag')
                  AND column_name IN ('created_at','last_modified_by','soft_delete')
                """, Integer.class);

        assertEquals(0, so, "Vòng đời của chúng trùng khít với bảng cha — docs/11 mục 11.2");
    }

    @Test
    @DisplayName("Mọi bảng có last_modified_at đều có trigger — quy tắc kiểm 17")
    void moiBangDeuCoTrigger() {
        List<String> thieu = jdbc.queryForList("""
                SELECT c.table_name
                FROM information_schema.columns c
                WHERE c.table_schema = 'public'
                  AND c.column_name = 'last_modified_at'
                  AND NOT EXISTS (
                    SELECT 1 FROM information_schema.triggers t
                    WHERE t.event_object_table = c.table_name
                      AND t.trigger_name = 'tg_' || c.table_name || '_last_modified')
                ORDER BY 1
                """, String.class);

        assertTrue(thieu.isEmpty(), "Bảng thiếu trigger: " + thieu);
    }

    // ------------------------------------------------------------ tiện ích

    private void themNgay(String id, int soNgay) {
        jdbc.update("INSERT INTO itinerary_day (id, product_id, day_number, destination_id) "
                + "VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, CAST(? AS uuid))",
                id, SAN_PHAM, soNgay, DIEM_DEN);
    }

    private int dem(String sql) {
        Integer so = jdbc.queryForObject(sql, Integer.class);
        return so == null ? 0 : so;
    }
}
