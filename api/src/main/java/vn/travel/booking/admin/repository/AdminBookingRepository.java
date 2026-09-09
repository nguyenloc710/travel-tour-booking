package vn.travel.booking.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.admin.dto.AdminBookingDetailView;
import vn.travel.booking.admin.dto.AdminBookingQuery;
import vn.travel.booking.admin.dto.AdminBookingRow;
import vn.travel.booking.admin.dto.BookingEventRow;
import vn.travel.booking.admin.dto.BookingPassengerRow;
import vn.travel.booking.booking.dto.BookingStatus;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.pricing.dto.PriceBreakdown;
import vn.travel.booking.pricing.dto.PriceLine;
import vn.travel.booking.pricing.dto.PriceLineKind;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Đơn đặt nhìn từ trang quản trị — danh sách (docs/22 M6) và chi tiết (M7).
 *
 * <p>Đường <b>đọc</b>, nên SQL thuần chứ không JPA (docs/10 mục 6). Ba chỗ khác
 * hẳn bề mặt công khai tra đơn, và cả ba đều cố ý:
 *
 * <ul>
 *   <li>Không đòi email khớp. Bề mặt công khai gộp "sai mã" với "sai email" để
 *       không thành kênh dò mã đơn; ở đây người dùng đã đăng nhập và có vai trò.
 *   <li>Không giới hạn một thị trường. Nhân viên trực tổng đài cho cả hai.
 *       Tổng đài không hỏi khách "anh đặt ở thị trường nào".
 *   <li>Trả cả hộ chiếu, quốc tịch và <b>toàn bộ</b> nhật ký — thứ khách không
 *       cần thấy và người dò mã đơn không được thấy.
 * </ul>
 *
 * <p>Điều kiện duy nhất giữ nguyên là {@code NOT b.soft_delete}: xoá mềm áp cho
 * cả hai bề mặt, và ADR-003 đòi nó nhìn thấy được trong câu truy vấn.
 */
@Repository
public class AdminBookingRepository {

    /**
     * {@code booking_passenger} đếm bằng truy vấn con chứ không {@code JOIN}:
     * {@code JOIN} nhân số dòng lên theo số hành khách và {@code totalItems}
     * đếm sai theo — cùng cái bẫy đã dính một lần với thẻ và chủ đề.
     */
    private static final String BASE_FROM = """
            FROM booking b
            JOIN market m ON m.code = b.market
            LEFT JOIN departure d ON d.id = b.departure_id
            WHERE NOT b.soft_delete
            """;

    private final JdbcTemplate jdbc;

    public AdminBookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PagedResult<AdminBookingRow> findBookings(AdminBookingQuery query) {
        List<Object> params = new ArrayList<>();
        String filterClause = buildFilterClause(query, params);

        Long total = jdbc.queryForObject(
                "SELECT count(*) " + BASE_FROM + filterClause, Long.class, params.toArray());
        long totalItems = total == null ? 0L : total;

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(query.size());
        pageParams.add((long) query.page() * query.size());

        List<AdminBookingRow> rows = jdbc.query(
                """
                SELECT b.id, b.reference, b.status, b.market, b.locale, b.product_title,
                       b.total, b.currency, b.contact_email, b.created_at,
                       d.depart_date, m.fraction_digits,
                       (SELECT count(*) FROM booking_passenger bp
                         WHERE bp.booking_id = b.id) AS pax_count
                """
                        + BASE_FROM + filterClause
                        // created_at DESC là thứ tự vận hành: đơn mới về nằm trên.
                        // Tiêu chí phụ theo id để hai lần gọi cùng một trang ra
                        // cùng thứ tự — thiếu nó thì phân trang offset lặp hoặc
                        // bỏ sót bản ghi khi hai đơn trùng mốc thời gian.
                        + "ORDER BY b.created_at DESC, b.id\n"
                        + "LIMIT ? OFFSET ?",
                (rs, i) -> {
                    String currency = rs.getString("currency");
                    int fractionDigits = rs.getInt("fraction_digits");
                    Date departDate = rs.getDate("depart_date");
                    return new AdminBookingRow(
                            rs.getObject("id", UUID.class),
                            rs.getString("reference"),
                            BookingStatus.valueOf(rs.getString("status")),
                            rs.getString("market"),
                            rs.getString("locale"),
                            rs.getString("product_title"),
                            departDate == null ? null : departDate.toLocalDate(),
                            rs.getInt("pax_count"),
                            new Money(rs.getBigDecimal("total"), currency).round(fractionDigits),
                            rs.getString("contact_email"),
                            rs.getObject("created_at", OffsetDateTime.class));
                },
                pageParams.toArray());

        return new PagedResult<>(rows, query.page(), query.size(), totalItems);
    }

