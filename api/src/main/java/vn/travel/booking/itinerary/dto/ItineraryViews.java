package vn.travel.booking.itinerary.dto;

import java.util.List;
import java.util.UUID;

/**
 * Lịch trình nhìn từ trang quản trị — <b>mọi</b> locale cạnh nhau.
 *
 * <p>Khác bề mặt khách ở đúng chỗ đó: khách đọc một bản theo `Accept-Language`,
 * còn người dịch cần nhìn hai bản một lúc (docs/22 mục 4.2).
 */
public final class ItineraryViews {

    private ItineraryViews() {
    }

    public record Itinerary(List<Day> days) {
    }

    public record Day(
            int dayNumber,
            UUID destinationId,
            String destinationName,
            UUID hotelId,
            String hotelName,
            List<Text> translations) {
    }

    public record Text(String locale, String title, String description, boolean isSource) {
    }

    /** Một ngày do người dùng gửi lên — cấu trúc kèm bản ngôn ngữ nguồn. */
    public record DayInput(
            int dayNumber,
            UUID destinationId,
            UUID hotelId,
            String title,
            String description) {
    }

    /** Một ngày trong bản dịch — chỉ chữ, không đụng cấu trúc. */
    public record TextInput(int dayNumber, String title, String description) {
    }
}
