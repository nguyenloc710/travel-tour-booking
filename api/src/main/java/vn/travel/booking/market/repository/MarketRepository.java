package vn.travel.booking.market.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.market.repository.MarketRepository;

@Repository
public class MarketRepository {

    private final JdbcTemplate jdbc;

    public MarketRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Bảng {@code market} có đúng hai dòng và gần như không bao giờ đổi, nhưng
     * vẫn hỏi CSDL mỗi lần thay vì nhét danh sách vào code: tắt một thị trường
     * là việc vận hành làm bằng một câu {@code UPDATE}, không phải việc phải
     * triển khai lại ứng dụng.
     */
    public boolean isActive(String market) {
        Integer so = jdbc.queryForObject(
                "SELECT count(*) FROM market WHERE code = ? AND is_active",
                Integer.class, market);
        return so != null && so > 0;
    }
}
