package vn.travel.booking.application.destination;

import java.util.List;
import java.util.Optional;

/**
 * Cổng ra ngoài do tầng application khai báo, infrastructure hiện thực.
 */
public interface DestinationQueryPort {

    /** {@code regionSlug} rỗng thì không lọc theo miền. */
    List<DestinationSummary> findDestinations(String market, String locale, String regionSlug);

    Optional<DestinationSummary> findDestination(String market, String locale, String slug);
}
