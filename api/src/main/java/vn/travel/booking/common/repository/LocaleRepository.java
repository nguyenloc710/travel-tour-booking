package vn.travel.booking.common.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Bảng {@code locale} — dữ liệu tra cứu, nạp bằng migration {@code R__}.
 *
 * <p>Ngôn ngữ nguồn <b>không hardcode</b>: nó đọc từ cột {@code is_source}, cùng
 * nguồn sự thật mà ADR-004 và ràng buộc {@code ux_locale_single_source} cưỡng
 * chế. Viết {@code "da"} thẳng trong truy vấn là để hai chỗ cùng nói một chuyện,
 * và ngày đổi ngôn ngữ nguồn thì một trong hai chỗ sẽ bị quên.
 */
@Repository
public class LocaleRepository {

    private final JdbcTemplate jdbc;

    public LocaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String localeNguon() {
        return jdbc.queryForObject(
                "SELECT code FROM locale WHERE is_source AND is_active", String.class);
    }

    /** Mọi locale đang bật <b>trừ</b> ngôn ngữ nguồn — tức là những thứ phải dịch. */
    public List<String> localeDich() {
        return jdbc.queryForList(
                "SELECT code FROM locale WHERE is_active AND NOT is_source ORDER BY code",
                String.class);
    }
}
