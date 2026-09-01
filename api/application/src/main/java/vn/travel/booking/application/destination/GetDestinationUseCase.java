package vn.travel.booking.application.destination;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.market.Markets;
import vn.travel.booking.application.shared.NotFoundException;

@Service
public class GetDestinationUseCase {

    private final DestinationQueryPort destinations;
    private final Markets markets;

    public GetDestinationUseCase(DestinationQueryPort destinations, Markets markets) {
        this.destinations = destinations;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public DestinationSummary execute(String market, String locale, String slug) {
        markets.requireActive(market);
        return destinations.findDestination(market, locale, slug)
                .orElseThrow(() -> new NotFoundException(
                        "destination slug=" + slug + " market=" + market + " locale=" + locale));
    }
}
