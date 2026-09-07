package vn.travel.booking.booking.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SeatHoldView(UUID id, UUID departureId, int seats, OffsetDateTime expiresAt) {
}
