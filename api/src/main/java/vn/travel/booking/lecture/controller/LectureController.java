package vn.travel.booking.lecture.controller;

import org.springframework.format.annotation.DateTimeFormat;
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
import vn.travel.booking.lecture.dto.LectureSummary;
import vn.travel.booking.lecture.mapper.LectureMapper;
import vn.travel.booking.lecture.service.LectureService;
import vn.travel.booking.web.generated.model.Lecture;

import java.time.Duration;
import java.util.List;

@RestController
@Validated
public class LectureController {

    private final LectureService listLectures;
    private final LectureMapper mapper;

    public LectureController(LectureService listLectures, LectureMapper mapper) {
        this.listLectures = listLectures;
        this.mapper = mapper;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/lectures",
        produces = { "application/json" }
    )
    public ResponseEntity<List<Lecture>> listLectures(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage
    ) {
        String locale = RequestScope.locale(acceptLanguage);

        List<Lecture> than = listLectures.execute(RequestScope.market(market), locale).stream()
                .map(mapper::toView)
                .toList();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                // Chỗ còn lại đổi khi có người đăng ký, nhưng không đổi từng phút
                // như ngày khởi hành. Một phút là đủ chậm để không dẫn khách vào
                // một buổi đã kín.
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(than);
    }
}
