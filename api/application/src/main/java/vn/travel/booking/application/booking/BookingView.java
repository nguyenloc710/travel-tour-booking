package vn.travel.booking.application.booking;

import vn.travel.booking.domain.booking.BookingStatus;
import vn.travel.booking.domain.pricing.PriceBreakdown;

import java.time.LocalDate;

public record BookingView(
        String reference,
        BookingStatus status,
        String productTitle,
        LocalDate departDate,
        PriceBreakdown breakdown) {
}
