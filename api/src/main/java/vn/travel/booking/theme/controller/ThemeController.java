package vn.travel.booking.theme.controller;

import vn.travel.booking.common.util.RequestScope;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.theme.service.ThemeService;
import vn.travel.booking.web.generated.api.ThemesApi;
import vn.travel.booking.web.generated.model.Theme;

import java.time.Duration;
import java.util.List;

@RestController
public class ThemeController implements ThemesApi {

    private final ThemeService listThemes;

    public ThemeController(ThemeService listThemes) {
        this.listThemes = listThemes;
    }

    @Override
    public ResponseEntity<List<Theme>> listThemes(String market, String acceptLanguage) {
        String locale = RequestScope.locale(acceptLanguage);

        List<Theme> than = listThemes.execute(RequestScope.market(market), locale).stream()
                .map(t -> new Theme(t.slug(), t.name(), t.productCount()))
                .toList();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(than);
    }
}
