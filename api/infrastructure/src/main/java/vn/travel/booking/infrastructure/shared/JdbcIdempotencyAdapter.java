package vn.travel.booking.infrastructure.shared;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.application.shared.IdempotencyPort;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcIdempotencyAdapter implements IdempotencyPort {

    private final JdbcTemplate jdbc;

    public JdbcIdempotencyAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Bản ghi quá hạn coi như không có: khoá chỉ sống 24 giờ (docs/13 mục 7). */
    @Override
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
    @Override
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
