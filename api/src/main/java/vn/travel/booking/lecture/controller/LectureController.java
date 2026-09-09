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
import vn.travel.booking.lecture.service.LectureService;
import vn.travel.booking.web.generated.model.Lecture;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@Validated
public class LectureController {

    /** Giờ địa phương dạng `HH:MM` — không kèm giây, vì không ai xếp lịch theo giây. */
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    private final LectureService listLectures;

    public LectureController(LectureService listLectures) {
        this.listLectures = listLectures;
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
                .map(LectureController::toView)
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

    private static Lecture toView(LectureSummary l) {
        return new Lecture(
                l.id(), l.eventDate(), l.city(), l.title(), l.description(),
                l.seats(), l.seatsAvailable())
                .startTime(l.startTime() == null ? null : l.startTime().format(GIO))
                .venue(l.venue());
    }
}
