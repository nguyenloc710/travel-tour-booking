package vn.travel.booking.booking.dto;

import java.time.LocalDate;

public record PassengerDraft(
        String paxTypeCode,
        String fullName,
        LocalDate dateOfBirth,
        String nationality) {
}
