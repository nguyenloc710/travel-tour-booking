package vn.travel.booking.product.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.product.dto.DepartureView;
import vn.travel.booking.product.dto.HotelStay;
import vn.travel.booking.product.dto.ItineraryDay;
import vn.travel.booking.product.dto.NamedRef;
import vn.travel.booking.product.repository.ProductContentRepository;
import vn.travel.booking.product.dto.ProductType;
import vn.travel.booking.product.dto.VisibleProduct;
import vn.travel.booking.departure.dto.BaseDepartureStatus;
import vn.travel.booking.departure.service.DepartureStatuses;
import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Nội dung con của một sản phẩm: lịch trình, chặng nghỉ, ngày khởi hành.
 *
 * <p>Cùng ba điều kiện chính sách như adapter danh mục — bản dịch
 * {@code INNER JOIN}, cổng chặn thị trường, và lọc {@code soft_delete} tường minh
 * ở mọi bảng có cột đó.
 */
@Repository
public class ProductContentRepository {

    private final JdbcTemplate jdbc;

    public ProductContentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    public Optional<VisibleProduct> findVisibleProduct(String market, String locale, String slug) {
        return jdbc.query("""
                SELECT p.id, p.product_type
                FROM product p
                JOIN product_market pm
                  ON pm.product_id = p.id AND pm.market = ? AND pm.is_published
                JOIN market m
                  ON m.code = pm.market AND m.is_active
                JOIN product_translation pt
                  ON pt.product_id = p.id
                 AND pt.locale = ?
                 AND pt.slug = ?
                 AND pt.status = 'PUBLISHED'
                 AND NOT pt.soft_delete
                WHERE NOT p.soft_delete
                """,
                (rs, i) -> new VisibleProduct(
                        rs.getObject("id", UUID.class),
                        ProductType.valueOf(rs.getString("product_type"))),
                market, locale, slug)
                .stream().findFirst();
    }

