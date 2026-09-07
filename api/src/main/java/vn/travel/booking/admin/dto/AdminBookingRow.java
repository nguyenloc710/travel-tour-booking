package vn.travel.booking.admin.dto;

import vn.travel.booking.booking.dto.BookingStatus;
import vn.travel.booking.common.money.Money;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một dòng của danh sách đơn quản trị (docs/22 M6).
 *
 * @param locale     ngôn ngữ khách đã đặt bằng — <b>không suy ra từ market</b>.
 *                   Khách Việt sống ở Đan Mạch đặt ở {@code DK} mà đọc
 *                   {@code vi} (docs/02)
 * @param departDate {@code null} với {@code PRIVATE_TOUR} và {@code COMBO} —
 *                   hai loại không gắn với một {@code departure}
 * @param paxCount   đếm từ {@code booking_passenger}, không phải cột lưu sẵn
 */
public record AdminBookingRow(
        UUID id,
        String reference,
        BookingStatus status,
        String market,
        String locale,
        String productTitle,
        LocalDate departDate,
        int paxCount,
        Money total,
        String contactEmail,
        OffsetDateTime createdAt) {
}
