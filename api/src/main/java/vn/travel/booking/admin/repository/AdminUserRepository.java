package vn.travel.booking.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.admin.dto.StaffUserView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Người dùng và vai trò — docs/22 M14.
 *
 * <p><b>Không có phương thức nào đọc hay ghi {@code password_hash}.</b> Đó là
 * chủ ý, không phải thiếu sót: màn hình M14 quản lý <i>ai làm được gì</i>, còn
 * mật khẩu thuộc luồng đăng nhập và đặt lại mật khẩu — hai việc khác nhau, và
 * gộp chúng vào một lớp là mở đường cho một endpoint quản trị vô tình trả băm
 * mật khẩu ra ngoài.
 */
@Repository
public class AdminUserRepository {

    private final JdbcTemplate jdbc;

    public AdminUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Mọi người dùng, kèm vai trò.
     *
     * <p>Vai trò gộp bằng truy vấn con chứ không {@code JOIN}: {@code JOIN} nhân
     * số dòng lên theo số vai trò, và một người mang ba vai trò sẽ xuất hiện ba
     * lần trong danh sách — cùng cái bẫy đã dính một lần với thẻ và chủ đề.
     *
     * <p>Trả cả người đã tắt: màn hình cần thấy họ để bật lại, và họ vẫn đứng
     * tên trong nhật ký kiểm toán.
     */
    public List<StaffUserView> findAll() {
        return jdbc.query("""
                SELECT u.id, u.email, u.display_name, u.is_active,
                       COALESCE(
                         (SELECT array_agg(r.role_code ORDER BY r.role_code)
                            FROM staff_user_role r WHERE r.staff_user_id = u.id),
                         ARRAY[]::varchar[]) AS vai_tro
                FROM staff_user u
                WHERE NOT u.soft_delete
                ORDER BY u.is_active DESC, u.display_name
                """, AdminUserRepository::doc);
    }

    public Optional<StaffUserView> find(UUID id) {
        return jdbc.query("""
                SELECT u.id, u.email, u.display_name, u.is_active,
                       COALESCE(
                         (SELECT array_agg(r.role_code ORDER BY r.role_code)
                            FROM staff_user_role r WHERE r.staff_user_id = u.id),
                         ARRAY[]::varchar[]) AS vai_tro
                FROM staff_user u
                WHERE u.id = ? AND NOT u.soft_delete
                """, AdminUserRepository::doc, id)
                .stream().findFirst();
    }

    public void patch(UUID id, String displayName, Boolean isActive, UUID nhanVienId) {
        jdbc.update("""
                UPDATE staff_user
                SET display_name = COALESCE(?, display_name),
                    is_active = COALESCE(?, is_active),
                    last_modified_by = ?
                WHERE id = ?
                """, displayName, isActive, nhanVienId, id);
    }

    /**
     * Thay <b>sạch</b> tập vai trò.
     *
     * <p>Xoá hết rồi chèn lại chứ không so từng cái: tập vai trò nhiều nhất bốn
     * phần tử, và một thuật toán so khớp ở đây chỉ để tránh vài lệnh
     * {@code INSERT} — đổi lại là một nhánh "vai trò đã có thì bỏ qua" mà không
     * ai test.
     */
    public void datVaiTro(UUID id, List<String> roles) {
        jdbc.update("DELETE FROM staff_user_role WHERE staff_user_id = ?", id);
        for (String role : roles) {
            jdbc.update("""
                    INSERT INTO staff_user_role (id, staff_user_id, role_code)
                    VALUES (?, ?, ?)
                    """, UUID.randomUUID(), id, role);
        }
    }

    /**
     * Còn bao nhiêu {@code ADMIN} <b>đang bật</b>, không tính người này.
     *
     * <p>Câu hỏi đúng phải loại người đang bị sửa ra khỏi phép đếm: hỏi "còn bao
     * nhiêu ADMIN" rồi so với 1 là đếm cả chính người sắp bị tắt, và phép so đó
     * đúng một cách tình cờ — nó vỡ ngay khi ai đó đổi dấu so sánh.
     */
    public int demAdminKhac(UUID ngoaiTru) {
        Integer so = jdbc.queryForObject("""
                SELECT count(*)
                FROM staff_user u
                JOIN staff_user_role r ON r.staff_user_id = u.id AND r.role_code = 'ADMIN'
                WHERE u.is_active AND NOT u.soft_delete AND u.id <> ?
                """, Integer.class, ngoaiTru);
        return so == null ? 0 : so;
    }

    private static StaffUserView doc(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        java.sql.Array mang = rs.getArray("vai_tro");
        List<String> roles = mang == null ? List.of() : List.of((String[]) mang.getArray());

        return new StaffUserView(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getBoolean("is_active"),
                roles);
    }
}
