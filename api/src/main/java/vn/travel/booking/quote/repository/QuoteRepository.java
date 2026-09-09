package vn.travel.booking.quote.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.quote.dto.AdminQuoteDetailView;
import vn.travel.booking.quote.dto.AdminQuoteQuery;
import vn.travel.booking.quote.dto.AdminQuoteRow;
import vn.travel.booking.quote.dto.QuoteLineDraft;
import vn.travel.booking.quote.dto.QuoteLineRow;
import vn.travel.booking.quote.dto.QuoteProduct;
import vn.travel.booking.quote.dto.QuoteReceiptView;
import vn.travel.booking.quote.dto.QuoteRequestCommand;
import vn.travel.booking.quote.dto.QuoteStatus;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Báo giá — đường ghi của khách và cả hai đường của trang quản trị.
 *
 * <p>SQL thuần chứ không JPA, cùng ba lý do với {@code AdminBookingRepository}:
 * {@code quote_line} là bảng chỉ thay sạch chứ không sửa từng dòng, bước chuyển
 * trạng thái cần khoá bi quan, và đường đọc của M8 là truy vấn có phân trang.
 */
@Repository
public class QuoteRepository {

    /** Cùng bộ chữ với mã đơn: không có I, O, 0, 1 — khách đọc mã qua điện thoại. */
    private static final char[] CHU_CAI = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom NGAU_NHIEN = new SecureRandom();

    /**
     * Nguồn dùng chung cho danh sách, đếm tổng và chi tiết.
     *
     * <p>{@code JOIN product_translation} theo <b>locale của báo giá</b>, không
     * theo locale nguồn: tư vấn viên mở màn hình này để gọi cho khách, nên tên
     * tour phải là tên khách đã nhìn thấy lúc gửi yêu cầu. Đây là {@code JOIN}
     * chứ không {@code LEFT JOIN} — báo giá chỉ sinh ra được từ một sản phẩm đã
     * dịch, nên không có dòng nào rơi ra vì thiếu bản dịch.
     */
    private static final String NGUON = """
            FROM quote q
            JOIN market m ON m.code = q.market
            JOIN product_translation pt
              ON pt.product_id = q.product_id AND pt.locale = q.locale
             AND NOT pt.soft_delete
            WHERE NOT q.soft_delete
            """;

    private final JdbcTemplate jdbc;

    public QuoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------ tra sản phẩm

    /**
     * Tra sản phẩm theo slug <b>của locale đang đọc</b>, trong <b>thị trường
     * đang xem</b>.
     *
     * <p>Ba điều kiện, và bỏ bất cứ cái nào cũng làm rò rỉ đúng thứ chính sách
     * không-fallback sinh ra để giấu: bản dịch phải {@code PUBLISHED}, sản phẩm
     * phải được gán và bật bán ở thị trường này, và cả hai bảng đều chưa xoá
     * mềm. Thiếu chúng thì gửi được yêu cầu báo giá cho một tour chưa mở bán.
     *
     * <p>{@code LEFT JOIN product_private}: loại khác vẫn tra ra được, để tầng
     * trên phân biệt "không có tour này" với "tour này không hỏi giá" — hai câu
     * trả lời khác nhau cho khách.
     */
    public Optional<QuoteProduct> findProduct(String market, String locale, String slug) {
        return jdbc.query("""
                SELECT p.id, p.product_type, pt.title,
                       pp.lead_time_days, pp.quote_valid_days
                FROM product p
                JOIN product_translation pt
                  ON pt.product_id = p.id AND pt.locale = ?
                 AND pt.status = 'PUBLISHED' AND NOT pt.soft_delete
                JOIN product_market pm
                  ON pm.product_id = p.id AND pm.market = ? AND pm.is_published
                LEFT JOIN product_private pp ON pp.product_id = p.id
                WHERE pt.slug = ? AND NOT p.soft_delete
                """,
                (rs, i) -> new QuoteProduct(
                        rs.getObject("id", UUID.class),
                        rs.getString("product_type"),
                        rs.getString("title"),
                        rs.getInt("lead_time_days"),
                        rs.getInt("quote_valid_days")),
                locale, market, slug)
                .stream().findFirst();
    }

    // ------------------------------------------------------ khách gửi yêu cầu

