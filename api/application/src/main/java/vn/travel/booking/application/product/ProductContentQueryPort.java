package vn.travel.booking.application.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductContentQueryPort {

    Optional<VisibleProduct> findVisibleProduct(String market, String locale, String slug);

    List<ItineraryDay> findItinerary(UUID productId, String locale);

    List<HotelStay> findHotelStays(UUID productId, String locale);

    /** {@code fewSeatsThreshold} truyền vào vì nó là cấu hình, không phải kiến thức của truy vấn. */
    List<DepartureView> findDepartures(UUID productId, String market, int fewSeatsThreshold);
}
