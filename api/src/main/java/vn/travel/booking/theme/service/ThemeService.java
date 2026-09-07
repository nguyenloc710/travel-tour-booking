package vn.travel.booking.theme.service;

import vn.travel.booking.theme.dto.ThemeSummary;
import vn.travel.booking.theme.repository.ThemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.market.service.MarketService;

import java.util.List;

@Service
public class ThemeService {

    private final ThemeRepository themes;
    private final MarketService markets;

    public ThemeService(ThemeRepository themes, MarketService markets) {
        this.themes = themes;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public List<ThemeSummary> execute(String market, String locale) {
        markets.requireActive(market);
        return themes.findThemes(market, locale);
    }
}
