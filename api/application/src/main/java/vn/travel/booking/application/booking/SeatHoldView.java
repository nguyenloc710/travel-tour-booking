package vn.travel.booking.application.booking;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SeatHoldView(UUID id, UUID departureId, int seats, OffsetDateTime expiresAt) {
}
