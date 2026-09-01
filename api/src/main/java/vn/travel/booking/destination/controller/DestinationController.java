package vn.travel.booking.destination.controller;

import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.common.util.RequestScope;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.destination.dto.DestinationSummary;
import vn.travel.booking.destination.service.DestinationService;
import vn.travel.booking.destination.service.DestinationService;
import vn.travel.booking.web.generated.api.DestinationsApi;
import vn.travel.booking.web.generated.model.Destination;

import java.time.Duration;
import java.util.List;

/**
 * Controller {@code implements} interface sinh từ {@code contracts/openapi.yaml}.
 */
@RestController
public class DestinationController implements DestinationsApi {

    private final DestinationService destinations;

    public DestinationController(DestinationService destinations) {
        this.destinations = destinations;
    }

    @Override
    public ResponseEntity<List<Destination>> listDestinations(
            String market, String acceptLanguage, String region) {

        String locale = RequestScope.locale(acceptLanguage);

        List<Destination> than = destinations
                .danhSach(RequestScope.market(market), locale, region).stream()
                .map(DestinationController::sang)
                .toList();

        return phanHoi(locale).body(than);
    }

    @Override
    public ResponseEntity<Destination> getDestination(
            String market, String acceptLanguage, String slug) {

        String locale = RequestScope.locale(acceptLanguage);

        return phanHoi(locale).body(sang(
                destinations.chiTiet(RequestScope.market(market), locale, slug)));
    }

    private static Destination sang(DestinationSummary d) {
        return new Destination(
                d.slug(),
                d.name(),
                RefMapper.sangRef(d.region()),
                d.productCount())
                // Chưa có mô tả thì bỏ hẳn trường khỏi JSON, không trả chuỗi rỗng.
                .summary(d.summary());
    }

    private static ResponseEntity.BodyBuilder phanHoi(String locale) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                // Thiếu Vary là CDN phục vụ bản tiếng Đan cho khách Việt.
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                // Danh mục tĩnh: 5 phút — docs/13 mục 8.
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic());
    }
}
