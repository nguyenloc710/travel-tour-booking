package vn.travel.booking.itinerary.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.common.util.AcceptLanguages;
import vn.travel.booking.common.util.SecurityUtils;
import vn.travel.booking.itinerary.mapper.ItineraryWebMapper;
import vn.travel.booking.itinerary.service.ItineraryService;
import vn.travel.booking.web.generated.model.AdminItinerary;
import vn.travel.booking.web.generated.model.AdminItinerarySave;
import vn.travel.booking.web.generated.model.AdminItineraryTranslationSave;

import java.util.UUID;

import static vn.travel.booking.common.util.AdminResponses.noCache;

/**
 * Lịch trình từng ngày, bề mặt quản trị — docs/22 M3.
 *
 * <p>{@code @PreAuthorize} chỉ trả lời "ai được vào cửa". Luật "vào rồi thì sửa
 * được bản dịch nào" phụ thuộc locale và nằm ở {@link ItineraryService}.
 */
@RestController
public class AdminItineraryController {

    private final ItineraryService itinerary;
    private final ItineraryWebMapper mapper;

    public AdminItineraryController(ItineraryService itinerary, ItineraryWebMapper mapper) {
        this.itinerary = itinerary;
        this.mapper = mapper;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/products/{id}/itinerary",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN','CONSULTANT')")
    public ResponseEntity<AdminItinerary> getAdminItinerary(@PathVariable("id") UUID id) {
        return noCache().body(mapper.toWeb(itinerary.get(id, AcceptLanguages.SOURCE)));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/products/{id}/itinerary",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminItinerary> saveItinerary(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminItinerarySave input
    ) {
        return noCache().body(mapper.toWeb(itinerary.save(
                id, AcceptLanguages.SOURCE, mapper.toInputList(input.getDays()))));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/products/{id}/itinerary/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminItinerary> saveItineraryTranslation(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminItineraryTranslationSave input
    ) {
        return noCache().body(mapper.toWeb(itinerary.saveTranslation(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(),
                mapper.toTextInputList(input.getDays()))));
    }
}
