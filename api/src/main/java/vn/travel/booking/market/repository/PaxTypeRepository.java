package vn.travel.booking.market.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Loại khách của một thị trường.
 *
 * <p><b>{@code pax_type} là dữ liệu riêng từng thị trường</b>, không phải danh
 * sách chung: {@code (market, code)} mới là khoá duy nhất. Cùng mã
 * {@code CHILD_5_11} ở hai thị trường là hai dòng khác nhau với hai mức giảm
 * khác nhau.
 *
 * <p>Bảng này <b>cố tình chưa được nạp bằng migration</b>: loại khách của từng
 * thị trường là dữ liệu nghiệp vụ chưa chốt (Q-2). Nạp số bịa vào migration là
 * biến phỏng đoán thành sự thật của hệ thống, nên hiện nó chỉ có trong
 * {@code seed-dev.sql} — nơi đã ghi rõ là dữ liệu giả.
 */
@Repository
public class PaxTypeRepository {

    private final JdbcTemplate jdbc;

    public PaxTypeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mã loại khách sang id, trong phạm vi một thị trường. Sắp theo thứ tự hiển thị. */
    public Map<String, UUID> byCode(String market) {
        Map<String, UUID> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT code, id FROM pax_type
                WHERE market = ? AND NOT soft_delete
                ORDER BY sort_order
                """,
                rs -> {
                    result.put(rs.getString("code"), rs.getObject("id", UUID.class));
                },
                market);
        return result;
    }

    /** Id sang mã — để trả về bảng giá bằng mã người đọc được, không phải UUID. */
    public Map<UUID, String> byId(String market) {
        Map<UUID, String> result = new LinkedHashMap<>();
        byCode(market).forEach((code, id) -> result.put(id, code));
        return result;
    }
}