    /**
     * Ghi yêu cầu thành một {@code quote} ở {@code DRAFT}.
     *
     * <p>{@code created_by} để {@code NULL}: v1 không có tài khoản khách, nên
     * không có ai để đứng tên (docs/11 mục 11.1). Cùng quy ước với {@code
     * booking} do khách tự đặt.
     */
    public QuoteReceiptView createRequest(String market, String locale, UUID productId,
                                      QuoteRequestCommand command) {

        UUID id = UUID.randomUUID();
        String reference = sinhMa(market);

        OffsetDateTime createdAt = jdbc.queryForObject("""
                INSERT INTO quote (id, reference, product_id, market, locale, status, party_size,
                                   contact_name, contact_email, contact_phone,
                                   requested_date, message)
                VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?)
                RETURNING created_at
                """,
                OffsetDateTime.class,
                id, reference, productId, market, locale, command.partySize(),
                command.contactName(), command.contactEmail(), command.contactPhone(),
                command.requestedDate() == null ? null : Date.valueOf(command.requestedDate()),
                command.message());

        return new QuoteReceiptView(reference, QuoteStatus.DRAFT, createdAt);
    }

    // ------------------------------------------------------ đường đọc M8

    public PagedResult<AdminQuoteRow> list(AdminQuoteQuery query) {
        List<Object> params = new ArrayList<>();
        String loc = buildFilterClause(query, params);

        Long tong = jdbc.queryForObject(
                "SELECT count(*) " + NGUON + loc, Long.class, params.toArray());
        long totalItems = tong == null ? 0L : tong;

        List<Object> thamSoTrang = new ArrayList<>(params);
        thamSoTrang.add(query.size());
        thamSoTrang.add((long) query.page() * query.size());

        List<AdminQuoteRow> row = jdbc.query(
                CHON_DONG + NGUON + loc
                        // CŨ NHẤT TRƯỚC — ngược với danh sách đơn, và có chủ ý:
                        // đây là hàng đợi việc, mà việc chờ lâu nhất thì gấp
                        // nhất. Tiêu chí phụ theo id để phân trang ổn định.
                        + "ORDER BY q.created_at, q.id\n"
                        + "LIMIT ? OFFSET ?",
                QuoteRepository::readRow,
                thamSoTrang.toArray());

        return new PagedResult<>(row, query.page(), query.size(), totalItems);
    }

    /**
     * Chi tiết một báo giá.
     *
     * <p>Truy vấn viết đủ chứ không ghép từ {@link #NGUON}: nó cần thêm
     * {@code LEFT JOIN product_private} ở giữa mệnh đề {@code FROM}, và ghép
     * chuỗi để chèn vào giữa là thứ đọc lên không ai biết câu SQL cuối cùng
     * trông ra sao.
     */
    public Optional<AdminQuoteDetailView> findByCode(String reference) {
        return jdbc.query("""
                SELECT q.id, q.reference, q.status, q.market, q.locale, q.product_id,
                       pt.title, q.party_size, q.requested_date, q.contact_name,
                       q.contact_email, q.total, q.valid_until, q.created_at,
                       m.currency, m.fraction_digits,
                       q.contact_phone, q.message,
                       COALESCE(pp.lead_time_days, 0)   AS lead_time_days,
                       COALESCE(pp.quote_valid_days, 0) AS quote_valid_days
                FROM quote q
                JOIN market m ON m.code = q.market
                JOIN product_translation pt
                  ON pt.product_id = q.product_id AND pt.locale = q.locale
                 AND NOT pt.soft_delete
                LEFT JOIN product_private pp ON pp.product_id = q.product_id
                WHERE q.reference = ? AND NOT q.soft_delete
                """,
                (rs, i) -> {
                    AdminQuoteRow tomTat = readRow(rs, i);
                    return new AdminQuoteDetailView(
                            tomTat,
                            rs.getString("contact_phone"),
                            rs.getString("message"),
                            rs.getInt("lead_time_days"),
                            rs.getInt("quote_valid_days"),
                            dongGia(tomTat.id(), rs.getString("currency"),
                                    rs.getInt("fraction_digits")));
                },
                reference)
                .stream().findFirst();
    }


    // ------------------------------------------------------ đường ghi M8

    /** Ảnh chụp một báo giá vừa đủ để quyết bước chuyển. */
    public record BaoGiaDeDoi(UUID id, QuoteStatus status, String market, String currency,
                              int fractionDigits, int quoteValidDays, LocalDate validUntil,
                              int rowCount) {
    }

