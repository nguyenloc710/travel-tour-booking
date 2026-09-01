package vn.travel.booking.infrastructure.region;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.application.region.RegionQueryPort;
import vn.travel.booking.application.region.RegionSummary;

import java.util.List;

/**
 * Truy vấn listing dùng SQL thuần, không JPA — docs/10 mục 6.
 */
@Repository
public class JdbcRegionQueryAdapter implements RegionQueryPort {

    /**
     * {@code INNER JOIN} với bảng dịch <b>chính là</b> chính sách không-fallback.
     * Miền thiếu bản dịch tự rơi khỏi kết quả, không cần điều kiện {@code if} nào
     * ở tầng ứng dụng. Đổi thành {@code LEFT JOIN} là phá chính sách của
     * docs/02 mục 4 mà không có test nào đỏ ngay lập tức.
     *
     * <p>Số sản phẩm đếm bằng {@code COUNT(DISTINCT p.id)}: một tour ghé nhiều
     * điểm đến trong cùng một miền chỉ được tính một lần.
     *
     * <p>{@code product_market} là cổng chặn thị trường — sản phẩm chưa xuất bản
     * ở thị trường này không được tính vào số của thị trường này.
     *
     * <p><b>Mọi bảng có {@code soft_delete} đều phải lọc.</b> Xoá mềm hoạt động
     * âm thầm: quên một chỗ là rò dữ liệu đã xoá ra khách mà không có lỗi nào
     * nổ — đúng tính chất mà ADR-003 đã bác bỏ khi từ chối Hibernate
     * {@code @Filter}. Điều kiện phải nhìn thấy được trong câu truy vấn.
     */
    private static final String SQL = """
            SELECT rt.slug,
                   rt.name,
                   COALESCE(pc.cnt, 0) AS product_count
            FROM region r
            JOIN region_translation rt
              ON rt.region_id = r.id
             AND rt.locale = ?
             AND NOT rt.soft_delete
            LEFT JOIN LATERAL (
              SELECT COUNT(DISTINCT p.id) AS cnt
              FROM product p
              JOIN destination d
                ON d.id = p.primary_destination_id
               AND d.region_id = r.id
               AND NOT d.soft_delete
              JOIN product_market pm
                ON pm.product_id = p.id
               AND pm.market = ?
               AND pm.is_published
              JOIN product_translation pt
                ON pt.product_id = p.id
               AND pt.locale = ?
               AND pt.status = 'PUBLISHED'
               AND NOT pt.soft_delete
              WHERE NOT p.soft_delete
            ) pc ON TRUE
            WHERE NOT r.soft_delete
            ORDER BY r.sort_order
            """;

    private final JdbcTemplate jdbc;

    public JdbcRegionQueryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RegionSummary> findRegions(String market, String locale) {
        return jdbc.query(
                SQL,
                (rs, i) -> new RegionSummary(
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getInt("product_count")),
                locale, market, locale);
    }
}
