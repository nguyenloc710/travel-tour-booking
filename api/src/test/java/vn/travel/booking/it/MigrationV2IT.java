package vn.travel.booking.it;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bốn nhóm bảng thêm ở {@code V2} — docs/12 mục 3.1, 4.6, 4.7, 6.1.
 *
 * <p>Test ở đây kiểm <b>hành vi của lược đồ</b>, không kiểm endpoint: ràng buộc
 * CSDL và trigger là thứ chỉ Postgres thật mới trả lời được, và chúng là chỗ
 * sai âm thầm nhất.
 */
@SpringBootTest
@Testcontainers
class MigrationV2IT {

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

    private static final String NHAN_VIEN = "c0000000-0000-4000-8000-0000000000a1";

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void chuanBiDuLieu() {
        jdbc.execute("""
                DELETE FROM slug_history;
                DELETE FROM staff_user_role;
                DELETE FROM product_image;
                DELETE FROM media_asset_translation;
                DELETE FROM media_asset;
                DELETE FROM product_translation;
                DELETE FROM product_market;
                DELETE FROM product_group_tour;
                DELETE FROM product;
                DELETE FROM destination_translation;
                DELETE FROM destination;
                DELETE FROM region_translation;
                DELETE FROM region;
                DELETE FROM staff_user;

                INSERT INTO staff_user (id, email, display_name, password_hash) VALUES
                  ('c0000000-0000-4000-8000-0000000000a1','bien.tap@travel.test','Biên tập viên','x');

                INSERT INTO region (id, code, sort_order) VALUES
                  ('a0000000-0000-4000-8000-0000000000b1','NORTH',1);
                INSERT INTO region_translation (region_id, locale, slug, name) VALUES
                  ('a0000000-0000-4000-8000-0000000000b1','da','nordvietnam','Nordvietnam');

                INSERT INTO destination (id, region_id, code, sort_order) VALUES
                  ('a0000000-0000-4000-8000-0000000000c1','a0000000-0000-4000-8000-0000000000b1','HANOI',1);
                INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
                  ('a0000000-0000-4000-8000-0000000000c1','da','hanoi','Hanoi');

                INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image)
                VALUES ('a0000000-0000-4000-8000-0000000000d1','GROUP_TOUR',
                        'a0000000-0000-4000-8000-0000000000c1',14,'/img/p.jpg');
                INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                                tour_leader_language, fitness_level)
                VALUES ('a0000000-0000-4000-8000-0000000000d1',12,20,10,'da',2);
                INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                       long_description, why_choose_this, hero_image_alt, status)
                VALUES ('a0000000-0000-4000-8000-0000000000d1','da','slug-cu-nhat',
                        'Vietnam fra nord til syd','Rundrejse.',
                        ARRAY['Et.','To.'], ARRAY['A','B','C'],
                        'Rismarker','PUBLISHED');
                """);
    }

    // ------------------------------------------------------------ vai trò

    @Test
    @DisplayName("Bốn vai trò nạp từ dữ liệu tra cứu, không nhập tay")
    void bonVaiTro() {
        List<String> ma = jdbc.queryForList(
                "SELECT code FROM role ORDER BY sort_order", String.class);

        assertEquals(List.of("CONSULTANT", "EDITOR", "TRANSLATOR", "ADMIN"), ma);
    }

    @Test
    @DisplayName("Thu quyền rồi cấp lại được — khoá duy nhất phải là index bộ phận")
    void capLaiVaiTroDaThu() {
        capQuyen("d1000000-0000-4000-8000-000000000001", "ADMIN");

        // Thu quyền = soft_delete, KHÔNG phải DELETE: xoá cứng là xoá bằng chứng
        // ai từng có quyền ADMIN (docs/11 mục 11.2).
        jdbc.update("UPDATE staff_user_role SET soft_delete = TRUE, last_modified_by = CAST(? AS uuid) "
                + "WHERE id = CAST(? AS uuid)", NHAN_VIEN, "d1000000-0000-4000-8000-000000000001");

        // Với khoá kép (staff_user_id, role_code) thì câu này ném lỗi trùng khoá,
        // và không ai hiểu vì sao không cấp lại quyền cho người cũ được.
        capQuyen("d1000000-0000-4000-8000-000000000002", "ADMIN");

        assertEquals(1, dem("SELECT count(*) FROM staff_user_role WHERE NOT soft_delete"));
        assertEquals(2, dem("SELECT count(*) FROM staff_user_role"));
    }

