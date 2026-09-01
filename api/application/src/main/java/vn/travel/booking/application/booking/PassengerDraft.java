package vn.travel.booking.application.booking;

import java.time.LocalDate;

public record PassengerDraft(
        String paxTypeCode,
        String fullName,
        LocalDate dateOfBirth,
        String nationality) {
}
