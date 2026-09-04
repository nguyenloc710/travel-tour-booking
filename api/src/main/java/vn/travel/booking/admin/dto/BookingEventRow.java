package vn.travel.booking.admin.dto;

import vn.travel.booking.booking.dto.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một dòng nhật ký {@code booking_event} (docs/22 M7).
 *
 * <p>{@code fromStatus} là {@code null} ở <b>dòng đầu tiên</b>: đơn vừa sinh ra
 * không đến từ trạng thái nào.
 *
 * <p>{@code actorName} đọc từ {@code staff_user} lúc trả về chứ không chụp lại
 * vào nhật ký. Đổi tên hiển thị thì mọi dòng cũ đổi theo — và đó là hành vi
 * đúng: nhật ký ghi <b>ai</b>, không ghi <b>tên gọi lúc đó</b>.
 */
public record BookingEventRow(
        UUID id,
        BookingStatus fromStatus,
        BookingStatus toStatus,
        String actorType,
        UUID actorId,
        String actorName,
        String note,
        OffsetDateTime createdAt) {
}
