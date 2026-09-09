package vn.travel.booking.theme.mapper;

import org.mapstruct.Mapper;
import vn.travel.booking.theme.dto.ThemeSummary;
import vn.travel.booking.web.generated.model.Theme;

@Mapper(componentModel = "spring")
public interface ThemeMapper {

    Theme toView(ThemeSummary summary);
}
