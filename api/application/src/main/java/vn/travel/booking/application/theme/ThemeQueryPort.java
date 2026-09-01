package vn.travel.booking.application.theme;

import java.util.List;

public interface ThemeQueryPort {

    List<ThemeSummary> findThemes(String market, String locale);
}