    /**
     * Khoá dòng báo giá rồi đọc trạng thái hiện tại.
     *
     * <p>Cùng lý do với {@code khoaDon}: thiếu {@code FOR UPDATE} thì hai tư vấn
     * viên cùng bấm "Gửi" đều đọc {@code DRAFT}, đều thấy hợp lệ, và khách nhận
     * hai báo giá mang hai hạn khác nhau cho cùng một chuyến đi.
     *
     * <p>{@code FOR UPDATE OF q} chứ không {@code FOR UPDATE} trống: câu này
     * {@code JOIN} sang {@code market} và {@code product_private}, và khoá luôn
     * hai bảng tra cứu đó là khoá mọi báo giá khác cùng thị trường.
     */
    public Optional<BaoGiaDeDoi> khoaBaoGia(String reference) {
        return jdbc.query("""
                SELECT q.id, q.status, q.market, q.valid_until,
                       m.currency, m.fraction_digits,
                       COALESCE(pp.quote_valid_days, 0) AS quote_valid_days,
                       (SELECT count(*) FROM quote_line ql WHERE ql.quote_id = q.id) AS so_dong
                FROM quote q
                JOIN market m ON m.code = q.market
                LEFT JOIN product_private pp ON pp.product_id = q.product_id
                WHERE q.reference = ? AND NOT q.soft_delete
                FOR UPDATE OF q
                """,
                (rs, i) -> new BaoGiaDeDoi(
                        rs.getObject("id", UUID.class),
                        QuoteStatus.valueOf(rs.getString("status")),
                        rs.getString("market"),
                        rs.getString("currency"),
                        rs.getInt("fraction_digits"),
                        rs.getInt("quote_valid_days"),
                        ngay(rs.getDate("valid_until")),
                        rs.getInt("so_dong")),
                reference)
                .stream().findFirst();
    }