    /**
     * Bản dịch của <b>ngày lịch trình</b> là {@code INNER JOIN}: ngày chưa dịch
     * biến mất khỏi locale đó. Đây là chỗ chính sách không-fallback có hệ quả
     * khó chịu nhất — lịch trình thủng ngày ở giữa — nên quy tắc kiểm 3 của
     * docs/12 mục 9 đối chiếu số ngày với {@code duration_days} và bắt đúng
     * trường hợp này trước khi khách nhìn thấy.
     *
     * <p>Bản dịch của <b>điểm đến</b> thì {@code LEFT JOIN}: thiếu tên điểm đến
     * chỉ làm mất một dòng phụ, không làm mất cả ngày trong hành trình.
     */
    public List<ItineraryDay> findItinerary(UUID productId, String locale) {
        return jdbc.query("""
                SELECT d.day_number,
                       t.title,
                       t.description,
                       dt.slug AS destination_slug,
                       dt.name AS destination_name,
                       h.name  AS hotel_name
                FROM itinerary_day d
                JOIN itinerary_day_translation t
                  ON t.itinerary_day_id = d.id
                 AND t.locale = ?
                 AND NOT t.soft_delete
                LEFT JOIN destination dest
                  ON dest.id = d.destination_id AND NOT dest.soft_delete
                LEFT JOIN destination_translation dt
                  ON dt.destination_id = dest.id AND dt.locale = ? AND NOT dt.soft_delete
                LEFT JOIN hotel h
                  ON h.id = d.hotel_id AND NOT h.soft_delete
                WHERE d.product_id = ? AND NOT d.soft_delete
                ORDER BY d.day_number
                """,
                (rs, i) -> new ItineraryDay(
                        rs.getInt("day_number"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("destination_slug") == null ? null
                                : new NamedRef(rs.getString("destination_slug"),
                                               rs.getString("destination_name")),
                        rs.getString("hotel_name")),
                locale, locale, productId);
    }

    /**
     * Mô tả khách sạn {@code LEFT JOIN}: <b>tên khách sạn không dịch</b>
     * (docs/24 mục 5), nên chặng nghỉ vẫn hiện được khi chưa có bản mô tả —
     * khách vẫn biết mình ngủ ở đâu, mấy đêm.
     */
    public List<HotelStay> findHotelStays(UUID productId, String locale) {
        return jdbc.query("""
                SELECT h.name,
                       h.stars,
                       h.image,
                       s.nights,
                       dt.slug AS destination_slug,
                       dt.name AS destination_name,
                       ht.description
                FROM product_hotel_stay s
                JOIN hotel h
                  ON h.id = s.hotel_id AND NOT h.soft_delete
                JOIN destination dest
                  ON dest.id = h.destination_id AND NOT dest.soft_delete
                JOIN destination_translation dt
                  ON dt.destination_id = dest.id AND dt.locale = ? AND NOT dt.soft_delete
                LEFT JOIN hotel_translation ht
                  ON ht.hotel_id = h.id AND ht.locale = ? AND NOT ht.soft_delete
                WHERE s.product_id = ?
                ORDER BY s.sort_order, h.name
                """,
                (rs, i) -> new HotelStay(
                        rs.getString("name"),
                        intOrNull(rs.getInt("stars"), rs.wasNull()),
                        rs.getInt("nights"),
                        new NamedRef(rs.getString("destination_slug"), rs.getString("destination_name")),
                        rs.getString("description"),
                        rs.getString("image")),
                locale, locale, productId);
    }

    /**
     * Số chỗ khả dụng trừ <b>ba</b> thứ, không phải hai: sức chứa, chỗ đã đặt,
     * và <b>chỗ đang giữ còn hạn</b>. Quên vế thứ ba là để hai khách cùng mua
     * được chỗ cuối cùng — bug hạng nhất của mọi hệ thống đặt chỗ
     * (docs/14 mục 6.1).
     *
     * <p>Điều kiện {@code expires_at > now()} nghĩa là chỗ về kho <b>ngay khi hết
     * hạn</b>, không chờ job quét dọn chạy.
     *
     * <p>Trạng thái không lấy từ cột nào: cột {@code base_status} đi vào hàm
     * thuần {@code DepartureStatuses.resolve} cùng số chỗ, và kết quả mới là thứ
     * khách thấy.
     */
    public List<DepartureView> findDepartures(UUID productId, String market, int fewSeatsThreshold) {
        return jdbc.query("""
                SELECT d.id,
                       d.depart_date,
                       d.return_date,
                       d.days,
                       d.base_status,
                       d.cabin_category,
                       d.capacity,
                       d.seats_booked,
                       COALESCE((SELECT SUM(sh.seats) FROM seat_hold sh
                                  WHERE sh.departure_id = d.id
                                    AND sh.released_at IS NULL
                                    AND sh.expires_at > now()), 0) AS held,
                       g.guaranteed_threshold,
                       o.city AS departure_city,
                       (SELECT MIN(dp.amount) FROM departure_price dp
                         WHERE dp.departure_id = d.id) AS price_from,
                       m.currency,
                       m.fraction_digits
                FROM departure d
                JOIN market m
                  ON m.code = d.market AND m.is_active
                LEFT JOIN product_group_tour g
                  ON g.product_id = d.product_id
                LEFT JOIN departure_origin o
                  ON o.id = d.departure_origin_id AND NOT o.soft_delete
                WHERE d.product_id = ? AND d.market = ? AND NOT d.soft_delete
                ORDER BY d.depart_date, d.cabin_category NULLS FIRST
                """,
                (rs, i) -> {
                    int capacity = rs.getInt("capacity");
                    int booked = rs.getInt("seats_booked");
                    int held = rs.getInt("held");
                    int conLai = Math.max(0, capacity - booked - held);

                    int nguong = rs.getInt("guaranteed_threshold");
                    Integer guaranteed = rs.wasNull() ? null : nguong;

                    BigDecimal price = rs.getBigDecimal("price_from");

                    return new DepartureView(
                            rs.getObject("id", UUID.class),
                            rs.getObject("depart_date", java.time.LocalDate.class),
                            rs.getObject("return_date", java.time.LocalDate.class),
                            rs.getInt("days"),
                            DepartureStatuses.resolve(
                                    BaseDepartureStatus.valueOf(rs.getString("base_status")),
                                    booked, conLai, guaranteed, fewSeatsThreshold),
                            conLai,
                            price == null ? null
                                    : new Money(price, rs.getString("currency"))
                                            .round(rs.getInt("fraction_digits")),
                            rs.getString("cabin_category"),
                            rs.getString("departure_city"));
                },
                productId, market);
    }

    private static Integer intOrNull(int gia_tri, boolean rong) {
        return rong ? null : gia_tri;
    }
}
