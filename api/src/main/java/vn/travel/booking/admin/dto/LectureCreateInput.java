package vn.travel.booking.admin.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tạo một buổi thuyết trình.
 *
 * <p>{@code market} chứ không locale: buổi ở København thuộc thị trường
 * {@code DK} dù nói bằng tiếng gì. Bản dịch của nó thì vẫn có cả hai locale —
 * đây đúng là chỗ hai khái niệm đó dễ bị gộp nhất.
 */
public record LectureCreateInput(
        String market,
        LocalDate eventDate,
        LocalTime startTime,
        String city,
        String venue,
        int seats,
        LectureTranslationInput translation) {
}
