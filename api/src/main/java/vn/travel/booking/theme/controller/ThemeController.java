package vn.travel.booking.theme.controller;

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
import vn.travel.booking.theme.mapper.ThemeMapper;
import vn.travel.booking.theme.service.ThemeService;
import vn.travel.booking.web.generated.model.Theme;

import java.time.Duration;
import java.util.List;

@RestController
@Validated
public class ThemeController {

    private final ThemeService listThemes;
    private final ThemeMapper themeMapper;

    public ThemeController(ThemeService listThemes, ThemeMapper themeMapper) {
        this.listThemes = listThemes;
        this.themeMapper = themeMapper;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/themes",
        produces = { "application/json" }
    )
    public ResponseEntity<List<Theme>> listThemes(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage
    ) {
        String locale = RequestScope.locale(acceptLanguage);

        List<Theme> than = listThemes.execute(RequestScope.market(market), locale).stream()
                .map(themeMapper::toView)
                .toList();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(than);
    }
}