    /**
     * <b>RowMapper chứ không {@code queryForMap}.</b> {@code queryForMap} trả
     * kiểu mặc định của driver: một cột {@code timestamptz} ra
     * {@code java.sql.Timestamp}, và ép thẳng sang {@code OffsetDateTime} nổ
     * lúc chạy chứ không lúc biên dịch. {@code rs.getObject(cot, kieu)} để
     * driver tự chuyển, và sai kiểu thì lộ ra ngay ở dòng đó.
     */
    public Optional<AdminBookingDetailView> findByReference(String reference) {
        List<AdminBookingDetailView> found = jdbc.query("""
                SELECT b.id, b.reference, b.status, b.market, b.locale,
                       b.product_id, b.product_title, b.departure_id,
                       b.total, b.deposit, b.currency,
                       b.contact_email, b.contact_phone, b.created_at,
                       d.depart_date, m.fraction_digits
                FROM booking b
                JOIN market m ON m.code = b.market
                LEFT JOIN departure d ON d.id = b.departure_id
                WHERE b.reference = ? AND NOT b.soft_delete
                """,
                (rs, i) -> {
                    UUID id = rs.getObject("id", UUID.class);
                    String currency = rs.getString("currency");
                    int fractionDigits = rs.getInt("fraction_digits");
                    Money total = new Money(rs.getBigDecimal("total"), currency);
                    Money deposit = new Money(rs.getBigDecimal("deposit"), currency);

                    return new AdminBookingDetailView(
                            id,
                            rs.getString("reference"),
                            BookingStatus.valueOf(rs.getString("status")),
                            rs.getString("market"),
                            rs.getString("locale"),
                            rs.getObject("product_id", UUID.class),
                            rs.getString("product_title"),
                            rs.getObject("departure_id", UUID.class),
                            toLocalDate(rs.getDate("depart_date")),
                            rs.getString("contact_email"),
                            rs.getString("contact_phone"),
                            rs.getObject("created_at", OffsetDateTime.class),
                            new PriceBreakdown(priceLines(id, currency), total.round(fractionDigits), deposit.round(fractionDigits),
                                    total.minus(deposit).round(fractionDigits)),
                            passengers(id),
                            events(id));
                },
                reference);

        return found.stream().findFirst();
    }

    // ------------------------------------------------------------ đường ghi
    //
    // JdbcTemplate chứ không JPA, khác quy ước chung của đường ghi quản trị
    // (docs/10 mục 6) — có lý do, không phải tiện tay:
    //
    //   · departure.seats_booked đã được đường đặt tour công khai cộng bằng một
    //     câu UPDATE tương đối (`= seats_booked + ?`). Trừ nó bằng JPA nghĩa là
    //     hai cơ chế ghi khác nhau trên cùng một bộ đếm tồn kho, và cái nào
    //     thắng thì phụ thuộc thứ tự flush.
    //   · booking_event là bảng chỉ ghi thêm, không có last_modified lẫn
    //     soft_delete. Dựng một entity cho nó là mời người sau gọi save() lần
    //     hai trên cùng một dòng nhật ký.
    //   · Bước chuyển cần khoá bi quan trên đúng dòng booking.

    /** Ảnh chụp một đơn vừa đủ để quyết bước chuyển. */
    public record BookingLockView(UUID id, BookingStatus status, UUID departureId, int paxCount) {
    }

