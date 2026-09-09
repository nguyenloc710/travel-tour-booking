package vn.travel.booking.destination.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.travel.booking.common.util.RequestScope;
import vn.travel.booking.destination.mapper.DestinationMapper;
import vn.travel.booking.destination.dto.DestinationSummary;
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
    private final DestinationMapper mapper;

    public DestinationController(DestinationService destinations, DestinationMapper mapper) {
        this.destinations = destinations;
        this.mapper = mapper;
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

        List<Destination> body = destinations
                .list(RequestScope.market(market), locale, region).stream()
                .map(mapper::toView)
                .toList();

        return response(locale).body(body);
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

        return response(locale).body(mapper.toView(
                destinations.detail(RequestScope.market(market), locale, slug)));
    }

    private static ResponseEntity.BodyBuilder response(String locale) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                // Thiếu Vary là CDN phục vụ bản tiếng Đan cho khách Việt.
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                // Danh mục tĩnh: 5 phút — docs/13 mục 8.
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic());
    }
}
