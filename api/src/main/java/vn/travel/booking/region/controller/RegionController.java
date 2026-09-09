package vn.travel.booking.region.controller;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.travel.booking.common.util.RequestScope;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.region.service.RegionService;
import vn.travel.booking.web.generated.model.Region;

import java.time.Duration;
import java.util.List;

/**
 * Controller {@code implements} interface SINH RA từ {@code contracts/openapi.yaml}.
 *
 * <p>Hệ quả: đổi spec mà quên sửa chỗ này là <b>lỗi biên dịch</b>, không phải bug
 * lúc chạy. Đó là lý do duy nhất để chọn spec-first thay vì code-first (ADR-002).
 */
@RestController
@Validated
public class RegionController {

    private final RegionService listRegions;

    public RegionController(RegionService listRegions) {
        this.listRegions = listRegions;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/regions",
        produces = { "application/json" }
    )
    public ResponseEntity<List<Region>> listRegions(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage
    ) {
        String locale = RequestScope.locale(acceptLanguage);

        List<Region> ket_qua = listRegions.execute(RequestScope.market(market), locale).stream()
                .map(r -> new Region(r.slug(), r.name(), r.productCount()))
                .toList();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                // Thiếu Vary là CDN phục vụ bản tiếng Đan cho khách Việt.
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                // Danh mục tĩnh: 5 phút. Ngày khởi hành thì no-store — docs/13 mục 8.
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(ket_qua);
    }
}
