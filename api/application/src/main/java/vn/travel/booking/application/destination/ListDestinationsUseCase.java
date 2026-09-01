package vn.travel.booking.application.destination;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.market.Markets;

import java.util.List;

@Service
public class ListDestinationsUseCase {

    private final DestinationQueryPort destinations;
    private final Markets markets;

    public ListDestinationsUseCase(DestinationQueryPort destinations, Markets markets) {
        this.destinations = destinations;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public List<DestinationSummary> execute(String market, String locale, String regionSlug) {
        markets.requireActive(market);
        return destinations.findDestinations(market, locale, regionSlug);
    }
}
