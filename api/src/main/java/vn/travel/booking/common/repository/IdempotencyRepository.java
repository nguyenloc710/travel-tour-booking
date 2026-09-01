package vn.travel.booking.common.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class IdempotencyRepository {

    /**
     * @param requestHash vân tay thân yêu cầu. Cùng khoá nhưng thân khác là lỗi
     *                    phía client dùng lại khoá cho việc khác, không phải một
     *                    lần gọi lại — trả kết quả cũ khi đó là im lặng nuốt mất
     *                    một đơn hàng thật.
     */
    public record StoredResponse(String requestHash, int statusCode, String responseJson) {
    }

    private final JdbcTemplate jdbc;

    public IdempotencyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Bản ghi quá hạn coi như không có: khoá chỉ sống 24 giờ (docs/13 mục 7). */
    public Optional<StoredResponse> find(UUID key) {
        try {
            Map<String, Object> d = jdbc.queryForMap("""
                    SELECT request_hash, status_code, response::text AS response
                    FROM idempotency_key
                    WHERE key = ? AND expires_at > now()
                    """, key);

            return Optional.of(new StoredResponse(
                    ((String) d.get("request_hash")).trim(),
                    ((Number) d.get("status_code")).intValue(),
                    (String) d.get("response")));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * {@code ON CONFLICT DO NOTHING}: hai yêu cầu song song cùng khoá thì đúng
     * một cái ghi được, cái kia đọc lại kết quả của cái đầu. Không có nhánh
     * {@code DO UPDATE} — một khoá đã trả kết quả nào thì vĩnh viễn trả kết quả đó.
     */
    public void save(UUID key, String market, String endpoint, String requestHash,
                     int statusCode, String responseJson, Duration ttl) {
        jdbc.update("""
                INSERT INTO idempotency_key (key, market, endpoint, request_hash,
                                             status_code, response, expires_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), now() + CAST(? AS interval))
                ON CONFLICT (key) DO NOTHING
                """,
                key, market, endpoint, requestHash, statusCode, responseJson,
                ttl.toHours() + " hours");
    }
}
