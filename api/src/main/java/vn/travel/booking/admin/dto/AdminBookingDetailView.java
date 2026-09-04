package vn.travel.booking.admin.dto;

import vn.travel.booking.booking.dto.BookingStatus;
import vn.travel.booking.pricing.dto.PriceBreakdown;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Một đơn nhìn từ trang quản trị (docs/22 M7).
 *
 * <p>{@code breakdown} dựng từ {@code booking_line} — những dòng đã <b>chụp lại
 * lúc đặt</b>. Không gọi lại engine giá: đổi bảng giá hôm nay không được làm đổi
 * đơn cũ (docs/40, tiêu chí ra số 5 của G4).
 *
 * <p>{@code events} là <b>toàn bộ</b> nhật ký, cũ nhất trước, không phân trang.
 * Một nhật ký kiểm toán hiện một nửa là một nhật ký không dùng được khi có tranh
 * chấp.
 */
public record AdminBookingDetailView(
        UUID id,
        String reference,
        BookingStatus status,
        String market,
        String locale,
        UUID productId,
        String productTitle,
        UUID departureId,
        LocalDate departDate,
        String contactEmail,
        String contactPhone,
        OffsetDateTime createdAt,
        PriceBreakdown breakdown,
        List<BookingPassengerRow> passengers,
        List<BookingEventRow> events) {
}
