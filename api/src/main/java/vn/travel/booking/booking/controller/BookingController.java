package vn.travel.booking.booking.controller;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.travel.booking.common.mapper.RefMapper;
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

    public BookingController(BookingService bookingService, QuoteService quoteService, Idempotency idempotency) {
        this.bookingService = bookingService;
        this.quoteService = quoteService;
        this.idempotency = idempotency;
    }

    // ------------------------------------------------------------ tính giá

    /** Không đòi {@code Idempotency-Key}: nó không tạo ra gì để mà trùng. */
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/{market}/pricing/preview",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    public ResponseEntity<PriceBreakdown> xemTruocGia(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @Valid @RequestBody PricingRequest request
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        var table = bookingService.xemTruocGia(RequestScope.market(market), new PricingQuery(
                request.getDepartureId(),
                paxCount(request.getPax()),
                request.getSingleTravellers() == null ? 0 : request.getSingleTravellers(),
                request.getDepartureOriginId()));

        return noCache(locale).body(PricingMapper.sangBang(table));
    }

    // ------------------------------------------------------------ giữ chỗ

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/{market}/seat-holds",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    public ResponseEntity<SeatHold> giuCho(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
            @Valid @RequestBody SeatHoldRequest request
    ) {

        String locale = RequestScope.locale(acceptLanguage);
        String marketCode = RequestScope.market(market);

        return idempotency.run(idempotencyKey, marketCode, "seat-holds", request, SeatHold.class, () -> {
            SeatHoldView hold = bookingService.giuCho(marketCode, request.getDepartureId(),
                    request.getSeats(), idempotencyKey.toString());

            return daTao(locale)
                    .body(new SeatHold(hold.id(), hold.departureId(), hold.seats(), hold.expiresAt()));
        });
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/api/v1/{market}/seat-holds/{id}",
        produces = { "application/json" }
    )
    public ResponseEntity<Void> boGiuCho(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("id") UUID id
    ) {
        RequestScope.locale(acceptLanguage);
        bookingService.boGiuCho(RequestScope.market(market), id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    // ------------------------------------------------------------ đặt tour

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/{market}/bookings",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    public ResponseEntity<Booking> datTour(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
            @Valid @RequestBody BookingRequest request
    ) {

        String locale = RequestScope.locale(acceptLanguage);
        String marketCode = RequestScope.market(market);

        return idempotency.run(idempotencyKey, marketCode, "bookings", request, Booking.class, () -> {
            BookingView booking = bookingService.datTour(marketCode, locale, new BookingCommand(
                    request.getDepartureId(),
                    request.getSeatHoldId(),
                    paxCount(request.getPax()),
                    request.getSingleTravellers() == null ? 0 : request.getSingleTravellers(),
                    request.getDepartureOriginId(),
                    request.getPassengers().stream()
                            .map(k -> new PassengerDraft(k.getPaxTypeCode(), k.getFullName(),
                                    k.getDateOfBirth(), k.getNationality()))
                            .toList(),
                    request.getContactEmail(),
                    request.getContactPhone()));

            return daTao(locale).body(toView(booking));
        });
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/bookings/{reference}",
        produces = { "application/json" }
    )
    public ResponseEntity<Booking> traDon(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("reference") String reference,
            @NotNull @jakarta.validation.constraints.Email  @Valid @RequestParam(value = "email", required = true) String email
    ) {

        String locale = RequestScope.locale(acceptLanguage);
        return noCache(locale)
                .body(toView(bookingService.traDon(RequestScope.market(market), reference, email)));
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
    public ResponseEntity<QuoteReceipt> guiYeuCauBaoGia(
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

            return daTao(locale).body(new QuoteReceipt(
                    receipt.reference(),
                    QuoteStatus.fromValue(receipt.status().name()),
                    receipt.createdAt()));
        });
    }

    // ------------------------------------------------------------ ánh xạ

    private static Booking toView(BookingView d) {
        List<PriceLine> row = PricingMapper.sangDong(d.breakdown().lines());

        return new Booking(
                d.reference(),
                BookingStatus.fromValue(d.status().name()),
                d.productTitle(),
                RefMapper.sangTien(d.breakdown().total()),
                RefMapper.sangTien(d.breakdown().deposit()),
                RefMapper.sangTien(d.breakdown().balance()),
                row)
                .departDate(d.departDate());
    }

    /** Số khách theo mã loại; giữ nguyên thứ tự client gửi để bảng phân rã đọc được. */
    private static Map<String, Integer> paxCount(List<PaxCount> pax) {
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (PaxCount p : pax) {
            byType.merge(p.getPaxTypeCode(), p.getCount(), Integer::sum);
        }
        return byType;
    }

    private static ResponseEntity.BodyBuilder daTao(String locale) {
        return them(ResponseEntity.status(HttpStatus.CREATED), locale);
    }

    private static ResponseEntity.BodyBuilder noCache(String locale) {
        return them(ResponseEntity.ok(), locale);
    }

    private static ResponseEntity.BodyBuilder them(ResponseEntity.BodyBuilder builder, String locale) {
        return builder
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }
}
