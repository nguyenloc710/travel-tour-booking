package vn.travel.booking.market.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    /**
     * Tiền tệ và số chữ số thập phân của một thị trường.
     *
     * <p>Đường ghi của trang quản trị lấy tiền tệ <b>từ đây</b>, không từ client:
     * cho client gửi tiền tệ là mở đường nhập giá DKK vào thị trường {@code VN},
     * và không ràng buộc CSDL nào bắt được chuyện đó.
     */
    public Optional<CauHinh> cauHinh(String market) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT currency, fraction_digits FROM market WHERE code = ? AND is_active",
                    (rs, i) -> new CauHinh(rs.getString("currency"), rs.getInt("fraction_digits")),
                    market));
        } catch (EmptyResultDataAccessException khong_co) {
            return Optional.empty();
        }
    }

    /** VND có 0 chữ số thập phân, DKK có 2 — không hardcode ở đâu khác. */
    public record CauHinh(String currency, int fractionDigits) {
    }
}
