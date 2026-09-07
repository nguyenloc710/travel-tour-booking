package vn.travel.booking.destination.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.destination.repository.DestinationRepository;
import vn.travel.booking.common.config.DiaChiKho;
import vn.travel.booking.destination.dto.DestinationSummary;
import vn.travel.booking.product.dto.GalleryImage;
import vn.travel.booking.product.dto.NamedRef;

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
public class DestinationRepository {

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
                   anh.path AS anh_path,
                   anh.alt  AS anh_alt,
                   anh.width  AS anh_width,
                   anh.height AS anh_height,
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
            -- Ảnh minh hoạ: tấm đầu theo sort_order. LEFT JOIN LATERAL chứ không
            -- một truy vấn riêng cho mỗi dòng — danh sách này trả cả mười điểm
            -- đến một lần, và N+1 ở đây là mười lần đi lại CSDL cho một trang.
            --
            -- LEFT chứ không INNER: điểm đến chưa có ảnh vẫn phải hiện ra. Đây
            -- đúng là lỗi mà chú thích về product_count ở trên đã cảnh báo, chỉ
            -- khác chỗ.
            LEFT JOIN LATERAL (
              SELECT a.path, a.width, a.height, t.alt
              FROM destination_image di
              JOIN media_asset a
                ON a.id = di.asset_id
               AND NOT a.soft_delete
              JOIN media_asset_translation t
                ON t.asset_id = a.id
               AND t.locale = ?
               AND NOT t.soft_delete
              WHERE di.destination_id = d.id
              ORDER BY di.sort_order
              LIMIT 1
            ) anh ON TRUE
            WHERE NOT d.soft_delete
            """;

    private final JdbcTemplate jdbc;
    private final DiaChiKho diaChiKho;

    public DestinationRepository(JdbcTemplate jdbc, DiaChiKho diaChiKho) {
        this.jdbc = jdbc;
        this.diaChiKho = diaChiKho;
    }
    public List<DestinationSummary> findDestinations(String market, String locale, String regionSlug) {
        // Năm tham số của NGUON, đúng thứ tự dấu ? xuất hiện: market và locale của
        // truy vấn đếm, rồi locale của ba phép JOIN dịch — điểm đến, miền, ảnh.
        List<Object> thamSo = new ArrayList<>(List.of(market, locale, locale, locale, locale));
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
    public Optional<DestinationSummary> findDestination(String market, String locale, String slug) {
        return jdbc.query(
                        NGUON + " AND dt.slug = ?",
                        (rs, i) -> doc(rs),
                        market, locale, locale, locale, locale, slug)
                .stream()
                .findFirst();
    }

    private DestinationSummary doc(ResultSet rs) throws SQLException {
        return new DestinationSummary(
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("summary"),
                new NamedRef(rs.getString("region_slug"), rs.getString("region_name")),
                anh(rs),
                rs.getInt("product_count"));
    }

    /** Không có ảnh thì LATERAL trả toàn NULL, và cả khối thành null. */
    private GalleryImage anh(ResultSet rs) throws SQLException {
        String path = rs.getString("anh_path");
        if (path == null) {
            return null;
        }
        return new GalleryImage(
                diaChiKho.diaChiCua(path),
                rs.getString("anh_alt"),
                rs.getInt("anh_width"),
                rs.getInt("anh_height"));
    }
}
