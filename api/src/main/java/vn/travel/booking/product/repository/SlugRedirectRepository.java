package vn.travel.booking.product.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Slug cũ trỏ tới slug nào bây giờ — đường đọc của bảng {@code slug_history}.
 *
 * <p><b>Chỉ trả slug mới khi bản ghi đích thật sự xem được ở
 * {@code (market, locale)} này.</b> Truy vấn vì thế lặp lại đúng những điều kiện
 * của đường đọc công khai — đã dịch, đã gán và đang bán ở thị trường này, chưa
 * xoá mềm. Thiếu điều kiện nào thì chuyển hướng dẫn tới một trang {@code 404},
 * và một vòng chuyển hướng tới ngõ cụt tệ hơn hẳn một {@code 404} thẳng.
 *
 * <p>Điều kiện xoá mềm viết tường minh ở đây, không dùng cơ chế ẩn — ADR-003.
 */
@Repository
public class SlugRedirectRepository {

    private final JdbcTemplate jdbc;

    public SlugRedirectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Sản phẩm: điều kiện thị trường là {@code pm.is_published}, giống hệt
     * listing. Sản phẩm đã gỡ khỏi thị trường thì slug cũ của nó cũng không dẫn
     * đi đâu ở thị trường đó.
     */
    public Optional<String> product(String market, String locale, String slugCu) {
        return mot("""
                SELECT pt.slug
                FROM slug_history h
                JOIN product p
                  ON p.id = h.entity_id AND NOT p.soft_delete
                JOIN product_translation pt
                  ON pt.product_id = p.id
                 AND pt.locale = h.locale
                 AND pt.status = 'PUBLISHED'
                 AND NOT pt.soft_delete
                JOIN product_market pm
                  ON pm.product_id = p.id AND pm.market = ? AND pm.is_published
                WHERE h.entity_type = 'PRODUCT'
                  AND h.locale = ?
                  AND h.old_slug = ?
                """, market, locale, slugCu);
    }

    /**
     * Điểm đến: không có cổng chặn thị trường vì bản thân điểm đến không gán vào
     * thị trường — chỉ sản phẩm mới gán. Tham số {@code market} vẫn nhận để chữ
     * ký hai phương thức giống nhau và chỗ gọi không phải nhớ ngoại lệ.
     */
    public Optional<String> destination(String market, String locale, String slugCu) {
        return mot("""
                SELECT dt.slug
                FROM slug_history h
                JOIN destination d
                  ON d.id = h.entity_id AND NOT d.soft_delete
                JOIN destination_translation dt
                  ON dt.destination_id = d.id
                 AND dt.locale = h.locale
                 AND NOT dt.soft_delete
                JOIN region r
                  ON r.id = d.region_id AND NOT r.soft_delete
                WHERE h.entity_type = 'DESTINATION'
                  AND h.locale = ?
                  AND h.old_slug = ?
                """, locale, slugCu);
    }

    private Optional<String> mot(String sql, Object... params) {
        return jdbc.query(sql, (rs, i) -> rs.getString(1), params).stream().findFirst();
    }
}
