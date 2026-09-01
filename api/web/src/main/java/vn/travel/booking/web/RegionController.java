package vn.travel.booking.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.application.region.ListRegionsUseCase;
import vn.travel.booking.web.generated.api.RegionsApi;
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
public class RegionController implements RegionsApi {

    private final ListRegionsUseCase listRegions;

    public RegionController(ListRegionsUseCase listRegions) {
        this.listRegions = listRegions;
    }

    @Override
    public ResponseEntity<List<Region>> listRegions(String market, String acceptLanguage) {
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
