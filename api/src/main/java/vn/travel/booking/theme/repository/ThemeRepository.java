package vn.travel.booking.theme.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.theme.repository.ThemeRepository;
import vn.travel.booking.theme.dto.ThemeSummary;

import java.util.List;

@Repository
public class ThemeRepository {

    private final JdbcTemplate jdbc;

    public ThemeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Đếm bằng truy vấn con tương quan, không {@code JOIN} rồi {@code GROUP BY}:
     * chủ đề chưa có sản phẩm nào vẫn phải trả về với số 0, vì nó vẫn là một lựa
     * chọn hợp lệ của bộ lọc.
     */
    public List<ThemeSummary> findThemes(String market, String locale) {
        return jdbc.query("""
                SELECT tt.slug,
                       tt.name,
                       (SELECT count(*)
                          FROM product_theme pth
                          JOIN product p ON p.id = pth.product_id AND NOT p.soft_delete
                          JOIN product_market pm
                            ON pm.product_id = p.id AND pm.market = ? AND pm.is_published
                          JOIN product_translation pt
                            ON pt.product_id = p.id
                           AND pt.locale = ?
                           AND pt.status = 'PUBLISHED'
                           AND NOT pt.soft_delete
                         WHERE pth.theme_id = t.id) AS product_count
                FROM theme t
                JOIN theme_translation tt
                  ON tt.theme_id = t.id AND tt.locale = ? AND NOT tt.soft_delete
                WHERE NOT t.soft_delete
                ORDER BY t.sort_order, tt.name
                """,
                (rs, i) -> new ThemeSummary(
                        rs.getString("slug"), rs.getString("name"), rs.getInt("product_count")),
                market, locale, locale);
    }
}
