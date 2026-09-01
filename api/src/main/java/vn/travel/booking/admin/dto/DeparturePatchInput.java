package vn.travel.booking.admin.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Trường {@code null} nghĩa là không đụng tới. */
public record DeparturePatchInput(
        LocalDate departDate,
        Short days,
        Short capacity,
        String cabinCategory,
        String baseStatus,
        UUID departureOriginId) {
}
