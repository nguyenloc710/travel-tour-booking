package vn.travel.booking.application.lecture;

import java.time.LocalDate;
import java.util.List;

public interface LectureQueryPort {

    /**
     * Chỉ buổi từ {@code homNay} trở đi.
     *
     * <p>"Hôm nay" là <b>tham số</b>, không đọc từ đồng hồ trong adapter: cùng
     * lý do với quy tắc "domain không đọc đồng hồ hệ thống" ở api/CLAUDE.md — test
     * phải cố định được ngày, nếu không nó sẽ đỏ vào một ngày nào đó trong tương lai.
     */
    List<LectureSummary> findUpcoming(String market, String locale, LocalDate homNay);
}
