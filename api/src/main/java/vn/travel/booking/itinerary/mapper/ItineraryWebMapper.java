package vn.travel.booking.itinerary.mapper;

import org.mapstruct.Mapper;
import vn.travel.booking.itinerary.dto.ItineraryViews;
import vn.travel.booking.web.generated.model.AdminItinerary;
import vn.travel.booking.web.generated.model.AdminItineraryDay;
import vn.travel.booking.web.generated.model.AdminItineraryDayInput;
import vn.travel.booking.web.generated.model.AdminItineraryDayText;
import vn.travel.booking.web.generated.model.AdminItineraryDayTranslationInput;

import java.util.List;

/**
 * View của tầng nghiệp vụ ↔ model của hợp đồng.
 *
 * <p>Không map tay trong service hay controller — api/CLAUDE.md mục 0b.
 */
@Mapper(componentModel = "spring")
public interface ItineraryWebMapper {

    AdminItinerary toWeb(ItineraryViews.Itinerary v);

    AdminItineraryDay toWeb(ItineraryViews.Day v);

    AdminItineraryDayText toWeb(ItineraryViews.Text v);

    ItineraryViews.DayInput toInput(AdminItineraryDayInput input);

    List<ItineraryViews.DayInput> toInputList(List<AdminItineraryDayInput> input);

    ItineraryViews.TextInput toTextInput(AdminItineraryDayTranslationInput input);

    List<ItineraryViews.TextInput> toTextInputList(List<AdminItineraryDayTranslationInput> input);
}
