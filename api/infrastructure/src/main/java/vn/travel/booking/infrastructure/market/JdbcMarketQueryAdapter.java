package vn.travel.booking.infrastructure.market;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.application.market.MarketQueryPort;

@Repository
public class JdbcMarketQueryAdapter implements MarketQueryPort {

    private final JdbcTemplate jdbc;

    public JdbcMarketQueryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Bảng {@code market} có đúng hai dòng và gần như không bao giờ đổi, nhưng
     * vẫn hỏi CSDL mỗi lần thay vì nhét danh sách vào code: tắt một thị trường
     * là việc vận hành làm bằng một câu {@code UPDATE}, không phải việc phải
     * triển khai lại ứng dụng.
     */
    @Override
    public boolean isActive(String market) {
        Integer so = jdbc.queryForObject(
                "SELECT count(*) FROM market WHERE code = ? AND is_active",
                Integer.class, market);
        return so != null && so > 0;
    }
}
