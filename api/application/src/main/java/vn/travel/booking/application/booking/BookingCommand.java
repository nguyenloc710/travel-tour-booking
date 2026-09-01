package vn.travel.booking.application.booking;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BookingCommand(
        UUID departureId,
        UUID seatHoldId,
        Map<String, Integer> pax,
        int singleTravellers,
        UUID departureOriginId,
        List<PassengerDraft> passengers,
        String contactEmail,
        String contactPhone) {
}
