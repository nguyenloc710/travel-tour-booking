package vn.travel.booking.infrastructure.destination;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.application.destination.DestinationQueryPort;
import vn.travel.booking.application.destination.DestinationSummary;
import vn.travel.booking.application.product.NamedRef;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn điểm đến bằng SQL thuần — docs/10 mục 6.
 *
 * <p>Cùng ba điều kiện chính sách như adapter sản phẩm, vì cùng lý do: bản dịch
 * {@code INNER JOIN} nên điểm đến chưa dịch tự rơi khỏi locale đó; số sản phẩm
 * đi qua cổng chặn thị trường {@code product_market}; và mọi bảng có
 * {@code soft_delete} đều lọc tường minh.
 */
@Repository
public class JdbcDestinationQueryAdapter implements DestinationQueryPort {

    /**
     * Số sản phẩm đếm bằng truy vấn con tương quan chứ không bằng {@code JOIN}
     * rồi {@code GROUP BY}: điểm đến <b>chưa có sản phẩm nào</b> vẫn phải trả về
     * với số 0. Với {@code JOIN} thì nó biến mất, và trang điểm đến vừa viết
     * xong sẽ không bao giờ hiện ra — lỗi im lặng, không ai báo.
     */
    private static final String NGUON = """
            SELECT dt.slug,
                   dt.name,
                   dt.summary,
                   rt.slug AS region_slug,
                   rt.name AS region_name,
                   (SELECT count(*)
                      FROM product p
                      JOIN product_market pm
                        ON pm.product_id = p.id
                       AND pm.market = ?
                       AND pm.is_published
                      JOIN product_translation pt
                        ON pt.product_id = p.id
                       AND pt.locale = ?
                       AND pt.status = 'PUBLISHED'
                       AND NOT pt.soft_delete
                     WHERE p.primary_destination_id = d.id
                       AND NOT p.soft_delete) AS product_count
            FROM destination d
            JOIN destination_translation dt
              ON dt.destination_id = d.id
             AND dt.locale = ?
             AND NOT dt.soft_delete
            JOIN region r
              ON r.id = d.region_id
             AND NOT r.soft_delete
            JOIN region_translation rt
              ON rt.region_id = r.id
             AND rt.locale = ?
             AND NOT rt.soft_delete
            WHERE NOT d.soft_delete
            """;

    private final JdbcTemplate jdbc;

    public JdbcDestinationQueryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<DestinationSummary> findDestinations(String market, String locale, String regionSlug) {
        List<Object> thamSo = new ArrayList<>(List.of(market, locale, locale, locale));
        String loc = "";

        if (regionSlug != null && !regionSlug.isBlank()) {
            loc = " AND rt.slug = ?";
            thamSo.add(regionSlug);
        }

        // Sắp theo miền rồi tới thứ tự trong miền: danh sách đọc được như một
        // hành trình từ Bắc vào Nam, không phải một mớ theo bảng chữ cái.
        return jdbc.query(
                NGUON + loc + " ORDER BY r.sort_order, d.sort_order, dt.slug",
                (rs, i) -> doc(rs),
                thamSo.toArray());
    }

    @Override
    public Optional<DestinationSummary> findDestination(String market, String locale, String slug) {
        return jdbc.query(
                        NGUON + " AND dt.slug = ?",
                        (rs, i) -> doc(rs),
                        market, locale, locale, locale, slug)
                .stream()
                .findFirst();
    }

    private static DestinationSummary doc(ResultSet rs) throws SQLException {
        return new DestinationSummary(
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("summary"),
                new NamedRef(rs.getString("region_slug"), rs.getString("region_name")),
                rs.getInt("product_count"));
    }
}
