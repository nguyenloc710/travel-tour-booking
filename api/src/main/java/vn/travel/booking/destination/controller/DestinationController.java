package vn.travel.booking.destination.controller;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.common.util.RequestScope;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.destination.dto.DestinationSummary;
import vn.travel.booking.web.generated.model.GalleryImage;
import vn.travel.booking.destination.service.DestinationService;
import vn.travel.booking.destination.service.DestinationService;
import vn.travel.booking.web.generated.model.Destination;

import java.time.Duration;
import java.util.List;

/**
 * Controller {@code implements} interface sinh từ {@code contracts/openapi.yaml}.
 */
@RestController
@Validated
public class DestinationController {

    private final DestinationService destinations;

    public DestinationController(DestinationService destinations) {
        this.destinations = destinations;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/destinations",
        produces = { "application/json" }
    )
    public ResponseEntity<List<Destination>> listDestinations(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @Valid @RequestParam(value = "region", required = false) @Nullable String region
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        List<Destination> than = destinations
                .list(RequestScope.market(market), locale, region).stream()
                .map(DestinationController::toView)
                .toList();

        return phanHoi(locale).body(than);
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/destinations/{slug}",
        produces = { "application/json" }
    )
    public ResponseEntity<Destination> getDestination(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("slug") String slug
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        return phanHoi(locale).body(toView(
                destinations.detail(RequestScope.market(market), locale, slug)));
    }

    private static Destination toView(DestinationSummary d) {
        return new Destination(
                d.slug(),
                d.name(),
                RefMapper.sangRef(d.region()),
                d.productCount())
                // Chưa có mô tả hoặc chưa có ảnh thì bỏ hẳn trường khỏi JSON,
                // không trả chuỗi rỗng và không trả đối tượng toàn null.
                .summary(d.summary())
                .image(d.image() == null ? null : new GalleryImage(
                        d.image().url(), d.image().alt(),
                        d.image().width(), d.image().height()));
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
