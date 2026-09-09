package vn.travel.booking.booking.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.exception.BookingErrors;
import vn.travel.booking.booking.repository.SeatHoldRepository;
import vn.travel.booking.booking.dto.SeatHoldView;
import vn.travel.booking.common.exception.NotFoundException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Giữ chỗ bằng <b>khoá bi quan</b> — docs/14 mục 6.2.
 *
 * <p>Đây là chỗ quan trọng nhất của cả hệ thống về mặt đúng đắn: hai khách cùng
 * mua được chỗ cuối cùng là bug hạng nhất của mọi hệ thống đặt chỗ.
 */
@Repository
public class SeatHoldRepository {

    private final JdbcTemplate jdbc;

    public SeatHoldRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ba bước, trong <b>một</b> transaction:
     *
     * <ol>
     *   <li>{@code SELECT ... FOR UPDATE} khoá dòng {@code departure} — mọi phiên
     *       khác xếp hàng ở đây
     *   <li>Cộng chỗ đang giữ còn hạn
     *   <li>Đủ chỗ thì chèn, không đủ thì {@code DEPARTURE_SOLD_OUT}
     * </ol>
     *
     * <p><b>Không bao giờ khoá ở tầng ứng dụng</b> ({@code synchronized}, khoá
     * trong bộ nhớ): chạy hai instance là hỏng ngay, và không có gì báo.
     *
     * <p>Transaction này ngắn — vài mili giây — nên khoá không giữ qua thao tác
     * của người dùng.
     */
    @Transactional
    public SeatHoldView hold(UUID departureId, int seats, String sessionRef, Duration holdDuration) {
        Map<String, Object> d;
        try {
            d = jdbc.queryForMap("""
                    SELECT capacity, seats_booked, base_status
                    FROM departure
                    WHERE id = ? AND NOT soft_delete
                    FOR UPDATE
                    """, departureId);
        } catch (EmptyResultDataAccessException ex) {
            throw new NotFoundException("departure id=" + departureId);
        }

        if ("PENDING".equals(d.get("base_status")) || "SOLD_OUT".equals(d.get("base_status"))) {
            throw new BookingErrors.DepartureClosed("ngày khởi hành đang " + d.get("base_status"));
        }

        int capacity = ((Number) d.get("capacity")).intValue();
        int daDat = ((Number) d.get("seats_booked")).intValue();

        // Vế thứ ba. Quên nó là để hai khách cùng mua được chỗ cuối cùng.
        Integer dangGiu = jdbc.queryForObject("""
                SELECT COALESCE(SUM(seats), 0)
                FROM seat_hold
                WHERE departure_id = ?
                  AND released_at IS NULL
                  AND expires_at > now()
                """, Integer.class, departureId);

        int conLai = capacity - daDat - (dangGiu == null ? 0 : dangGiu);
        if (conLai < seats) {
            throw new BookingErrors.DepartureSoldOut(
                    "còn " + conLai + " chỗ, cần " + seats);
        }

        UUID id = UUID.randomUUID();
        OffsetDateTime hetHan = jdbc.queryForObject(
                "SELECT now() + CAST(? AS interval)", OffsetDateTime.class,
                holdDuration.toMinutes() + " minutes");

        jdbc.update("""
                INSERT INTO seat_hold (id, departure_id, seats, session_ref, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, departureId, seats, sessionRef, hetHan);

        return new SeatHoldView(id, departureId, seats, hetHan);
    }

    /**
     * Trả chỗ về kho. Gọi lại nhiều lần không hại: điều kiện
     * {@code released_at IS NULL} làm câu lệnh bất biến khi lặp.
     */
    @Transactional
    public void release(UUID seatHoldId) {
        jdbc.update("""
                UPDATE seat_hold SET released_at = now()
                WHERE id = ? AND released_at IS NULL
                """, seatHoldId);
    }
}
