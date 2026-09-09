package vn.travel.booking.destination.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.destination.dto.DestinationSummary;
import vn.travel.booking.destination.repository.DestinationRepository;
import vn.travel.booking.market.service.MarketService;

import java.util.List;

@Service
public class DestinationService {

    private final DestinationRepository destinations;
    private final MarketService markets;

    public DestinationService(DestinationRepository destinations, MarketService markets) {
        this.destinations = destinations;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public List<DestinationSummary> list(String market, String locale, String regionSlug) {
        markets.requireActive(market);
        return destinations.findDestinations(market, locale, regionSlug);
    }

    @Transactional(readOnly = true)
    public DestinationSummary detail(String market, String locale, String slug) {
        markets.requireActive(market);
        return destinations.findDestination(market, locale, slug)
                .orElseThrow(() -> new NotFoundException(
                        "destination slug=" + slug + " market=" + market + " locale=" + locale));
    }
}
