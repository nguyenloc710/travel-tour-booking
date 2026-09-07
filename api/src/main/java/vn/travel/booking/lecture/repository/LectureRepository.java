package vn.travel.booking.lecture.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.lecture.repository.LectureRepository;
import vn.travel.booking.lecture.dto.LectureSummary;

import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class LectureRepository {

    private final JdbcTemplate jdbc;

    public LectureRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lọc buổi đã qua <b>ở truy vấn</b>, không ở tầng hiển thị (docs/13 mục 9.1).
     * Lọc ở frontend nghĩa là buổi đã qua vẫn đi qua dây, vẫn nằm trong cache, và
     * sẽ có ngày hiện ra.
     *
     * <p>Số chỗ còn lại tính ngay trong câu truy vấn từ {@code seats - seats_taken};
     * không có cột thứ ba nào lưu nó, và cũng không có cờ "đã đầy".
     */
    public List<LectureSummary> findUpcoming(String market, String locale, LocalDate homNay) {
        return jdbc.query("""
                SELECT l.id,
                       l.event_date,
                       l.start_time,
                       l.city,
                       l.venue,
                       l.seats,
                       l.seats - l.seats_taken AS seats_available,
                       lt.title,
                       lt.description
                FROM lecture l
                JOIN lecture_translation lt
                  ON lt.lecture_id = l.id AND lt.locale = ? AND NOT lt.soft_delete
                WHERE l.market = ?
                  AND l.event_date >= ?
                  AND NOT l.soft_delete
                ORDER BY l.event_date, l.start_time NULLS LAST
                """,
                (rs, i) -> {
                    Time gio = rs.getTime("start_time");
                    return new LectureSummary(
                            rs.getObject("id", UUID.class),
                            rs.getObject("event_date", LocalDate.class),
                            gio == null ? null : gio.toLocalTime(),
                            rs.getString("city"),
                            rs.getString("venue"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getInt("seats"),
                            rs.getInt("seats_available"));
                },
                locale, market, homNay);
    }
}
