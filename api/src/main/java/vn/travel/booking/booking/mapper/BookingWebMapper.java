package vn.travel.booking.booking.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.booking.dto.BookingView;
import vn.travel.booking.booking.dto.PassengerDraft;
import vn.travel.booking.booking.dto.SeatHoldView;
import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.pricing.mapper.PricingMapper;
import vn.travel.booking.quote.dto.QuoteReceiptView;
import vn.travel.booking.web.generated.model.Booking;
import vn.travel.booking.web.generated.model.BookingStatus;
import vn.travel.booking.web.generated.model.PassengerInput;
import vn.travel.booking.web.generated.model.QuoteReceipt;
import vn.travel.booking.web.generated.model.QuoteStatus;
import vn.travel.booking.web.generated.model.SeatHold;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PricingMapper.class})
public interface BookingWebMapper {

    @Mapping(target = "status", source = "status")
    @Mapping(target = "total", source = "breakdown.total")
    @Mapping(target = "deposit", source = "breakdown.deposit")
    @Mapping(target = "balance", source = "breakdown.balance")
    @Mapping(target = "lines", source = "breakdown.lines")
    Booking toBooking(BookingView d);

    SeatHold toSeatHold(SeatHoldView hold);

    @Mapping(target = "status", source = "status")
    QuoteReceipt toQuoteReceipt(QuoteReceiptView receipt);

    PassengerDraft toPassengerDraft(PassengerInput passenger);

    List<PassengerDraft> toPassengerDraftList(List<PassengerInput> passengers);

    default BookingStatus toBookingStatus(vn.travel.booking.booking.dto.BookingStatus status) {
        return status == null ? null : BookingStatus.fromValue(status.name());
    }

    default QuoteStatus toQuoteStatus(vn.travel.booking.quote.dto.QuoteStatus status) {
        return status == null ? null : QuoteStatus.fromValue(status.name());
    }
}
