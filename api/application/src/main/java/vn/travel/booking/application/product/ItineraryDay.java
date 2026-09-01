package vn.travel.booking.application.product;

/**
 * Một ngày trong lịch trình. {@code destination} và {@code hotelName} là
 * {@code null} với ngày bay hoặc ngày trên tàu.
 *
 * <p>{@code hotelName} là tên riêng nên <b>không dịch</b> — docs/24 mục 5.
 */
public record ItineraryDay(
        int dayNumber,
        String title,
        String description,
        NamedRef destination,
        String hotelName) {
}