    /**
     * Khoá dòng đơn rồi đọc trạng thái hiện tại.
     *
     * <p>{@code FOR UPDATE} là thứ duy nhất chặn được hai nhân viên cùng bấm một
     * nút: người thứ hai chờ ở đây, đọc được trạng thái <b>sau</b> khi người thứ
     * nhất đã ghi, và rơi đúng vào máy trạng thái. Thiếu nó thì cả hai cùng đọc
     * {@code CONFIRMED}, cùng thấy hợp lệ, và một lần huỷ trừ chỗ <b>hai lần</b>.
     */
    public Optional<BookingLockView> lockBooking(String reference) {
        return jdbc.query("""
                SELECT b.id, b.status, b.departure_id,
                       (SELECT count(*) FROM booking_passenger bp
                         WHERE bp.booking_id = b.id) AS pax_count
                FROM booking b
                WHERE b.reference = ? AND NOT b.soft_delete
                FOR UPDATE OF b
                """,
                (rs, i) -> new BookingLockView(
                        rs.getObject("id", UUID.class),
                        BookingStatus.valueOf(rs.getString("status")),
                        rs.getObject("departure_id", UUID.class),
                        rs.getInt("pax_count")),
                reference)
                .stream().findFirst();
    }

    /**
     * {@code last_modified_by} do ứng dụng ghi, {@code last_modified_at} do
     * trigger — api/CLAUDE.md mục 7b. Đặt tay cột thời gian ở đây là tạo ra chỗ
     * thứ hai cùng ghi một cột.
     */
    public void setStatus(UUID bookingId, BookingStatus toStatus, UUID staffUserId) {
        jdbc.update("UPDATE booking SET status = ?, last_modified_by = ? WHERE id = ?",
                toStatus.name(), staffUserId, bookingId);
    }

    public void writeAuditLog(UUID bookingId, BookingStatus fromStatus, BookingStatus toStatus,
                              UUID staffUserId, String note) {
        jdbc.update("""
                INSERT INTO booking_event (id, booking_id, from_status, to_status,
                                           actor_type, actor_id, note)
                VALUES (?, ?, ?, ?, 'STAFF', ?, ?)
                """,
                UUID.randomUUID(), bookingId, fromStatus == null ? null : fromStatus.name(), toStatus.name(),
                staffUserId, note);
    }

    /**
     * Trả chỗ về kho.
     *
     * <p>{@code GREATEST(..., 0)} là lưới an toàn, không phải phép tính: bộ đếm
     * âm làm mọi phép kiểm còn chỗ ở nơi khác đọc ra số vô nghĩa, và hỏng âm
     * thầm. Nếu nó chạm 0 mà đáng lẽ không nên thì dữ liệu đã lệch từ trước —
     * xem ghi chú ở {@code AdminBookingService.doiTrangThai}.
     */
    public void releaseSeats(UUID departureId, int paxCount) {
        jdbc.update("""
                UPDATE departure
                SET seats_booked = GREATEST(seats_booked - ?, 0)
                WHERE id = ?
                """, paxCount, departureId);
    }

    // ------------------------------------------------------------ ba khối con

