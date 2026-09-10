package vn.travel.booking.itinerary.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tra tên điểm đến và khách sạn cho màn hình lịch trình.
 *
 * <p>SQL thuần chứ không JPA: đây là đường ĐỌC, và nó chỉ cần hai cột tên —
 * dựng entity cho khách sạn chỉ để đọc một chuỗi là mua một thứ phải bảo trì mà
 * không dùng tới (docs/10 mục 6).
 */
@Repository
public class ItineraryRefRepository {

    private final JdbcTemplate jdbc;

    public ItineraryRefRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean destinationExists(UUID id) {
        Boolean co = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM destination WHERE id = ? AND NOT soft_delete)",
                Boolean.class, id);
        return Boolean.TRUE.equals(co);
    }

    public boolean hotelExists(UUID id) {
        Boolean co = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM hotel WHERE id = ? AND NOT soft_delete)",
                Boolean.class, id);
        return Boolean.TRUE.equals(co);
    }

    /** Tên điểm đến ở ngôn ngữ nguồn, theo id. */
    public Map<UUID, String> destinationNames(String sourceLocale) {
        return jdbc.query("""
                SELECT d.id, t.name
                FROM destination d
                JOIN destination_translation t
                  ON t.destination_id = d.id AND t.locale = ? AND NOT t.soft_delete
                WHERE NOT d.soft_delete
                """,
                (rs, i) -> Map.entry((UUID) rs.getObject("id"), rs.getString("name")),
                sourceLocale)
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Tên khách sạn — tên riêng, không dịch (docs/24 mục 5). */
    public Map<UUID, String> hotelNames() {
        return jdbc.query("SELECT id, name FROM hotel WHERE NOT soft_delete",
                (rs, i) -> Map.entry((UUID) rs.getObject("id"), rs.getString("name")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
