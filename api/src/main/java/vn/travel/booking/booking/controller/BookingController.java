package vn.travel.booking.booking.controller;

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
import vn.travel.booking.web.generated.api.BookingApi;
import vn.travel.booking.web.generated.model.Booking;
import vn.travel.booking.web.generated.model.BookingRequest;
import vn.travel.booking.web.generated.model.BookingStatus;
import vn.travel.booking.web.generated.model.PaxCount;
import vn.travel.booking.web.generated.model.PriceBreakdown;
import vn.travel.booking.web.generated.model.PriceLine;
import vn.travel.booking.web.generated.model.PricingRequest;
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
public class BookingController implements BookingApi {

    private final BookingService datTour;
    private final Idempotency motLan;

    public BookingController(BookingService datTour, Idempotency motLan) {
        this.datTour = datTour;
        this.motLan = motLan;
    }

    // ------------------------------------------------------------ tính giá

    /** Không đòi {@code Idempotency-Key}: nó không tạo ra gì để mà trùng. */
    @Override
    public ResponseEntity<PriceBreakdown> xemTruocGia(
            String market, String acceptLanguage, PricingRequest yeuCau) {

        String locale = RequestScope.locale(acceptLanguage);

        var bang = datTour.xemTruocGia(RequestScope.market(market), new PricingQuery(
                yeuCau.getDepartureId(),
                soKhach(yeuCau.getPax()),
                yeuCau.getSingleTravellers() == null ? 0 : yeuCau.getSingleTravellers(),
                yeuCau.getDepartureOriginId()));

        return khongCache(locale).body(PricingMapper.sangBang(bang));
    }

    // ------------------------------------------------------------ giữ chỗ

    @Override
    public ResponseEntity<SeatHold> giuCho(
            String market, String acceptLanguage, UUID idempotencyKey, SeatHoldRequest yeuCau) {

        String locale = RequestScope.locale(acceptLanguage);
        String thiTruong = RequestScope.market(market);

        return motLan.chay(idempotencyKey, thiTruong, "seat-holds", yeuCau, SeatHold.class, () -> {
            SeatHoldView giu = datTour.giuCho(thiTruong, yeuCau.getDepartureId(),
                    yeuCau.getSeats(), idempotencyKey.toString());

            return daTao(locale)
                    .body(new SeatHold(giu.id(), giu.departureId(), giu.seats(), giu.expiresAt()));
        });
    }

    @Override
    public ResponseEntity<Void> boGiuCho(String market, String acceptLanguage, UUID id) {
        RequestScope.locale(acceptLanguage);
        datTour.boGiuCho(RequestScope.market(market), id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    // ------------------------------------------------------------ đặt tour

    @Override
    public ResponseEntity<Booking> datTour(
            String market, String acceptLanguage, UUID idempotencyKey, BookingRequest yeuCau) {

        String locale = RequestScope.locale(acceptLanguage);
        String thiTruong = RequestScope.market(market);

        return motLan.chay(idempotencyKey, thiTruong, "bookings", yeuCau, Booking.class, () -> {
            BookingView don = datTour.datTour(thiTruong, locale, new BookingCommand(
                    yeuCau.getDepartureId(),
                    yeuCau.getSeatHoldId(),
                    soKhach(yeuCau.getPax()),
                    yeuCau.getSingleTravellers() == null ? 0 : yeuCau.getSingleTravellers(),
                    yeuCau.getDepartureOriginId(),
                    yeuCau.getPassengers().stream()
                            .map(k -> new PassengerDraft(k.getPaxTypeCode(), k.getFullName(),
                                    k.getDateOfBirth(), k.getNationality()))
                            .toList(),
                    yeuCau.getContactEmail(),
                    yeuCau.getContactPhone()));

            return daTao(locale).body(sang(don));
        });
    }

    @Override
    public ResponseEntity<Booking> traDon(
            String market, String acceptLanguage, String reference, String email) {

        String locale = RequestScope.locale(acceptLanguage);
        return khongCache(locale)
                .body(sang(datTour.traDon(RequestScope.market(market), reference, email)));
    }

    // ------------------------------------------------------------ ánh xạ

    private static Booking sang(BookingView d) {
        List<PriceLine> dong = PricingMapper.sangDong(d.breakdown().lines());

        return new Booking(
                d.reference(),
                BookingStatus.fromValue(d.status().name()),
                d.productTitle(),
                RefMapper.sangTien(d.breakdown().total()),
                RefMapper.sangTien(d.breakdown().deposit()),
                RefMapper.sangTien(d.breakdown().balance()),
                dong)
                .departDate(d.departDate());
    }

    /** Số khách theo mã loại; giữ nguyên thứ tự client gửi để bảng phân rã đọc được. */
    private static Map<String, Integer> soKhach(List<PaxCount> pax) {
        Map<String, Integer> theoLoai = new LinkedHashMap<>();
        for (PaxCount p : pax) {
            theoLoai.merge(p.getPaxTypeCode(), p.getCount(), Integer::sum);
        }
        return theoLoai;
    }

    private static ResponseEntity.BodyBuilder daTao(String locale) {
        return them(ResponseEntity.status(HttpStatus.CREATED), locale);
    }

    private static ResponseEntity.BodyBuilder khongCache(String locale) {
        return them(ResponseEntity.ok(), locale);
    }

    private static ResponseEntity.BodyBuilder them(ResponseEntity.BodyBuilder builder, String locale) {
        return builder
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }
}
