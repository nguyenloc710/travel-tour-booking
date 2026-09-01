package vn.travel.booking.application.theme;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.market.Markets;

import java.util.List;

@Service
public class ListThemesUseCase {

    private final ThemeQueryPort themes;
    private final Markets markets;

    public ListThemesUseCase(ThemeQueryPort themes, Markets markets) {
        this.themes = themes;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public List<ThemeSummary> execute(String market, String locale) {
        markets.requireActive(market);
        return themes.findThemes(market, locale);
    }
}
