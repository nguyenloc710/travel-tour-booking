package vn.travel.booking.booking.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.booking.dto.BookingDraft;
import vn.travel.booking.common.exception.BookingErrors;
import vn.travel.booking.booking.dto.BookingView;
import vn.travel.booking.booking.repository.BookingRepository;
import vn.travel.booking.booking.dto.PassengerDraft;
import vn.travel.booking.booking.dto.BookingStatus;
import vn.travel.booking.pricing.dto.PriceBreakdown;
import vn.travel.booking.pricing.dto.PriceLine;
import vn.travel.booking.pricing.dto.PriceLineKind;
import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tạo đơn — sáu việc trong <b>một</b> transaction, docs/14 mục 6.3.
 */
@Repository
public class BookingRepository {

    /** Không có I, O, 0, 1 — khách đọc mã này qua điện thoại cho tổng đài. */
    private static final char[] CHU_CAI = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom NGAU_NHIEN = new SecureRandom();

    private final JdbcTemplate jdbc;

    public BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    @Transactional
    public BookingView create(BookingDraft draft) {
        int soKhach = draft.tongSoKhach();

        if (draft.seatHoldId() != null) {
            khoaVaDungGiuCho(draft.seatHoldId(), draft.departureId(), soKhach);
        }

        UUID bookingId = UUID.randomUUID();
        String reference = sinhMa(draft.market());
        PriceBreakdown b = draft.breakdown();
        LocalDate ngayDi = jdbc.queryForObject(
                "SELECT depart_date FROM departure WHERE id = ?", LocalDate.class, draft.departureId());

        jdbc.update("""
                INSERT INTO booking (id, reference, market, locale, product_id, departure_id,
                                     status, product_title, total, deposit, currency,
                                     contact_email, contact_phone)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                bookingId, reference, draft.market(), draft.locale(), draft.productId(),
                draft.departureId(), BookingStatus.PENDING_PAYMENT.name(), draft.productTitle(),
                b.total().amount(), b.deposit().amount(), b.total().currency(),
                draft.contactEmail(), draft.contactPhone());

        ghiDongGia(bookingId, b);
        ghiHanhKhach(bookingId, draft.passengers());

        // Mọi lần đổi trạng thái ghi một dòng nhật ký — kể cả lần đầu tiên.
        // Đơn không có dòng nào trong booking_event là đơn không ai giải thích
        // được khi khách khiếu nại (docs/23 mục 4).
        jdbc.update("""
                INSERT INTO booking_event (id, booking_id, from_status, to_status, actor_type)
                VALUES (?, ?, NULL, ?, 'CUSTOMER')
                """, UUID.randomUUID(), bookingId, BookingStatus.PENDING_PAYMENT.name());

        return new BookingView(reference, BookingStatus.PENDING_PAYMENT,
                draft.productTitle(), ngayDi, b);
    }

    /**
     * Khoá giữ chỗ, <b>kiểm lại hạn</b>, cộng chỗ đã đặt, đánh dấu giữ chỗ đã dùng.
     *
     * <p>Kiểm lại dù tầng trên đã kiểm: thời gian trôi giữa hai lần gọi, và giữ
     * chỗ có thể vừa hết hạn (docs/14 mục 6.3 bước 2).
     */
    private void khoaVaDungGiuCho(UUID seatHoldId, UUID departureId, int soKhach) {
        Map<String, Object> h;
        try {
            h = jdbc.queryForMap("""
                    SELECT seats, departure_id, released_at, expires_at > now() AS con_han
                    FROM seat_hold WHERE id = ?
                    FOR UPDATE
                    """, seatHoldId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BookingErrors.SeatHoldExpired("không có giữ chỗ id=" + seatHoldId);
        }

        if (h.get("released_at") != null || !Boolean.TRUE.equals(h.get("con_han"))) {
            throw new BookingErrors.SeatHoldExpired("giữ chỗ đã hết hạn hoặc đã dùng");
        }
        if (!departureId.equals(h.get("departure_id"))) {
            throw new BookingErrors.SeatHoldExpired("giữ chỗ không thuộc ngày khởi hành này");
        }
        if (((Number) h.get("seats")).intValue() < soKhach) {
            throw new BookingErrors.SeatHoldExpired("giữ chỗ ít hơn số khách của đơn");
        }

        jdbc.update("UPDATE departure SET seats_booked = seats_booked + ? WHERE id = ?",
                soKhach, departureId);
        jdbc.update("UPDATE seat_hold SET released_at = now() WHERE id = ?", seatHoldId);
    }

    private void ghiDongGia(UUID bookingId, PriceBreakdown b) {
        int seq = 1;
        for (PriceLine d : b.lines()) {
            jdbc.update("""
                    INSERT INTO booking_line (booking_id, seq, line_key, label_key,
                                              quantity, unit_amount, amount)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    bookingId, seq++, d.kind().name(), d.labelKey(),
                    d.quantity(),
                    d.unitAmount() == null ? null : d.unitAmount().amount(),
                    d.amount().amount());
        }
    }

    private void ghiHanhKhach(UUID bookingId, List<PassengerDraft> khach) {
        int seq = 1;
        for (PassengerDraft k : khach) {
            jdbc.update("""
                    INSERT INTO booking_passenger (booking_id, seq, pax_type_id, full_name,
                                                   date_of_birth, nationality)
                    SELECT ?, ?, pt.id, ?, ?, ?
                    FROM pax_type pt
                    JOIN booking b ON b.id = ?
                    WHERE pt.code = ? AND pt.market = b.market AND NOT pt.soft_delete
                    """,
                    bookingId, seq++, k.fullName(),
                    k.dateOfBirth() == null ? null : Date.valueOf(k.dateOfBirth()),
                    k.nationality(), bookingId, k.paxTypeCode());
        }
    }
    public Optional<BookingView> findByReferenceAndEmail(String market, String reference, String email) {
        Map<String, Object> b;
        try {
            b = jdbc.queryForMap("""
                    SELECT b.id, b.reference, b.status, b.product_title, b.total, b.deposit,
                           b.currency, d.depart_date, m.fraction_digits
                    FROM booking b
                    JOIN market m ON m.code = b.market
                    LEFT JOIN departure d ON d.id = b.departure_id
                    WHERE b.reference = ? AND b.market = ?
                      AND lower(b.contact_email) = lower(?)
                      AND NOT b.soft_delete
                    """, reference, market, email);
        } catch (EmptyResultDataAccessException ex) {
            // Sai mã và sai email trả CÙNG một kết quả — phân biệt hai cái là
            // biến endpoint này thành kênh dò mã đơn (docs/13 mục 10).
            return Optional.empty();
        }

        String tienTe = (String) b.get("currency");
        int soLe = ((Number) b.get("fraction_digits")).intValue();
        Money tong = new Money((BigDecimal) b.get("total"), tienTe);
        Money coc = new Money((BigDecimal) b.get("deposit"), tienTe);

        List<PriceLine> dong = jdbc.query("""
                SELECT line_key, label_key, quantity, unit_amount, amount
                FROM booking_line WHERE booking_id = ? ORDER BY seq
                """,
                (rs, i) -> new PriceLine(
                        PriceLineKind.valueOf(rs.getString("line_key")),
                        rs.getString("label_key"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("unit_amount") == null ? null
                                : new Money(rs.getBigDecimal("unit_amount"), tienTe),
                        new Money(rs.getBigDecimal("amount"), tienTe)),
                b.get("id"));

        Object ngay = b.get("depart_date");

        return Optional.of(new BookingView(
                (String) b.get("reference"),
                BookingStatus.valueOf((String) b.get("status")),
                (String) b.get("product_title"),
                ngay == null ? null : ((Date) ngay).toLocalDate(),
                new PriceBreakdown(dong, tong.round(soLe), coc.round(soLe),
                        tong.minus(coc).round(soLe))));
    }

    /**
     * Mã tra cứu: {@code DK-2026-8F3K2P}.
     *
     * <p>Định dạng <b>chưa chốt</b> (docs/13 mục 12). Bộ chữ cái bỏ I, O, 0 và 1
     * vì khách đọc mã này qua điện thoại, và "I hay 1" là câu hỏi tổng đài sẽ
     * phải hỏi lại mỗi ngày.
     */
    private String sinhMa(String market) {
        StringBuilder duoi = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            duoi.append(CHU_CAI[NGAU_NHIEN.nextInt(CHU_CAI.length)]);
        }
        return market + "-" + LocalDate.now().getYear() + "-" + duoi;
    }
}