    /**
     * Thay sạch bảng giá và ghi lại tổng.
     *
     * <p>Xoá hết rồi chèn lại chứ không so từng dòng: bảng giá đang dựng thì thứ
     * tự đổi liên tục, và một thuật toán so khớp ở đây chỉ để tránh vài lệnh
     * {@code INSERT} trên một bảng có nhiều nhất bốn chục dòng.
     *
     * <p>{@code total} do <b>máy chủ cộng</b>, không nhận từ client — nhận rồi
     * tin là mở đường cho một báo giá mà tổng không khớp bảng.
     */
    public void setQuoteLines(UUID quoteId, String currency, List<QuoteLineDraft> row,
                           UUID staffUserId) {
        jdbc.update("DELETE FROM quote_line WHERE quote_id = ?", quoteId);

        BigDecimal tong = BigDecimal.ZERO;
        int seq = 1;
        for (QuoteLineDraft d : row) {
            jdbc.update("""
                    INSERT INTO quote_line (quote_id, seq, label_key, quantity, unit_amount, amount)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    quoteId, seq++, d.labelKey(), d.quantity(), d.unitAmount(), d.amount());
            tong = tong.add(d.amount());
        }

        // last_modified_by do ứng dụng ghi, last_modified_at do trigger —
        // api/CLAUDE.md mục 7b.
        jdbc.update("UPDATE quote SET total = ?, currency = ?, last_modified_by = ? WHERE id = ?",
                tong, currency, staffUserId, quoteId);
    }

    /**
     * Ghi trạng thái mới.
     *
     * <p>{@code sentAt} và {@code validUntil} chỉ khác {@code null} ở bước gửi;
     * hai bước sau giữ nguyên giá trị cũ, nên câu {@code UPDATE} dùng
     * {@code COALESCE} thay vì hai câu riêng — ràng buộc {@code ck_quote_sent}
     * bắt mọi trạng thái sau {@code DRAFT} phải có cả hai.
     */
    public void setStatus(UUID quoteId, QuoteStatus sang, UUID staffUserId,
                             OffsetDateTime sentAt, LocalDate validUntil) {
        jdbc.update("""
                UPDATE quote
                SET status = ?,
                    sent_at = COALESCE(sent_at, ?),
                    valid_until = COALESCE(valid_until, ?),
                    last_modified_by = ?
                WHERE id = ?
                """,
                sang.name(), sentAt, validUntil == null ? null : Date.valueOf(validUntil),
                staffUserId, quoteId);
    }

    /**
     * Job quét hạn: {@code SENT} đã quá {@code valid_until} thành {@code EXPIRED}.
     *
     * <p>Một câu {@code UPDATE} theo điều kiện chứ không đọc rồi ghi từng dòng:
     * bất biến khi lặp, và không có khoảng trống nào giữa lúc đọc và lúc ghi.
     *
     * @return số báo giá vừa hết hạn
     */
    public int hetHan(LocalDate homNay) {
        return jdbc.update("""
                UPDATE quote SET status = 'EXPIRED'
                WHERE status = 'SENT' AND valid_until < ? AND NOT soft_delete
                """, Date.valueOf(homNay));
    }

    // ------------------------------------------------------------ nội bộ

    private static final String CHON_DONG = """
            SELECT q.id, q.reference, q.status, q.market, q.locale, q.product_id,
                   pt.title, q.party_size, q.requested_date, q.contact_name,
                   q.contact_email, q.total, q.valid_until, q.created_at,
                   m.currency, m.fraction_digits
            """;

    private static AdminQuoteRow readRow(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        BigDecimal tong = rs.getBigDecimal("total");
        String currency = rs.getString("currency");

        return new AdminQuoteRow(
                rs.getObject("id", UUID.class),
                rs.getString("reference"),
                QuoteStatus.valueOf(rs.getString("status")),
                rs.getString("market"),
                rs.getString("locale"),
                rs.getObject("product_id", UUID.class),
                rs.getString("title"),
                rs.getInt("party_size"),
                ngay(rs.getDate("requested_date")),
                rs.getString("contact_name"),
                rs.getString("contact_email"),
                // Chưa dựng bảng giá thì KHÔNG có tổng — trả 0 ở đây là bịa ra
                // một con số mà màn hình sẽ hiển thị như một báo giá miễn phí.
                tong == null ? null : new Money(tong, currency).round(rs.getInt("fraction_digits")),
                ngay(rs.getDate("valid_until")),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    private List<QuoteLineRow> dongGia(UUID quoteId, String currency, int fractionDigits) {
        return jdbc.query("""
                SELECT seq, label_key, quantity, unit_amount, amount
                FROM quote_line WHERE quote_id = ? ORDER BY seq
                """,
                (rs, i) -> new QuoteLineRow(
                        rs.getInt("seq"),
                        rs.getString("label_key"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("unit_amount") == null ? null
                                : new Money(rs.getBigDecimal("unit_amount"), currency).round(fractionDigits),
                        new Money(rs.getBigDecimal("amount"), currency).round(fractionDigits)),
                quoteId);
    }

    private static String buildFilterClause(AdminQuoteQuery query, List<Object> params) {
        StringBuilder sb = new StringBuilder();

        if (query.status() != null && !AdminQuoteQuery.TAT_CA.equals(query.status())) {
            sb.append(" AND q.status = ?");
            params.add(query.status());
        }
        if (query.market() != null) {
            sb.append(" AND q.market = ?");
            params.add(query.market());
        }
        if (query.q() != null && !query.q().isBlank()) {
            // Mã, tên, hoặc email: tổng đài cầm một trong ba và không biết mình
            // đang cầm cái nào cho tới khi gõ xong.
            sb.append(" AND (q.reference ILIKE '%' || ? || '%'"
                    + " OR q.contact_name ILIKE '%' || ? || '%'"
                    + " OR q.contact_email ILIKE '%' || ? || '%')");
            String tu = query.q().trim();
            params.add(tu);
            params.add(tu);
            params.add(tu);
        }
        return sb.isEmpty() ? "" : sb.append('\n').toString();
    }

    private static LocalDate ngay(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    /** {@code Q-DK-2026-8F3K2P} — tiền tố Q để không lẫn với mã đơn ở tổng đài. */
    private String sinhMa(String market) {
        for (int lan = 0; lan < 5; lan++) {
            StringBuilder duoi = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                duoi.append(CHU_CAI[NGAU_NHIEN.nextInt(CHU_CAI.length)]);
            }
            String ma = "Q-" + market + "-" + LocalDate.now().getYear() + "-" + duoi;

            Integer trung = jdbc.queryForObject(
                    "SELECT count(*) FROM quote WHERE reference = ?", Integer.class, ma);
            if (trung != null && trung == 0) {
                return ma;
            }
        }
        throw new IllegalStateException("không sinh được mã báo giá không trùng sau 5 lần");
    }
}