    private List<PriceLine> priceLines(UUID bookingId, String currency) {
        return jdbc.query("""
                SELECT line_key, label_key, quantity, unit_amount, amount
                FROM booking_line WHERE booking_id = ? ORDER BY seq
                """,
                (rs, i) -> new PriceLine(
                        PriceLineKind.valueOf(rs.getString("line_key")),
                        rs.getString("label_key"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("unit_amount") == null ? null
                                : new Money(rs.getBigDecimal("unit_amount"), currency),
                        new Money(rs.getBigDecimal("amount"), currency)),
                bookingId);
    }

    private List<BookingPassengerRow> passengers(UUID bookingId) {
        return jdbc.query("""
                SELECT bp.seq, pt.code AS pax_type_code, bp.full_name, bp.date_of_birth,
                       bp.passport_no, bp.passport_expiry, bp.nationality
                FROM booking_passenger bp
                JOIN pax_type pt ON pt.id = bp.pax_type_id
                WHERE bp.booking_id = ?
                ORDER BY bp.seq
                """,
                (rs, i) -> new BookingPassengerRow(
                        rs.getInt("seq"),
                        rs.getString("pax_type_code"),
                        rs.getString("full_name"),
                        toLocalDate(rs.getDate("date_of_birth")),
                        rs.getString("passport_no"),
                        toLocalDate(rs.getDate("passport_expiry")),
                        rs.getString("nationality")),
                bookingId);
    }

    /**
     * Toàn bộ nhật ký, <b>cũ nhất trước</b> — đọc từ trên xuống là đọc câu
     * chuyện của đơn theo đúng thứ tự nó xảy ra.
     *
     * <p>{@code LEFT JOIN staff_user}: dòng do {@code CUSTOMER} hay
     * {@code SYSTEM} sinh ra không có nhân viên nào, và đó là phần lớn nhật ký
     * của một đơn bình thường.
     */
    private List<BookingEventRow> events(UUID bookingId) {
        return jdbc.query("""
                SELECT e.id, e.from_status, e.to_status, e.actor_type, e.actor_id,
                       s.display_name, e.note, e.created_at
                FROM booking_event e
                LEFT JOIN staff_user s ON s.id = e.actor_id
                WHERE e.booking_id = ?
                ORDER BY e.created_at, e.id
                """,
                (rs, i) -> {
                    String fromStatus = rs.getString("from_status");
                    return new BookingEventRow(
                            rs.getObject("id", UUID.class),
                            fromStatus == null ? null : BookingStatus.valueOf(fromStatus),
                            BookingStatus.valueOf(rs.getString("to_status")),
                            rs.getString("actor_type"),
                            rs.getObject("actor_id", UUID.class),
                            rs.getString("display_name"),
                            rs.getString("note"),
                            rs.getObject("created_at", OffsetDateTime.class));
                },
                bookingId);
    }

    private static LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    // ------------------------------------------------------------ lọc

    private static String buildFilterClause(AdminBookingQuery query, List<Object> params) {
        StringBuilder sb = new StringBuilder();

        // status tường minh thắng scope. Xem AdminBookingQuery.
        if (query.status() != null) {
            sb.append(" AND b.status = ?");
            params.add(query.status());
        } else if (!"ALL".equals(query.scope())) {
            String placeholders = AdminBookingQuery.NEEDS_ACTION_STATUSES.stream()
                    .map(x -> "?").collect(Collectors.joining(","));
            sb.append(" AND b.status IN (").append(placeholders).append(')');
            params.addAll(AdminBookingQuery.NEEDS_ACTION_STATUSES);
        }
        if (query.market() != null) {
            sb.append(" AND b.market = ?");
            params.add(query.market());
        }
        // Biên ngày dựng ở Java theo UTC chứ không để Postgres tự ép date sang
        // timestamptz: phép ép đó dùng múi giờ của PHIÊN, nên cùng một câu truy
        // vấn ra kết quả khác nhau tuỳ máy chủ — và không ai phát hiện cho tới
        // khi một đơn đặt lúc nửa đêm rơi nhầm sang ngày hôm trước.
        if (query.from() != null) {
            sb.append(" AND b.created_at >= ?");
            params.add(query.from().atStartOfDay().atOffset(ZoneOffset.UTC));
        }
        if (query.to() != null) {
            sb.append(" AND b.created_at < ?");
            params.add(query.to().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        }
        if (query.q() != null && !query.q().isBlank()) {
            // Mã tra cứu HOẶC email: tổng đài có trong tay một trong hai, và
            // không biết mình đang cầm cái nào cho tới khi gõ xong.
            sb.append(" AND (b.reference ILIKE '%' || ? || '%'"
                    + " OR b.contact_email ILIKE '%' || ? || '%')");
            params.add(query.q().trim());
            params.add(query.q().trim());
        }
        return sb.isEmpty() ? "" : sb.append('\n').toString();
    }
}