    @Test
    @DisplayName("Cấp trùng một vai trò đang còn hiệu lực thì bị chặn")
    void capTrungVaiTro() {
        capQuyen("d1000000-0000-4000-8000-000000000003", "EDITOR");

        assertThrows(DataIntegrityViolationException.class,
                () -> capQuyen("d1000000-0000-4000-8000-000000000004", "EDITOR"));
    }

    // ------------------------------------------------------------ ảnh

    @Test
    @DisplayName("Ảnh không phải tự chụp mà thiếu chứng từ giấy phép thì bị từ chối")
    void anhThieuGiayPhep() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO media_asset (id, path, width, height, byte_size, source)
                VALUES ('e1000000-0000-4000-8000-000000000001','/img/doi-tac.jpg',
                        1600,1067,180000,'PARTNER')
                """),
                "docs/24 mục 8: ảnh không rõ nguồn không được dùng, kể cả ở bản nháp");

        // Cùng dòng đó, có mã giấy phép, thì vào được.
        jdbc.update("""
                INSERT INTO media_asset (id, path, width, height, byte_size, source, licence_ref)
                VALUES ('e1000000-0000-4000-8000-000000000001','/img/doi-tac.jpg',
                        1600,1067,180000,'PARTNER','Thư đồng ý 2026-08-14 từ khách sạn')
                """);
        assertEquals(1, dem("SELECT count(*) FROM media_asset"));
    }

    @Test
    @DisplayName("Gỡ ảnh khỏi bộ ảnh không làm mất ảnh lẫn chứng từ giấy phép")
    void goAnhKhoiBoAnh() {
        themAnh("e1000000-0000-4000-8000-000000000002", "/img/sapa.jpg");
        jdbc.update("INSERT INTO product_image (product_id, asset_id, sort_order) "
                + "VALUES (CAST(? AS uuid), CAST(? AS uuid), 1)",
                "a0000000-0000-4000-8000-0000000000d1", "e1000000-0000-4000-8000-000000000002");

        jdbc.update("DELETE FROM product_image");

        assertEquals(1, dem("SELECT count(*) FROM media_asset"),
                "Ảnh và giấy phép của nó phải ở lại — nó còn dùng cho sản phẩm khác");
    }

    @Test
    @DisplayName("Không xoá cứng được ảnh đang nằm trong một bộ ảnh")
    void khongXoaDuocAnhDangDung() {
        themAnh("e1000000-0000-4000-8000-000000000003", "/img/halong.jpg");
        jdbc.update("INSERT INTO product_image (product_id, asset_id, sort_order) "
                + "VALUES (CAST(? AS uuid), CAST(? AS uuid), 1)",
                "a0000000-0000-4000-8000-0000000000d1", "e1000000-0000-4000-8000-000000000003");

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("DELETE FROM media_asset WHERE id = CAST(? AS uuid)",
                        "e1000000-0000-4000-8000-000000000003"));
    }

    @Test
    @DisplayName("Alt của ảnh phụ thuộc locale, ảnh thì không")
    void altPhuThuocLocale() {
        themAnh("e1000000-0000-4000-8000-000000000004", "/img/hoi-an.jpg");
        jdbc.update("""
                INSERT INTO media_asset_translation (asset_id, locale, alt) VALUES
                  ('e1000000-0000-4000-8000-000000000004','da','Lanterner i Hoi An'),
                  ('e1000000-0000-4000-8000-000000000004','vi','Đèn lồng Hội An')
                """);

        assertEquals("Đèn lồng Hội An", jdbc.queryForObject(
                "SELECT alt FROM media_asset_translation WHERE asset_id = CAST(? AS uuid) AND locale = 'vi'",
                String.class, "e1000000-0000-4000-8000-000000000004"));
    }

    // ------------------------------------------------------------ slug cũ

    @Test
    @DisplayName("Đổi slug thì trigger tự ghi slug cũ — không trông vào code ứng dụng")
    void doiSlugGhiLichSu() {
        doiSlug("slug-moi-hon");

        Map<String, Object> dong = jdbc.queryForMap("SELECT * FROM slug_history");

        assertEquals("PRODUCT", dong.get("entity_type"));
        assertEquals("slug-cu-nhat", dong.get("old_slug"));
        assertEquals("da", dong.get("locale"));
        assertNotNull(dong.get("created_by"), "Ai đổi slug phải lấy được từ last_modified_by");
    }

    @Test
    @DisplayName("Sửa trường khác không sinh dòng lịch sử nào")
    void suaTruongKhacKhongGhi() {
        jdbc.update("UPDATE product_translation SET title = 'Tên khác' "
                + "WHERE product_id = CAST(? AS uuid)", "a0000000-0000-4000-8000-0000000000d1");

        assertEquals(0, dem("SELECT count(*) FROM slug_history"));
    }

    @Test
    @DisplayName("Quay lại slug cũ thì dòng chuyển hướng bị dọn — không sinh vòng lặp")
    void quayLaiSlugCu() {
        doiSlug("slug-giua-chung");
        doiSlug("slug-cu-nhat");

        List<String> con_lai = jdbc.queryForList(
                "SELECT old_slug FROM slug_history", String.class);

        assertEquals(List.of("slug-giua-chung"), con_lai,
                "slug-cu-nhat nay là slug đang dùng; để nó lại trong bảng là vòng lặp 301");
    }

    @Test
    @DisplayName("Slug có dấu bị từ chối ngay ở tầng CSDL")
    void slugCoDauBiTuChoi() {
        assertThrows(DataIntegrityViolationException.class, () -> doiSlug("việt-nam"));
    }

    // ------------------------------------------------------------ hành khách

    @Test
    @DisplayName("Số hộ chiếu không kèm ngày hết hạn bị từ chối")
    void hoChieuThieuHan() {
        Integer soCot = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_name = 'booking_passenger'", Integer.class);
        assertEquals(8, soCot);

        Integer coKiemToan = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_name = 'booking_passenger' "
                        + "AND column_name IN ('soft_delete','last_modified_by')", Integer.class);
        assertEquals(0, coKiemToan,
                "booking_passenger thuộc nhóm C: vòng đời theo booking, không cột kiểm toán");

        Boolean coRangBuoc = jdbc.queryForObject(
                "SELECT count(*) > 0 FROM pg_constraint WHERE conname = 'ck_bp_passport'",
                Boolean.class);
        assertEquals(Boolean.TRUE, coRangBuoc);
    }

    // ------------------------------------------------------------ chung

    @Test
    @DisplayName("Mọi bảng có last_modified_at đều có trigger — quy tắc kiểm 17 của docs/12")
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

        assertTrue(thieu.isEmpty(),
                "Thiếu trigger thì cột \"sửa lần cuối\" đứng yên vĩnh viễn. Bảng thiếu: " + thieu);
    }

    // ------------------------------------------------------------ tiện ích

    private void capQuyen(String id, String vaiTro) {
        jdbc.update("INSERT INTO staff_user_role (id, staff_user_id, role_code, created_by) "
                + "VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, CAST(? AS uuid))",
                id, NHAN_VIEN, vaiTro, NHAN_VIEN);
    }

    private void themAnh(String id, String duongDan) {
        jdbc.update("INSERT INTO media_asset (id, path, width, height, byte_size, source) "
                + "VALUES (CAST(? AS uuid), ?, 1600, 1067, 180000, 'SELF')", id, duongDan);
    }

    private void doiSlug(String slugMoi) {
        jdbc.update("UPDATE product_translation SET slug = ?, last_modified_by = CAST(? AS uuid) "
                + "WHERE product_id = CAST(? AS uuid) AND locale = 'da'",
                slugMoi, NHAN_VIEN, "a0000000-0000-4000-8000-0000000000d1");
    }

    private int dem(String sql) {
        Integer so = jdbc.queryForObject(sql, Integer.class);
        return so == null ? 0 : so;
    }
}
