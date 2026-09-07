package vn.travel.booking.booking.dto;

import vn.travel.booking.booking.dto.BookingStatus;
import vn.travel.booking.pricing.dto.PriceBreakdown;

import java.time.LocalDate;

public record BookingView(
        String reference,
        BookingStatus status,
        String productTitle,
        LocalDate departDate,
        PriceBreakdown breakdown) {
}
