package vn.travel.booking.booking.controller;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.travel.booking.booking.mapper.BookingWebMapper;
import vn.travel.booking.common.util.Idempotency;
import vn.travel.booking.common.util.RequestScope;
import vn.travel.booking.pricing.mapper.PricingMapper;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.booking.dto.BookingCommand;
import vn.travel.booking.booking.service.BookingService;
import vn.travel.booking.booking.dto.BookingView;
import vn.travel.booking.booking.dto.PassengerDraft;
import vn.travel.booking.booking.dto.PricingQuery;
import vn.travel.booking.booking.dto.SeatHoldView;
import vn.travel.booking.quote.dto.QuoteReceiptView;
import vn.travel.booking.quote.dto.QuoteRequestCommand;
import vn.travel.booking.quote.service.QuoteService;
import vn.travel.booking.web.generated.model.Booking;
import vn.travel.booking.web.generated.model.BookingRequest;
import vn.travel.booking.web.generated.model.BookingStatus;
import vn.travel.booking.web.generated.model.PaxCount;
import vn.travel.booking.web.generated.model.PriceBreakdown;
import vn.travel.booking.web.generated.model.PriceLine;
import vn.travel.booking.web.generated.model.PricingRequest;
import vn.travel.booking.web.generated.model.QuoteReceipt;
import vn.travel.booking.web.generated.model.QuoteRequestInput;
import vn.travel.booking.web.generated.model.QuoteStatus;
import vn.travel.booking.web.generated.model.SeatHold;
import vn.travel.booking.web.generated.model.SeatHoldRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Đường ghi của khách: tính giá, giữ chỗ, đặt tour, tra đơn.
 *
 * <p><b>Mọi phản hồi ở đây là {@code no-store}.</b> Số chỗ đổi từng phút, và giá
 * là thứ khách sắp trả tiền theo — cache một trong hai là dẫn khách vào một giao
 * dịch sẽ thất bại ở bước cuối (docs/13 mục 8).
 */
@RestController
@Validated
public class BookingController {

    private final BookingService bookingService;
    private final QuoteService quoteService;
    private final Idempotency idempotency;
    private final BookingWebMapper bookingMapper;
    private final PricingMapper pricingMapper;

    public BookingController(BookingService bookingService,
                             QuoteService quoteService,
                             Idempotency idempotency,
                             BookingWebMapper bookingMapper,
                             PricingMapper pricingMapper) {
        this.bookingService = bookingService;
        this.quoteService = quoteService;
        this.idempotency = idempotency;
        this.bookingMapper = bookingMapper;
        this.pricingMapper = pricingMapper;
    }

    // ------------------------------------------------------------ tính giá

    /** Không đòi {@code Idempotency-Key}: nó không tạo ra gì để mà trùng. */
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/{market}/pricing/preview",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    public ResponseEntity<PriceBreakdown> previewPrice(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @Valid @RequestBody PricingRequest request
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        var table = bookingService.previewPrice(RequestScope.market(market), new PricingQuery(
                request.getDepartureId(),
                paxCount(request.getPax()),
                request.getSingleTravellers() == null ? 0 : request.getSingleTravellers(),
                request.getDepartureOriginId()));

        return noCache(locale).body(pricingMapper.toPriceBreakdown(table));
    }

    // ------------------------------------------------------------ giữ chỗ

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/{market}/seat-holds",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    public ResponseEntity<SeatHold> holdSeat(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
            @Valid @RequestBody SeatHoldRequest request
    ) {

        String locale = RequestScope.locale(acceptLanguage);
        String marketCode = RequestScope.market(market);

        return idempotency.run(idempotencyKey, marketCode, "seat-holds", request, SeatHold.class, () -> {
            SeatHoldView hold = bookingService.holdSeat(marketCode, request.getDepartureId(),
                    request.getSeats(), idempotencyKey.toString());

            return created(locale).body(bookingMapper.toSeatHold(hold));
        });
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/{market}/seat-holds/{id}",
        produces = { "application/json" }
    )
    public ResponseEntity<Void> releaseSeatHold(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("id") UUID id
    ) {
        RequestScope.locale(acceptLanguage);
        bookingService.releaseSeatHold(RequestScope.market(market), id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    // ------------------------------------------------------------ đặt tour

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/{market}/bookings",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    public ResponseEntity<Booking> createBooking(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
            @Valid @RequestBody BookingRequest request
    ) {

        String locale = RequestScope.locale(acceptLanguage);
        String marketCode = RequestScope.market(market);

        return idempotency.run(idempotencyKey, marketCode, "bookings", request, Booking.class, () -> {
            BookingView booking = bookingService.bookTour(marketCode, locale, new BookingCommand(
                    request.getDepartureId(),
                    request.getSeatHoldId(),
                    paxCount(request.getPax()),
                    request.getSingleTravellers() == null ? 0 : request.getSingleTravellers(),
                    request.getDepartureOriginId(),
                    bookingMapper.toPassengerDraftList(request.getPassengers()),
                    request.getContactEmail(),
                    request.getContactPhone()));

            return created(locale).body(bookingMapper.toBooking(booking));
        });
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/bookings/{reference}",
        produces = { "application/json" }
    )
    public ResponseEntity<Booking> getBooking(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("reference") String reference,
            @NotNull @jakarta.validation.constraints.Email  @Valid @RequestParam(value = "email", required = true) String email
    ) {

        String locale = RequestScope.locale(acceptLanguage);
        return noCache(locale)
                .body(bookingMapper.toBooking(bookingService.getBooking(RequestScope.market(market), reference, email)));
    }

    // ------------------------------------------------------------ báo giá

    /**
     * Yêu cầu báo giá của khách — {@code PRIVATE_TOUR}, docs/23 mục 7.
     *
     * <p>Đòi {@code Idempotency-Key} như hai endpoint ghi kia (docs/13 mục 7).
     * Không phải vì nó tính tiền — nó không tính gì — mà vì khách bấm nút hai
     * lần thì tư vấn viên nhận hai yêu cầu giống hệt nhau và gọi điện hai lần.
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/{market}/quote-requests",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    public ResponseEntity<QuoteReceipt> submitQuoteRequest(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
            @Valid @RequestBody QuoteRequestInput request
    ) {

        String locale = RequestScope.locale(acceptLanguage);
        String marketCode = RequestScope.market(market);

        return idempotency.run(idempotencyKey, marketCode, "quote-requests", request,
                QuoteReceipt.class, () -> {

            QuoteReceiptView receipt = quoteService.submitRequest(marketCode, locale,
                    new QuoteRequestCommand(
                            request.getProductSlug(),
                            request.getPartySize(),
                            request.getRequestedDate(),
                            request.getContactName(),
                            request.getContactEmail(),
                            request.getContactPhone(),
                            request.getMessage()));

            return created(locale).body(bookingMapper.toQuoteReceipt(receipt));
        });
    }

    /** Số khách theo mã loại; giữ nguyên thứ tự client gửi để bảng phân rã đọc được. */
    private static Map<String, Integer> paxCount(List<PaxCount> pax) {
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (PaxCount p : pax) {
            byType.merge(p.getPaxTypeCode(), p.getCount(), Integer::sum);
        }
        return byType;
    }

    private static ResponseEntity.BodyBuilder created(String locale) {
        return withHeaders(ResponseEntity.status(HttpStatus.CREATED), locale);
    }

    private static ResponseEntity.BodyBuilder noCache(String locale) {
        return withHeaders(ResponseEntity.ok(), locale);
    }

    private static ResponseEntity.BodyBuilder withHeaders(ResponseEntity.BodyBuilder builder, String locale) {
        return builder
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }
}
