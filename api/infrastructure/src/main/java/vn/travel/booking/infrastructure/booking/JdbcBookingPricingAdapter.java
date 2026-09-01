package vn.travel.booking.infrastructure.booking;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.application.booking.BookingPricingPort;
import vn.travel.booking.application.booking.DeparturePricing;
import vn.travel.booking.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gom mọi thứ cần để tính giá một ngày khởi hành.
 *
 * <p>Bốn dòng của docs/14 mục 2.1 <b>chưa đọc được từ đâu</b>: bảo hiểm, đêm
 * khách sạn trước bay, giảm đặt sớm và nâng hạng cabin. Lược đồ chưa có bảng cho
 * chúng — ghi ở docs/12 mục 10, không phải bỏ quên.
 */
@Repository
public class JdbcBookingPricingAdapter implements BookingPricingPort {

    private final JdbcTemplate jdbc;

    public JdbcBookingPricingAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<DeparturePricing> load(String market, UUID departureId, UUID departureOriginId) {
        Map<String, Object> d;
        try {
            // Ngày khởi hành thuộc về ĐÚNG MỘT thị trường (ADR-006), nên điều
            // kiện market ở đây vừa là bộ lọc vừa là hàng rào: không có cách nào
            // đặt một chuyến của thị trường kia qua đường dẫn của thị trường này.
            d = jdbc.queryForMap("""
                    SELECT dep.id, dep.product_id, dep.depart_date,
                           p.product_type,
                           pt.title AS product_title,
                           m.currency, m.fraction_digits, m.deposit_rate, m.processing_fee
                    FROM departure dep
                    JOIN product p ON p.id = dep.product_id AND NOT p.soft_delete
                    JOIN market m ON m.code = dep.market AND m.is_active
                    JOIN locale l ON l.is_source AND l.is_active
                    JOIN product_translation pt
                      ON pt.product_id = p.id AND pt.locale = l.code AND NOT pt.soft_delete
                    WHERE dep.id = ? AND dep.market = ? AND NOT dep.soft_delete
                    """, departureId, market);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }

        String tienTe = (String) d.get("currency");

        Map<String, Money> phongDoi = giaTheoLoaiKhach(departureId, "DOUBLE", tienTe);
        Map<String, Money> phongDon = giaTheoLoaiKhach(departureId, "SINGLE", tienTe);

        Money phuThuDiem = null;
        if (departureOriginId != null) {
            BigDecimal so = jdbc.queryForObject("""
                    SELECT surcharge FROM departure_origin
                    WHERE id = ? AND market = ? AND NOT soft_delete
                    """, BigDecimal.class, departureOriginId, market);
            phuThuDiem = so == null ? null : new Money(so, tienTe);
        }

        return Optional.of(new DeparturePricing(
                (UUID) d.get("id"),
                (UUID) d.get("product_id"),
                (String) d.get("product_type"),
                (String) d.get("product_title"),
                ((java.sql.Date) d.get("depart_date")).toLocalDate(),
                tienTe,
                ((Number) d.get("fraction_digits")).intValue(),
                (BigDecimal) d.get("deposit_rate"),
                new Money((BigDecimal) d.get("processing_fee"), tienTe),
                phongDoi,
                phongDon,
                phuThuDiem));
    }

    private Map<String, Money> giaTheoLoaiKhach(UUID departureId, String kieuPhong, String tienTe) {
        Map<String, Money> gia = new LinkedHashMap<>();
        jdbc.query("""
                SELECT pt.code, dp.amount
                FROM departure_price dp
                JOIN pax_type pt ON pt.id = dp.pax_type_id AND NOT pt.soft_delete
                WHERE dp.departure_id = ? AND dp.occupancy = ?
                ORDER BY pt.sort_order
                """,
                rs -> {
                    gia.put(rs.getString("code"), new Money(rs.getBigDecimal("amount"), tienTe));
                },
                departureId, kieuPhong);
        return gia;
    }

    /** Ngày khởi hành, dùng cho bản chụp trong đơn. */
    LocalDate departDate(UUID departureId) {
        return jdbc.queryForObject(
                "SELECT depart_date FROM departure WHERE id = ?", LocalDate.class, departureId);
    }
}
