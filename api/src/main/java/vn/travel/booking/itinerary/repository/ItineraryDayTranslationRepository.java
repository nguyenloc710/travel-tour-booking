package vn.travel.booking.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.itinerary.entity.ItineraryDayTranslationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryDayTranslationRepository
        extends JpaRepository<ItineraryDayTranslationEntity, ItineraryDayTranslationEntity.CompositeId> {

    List<ItineraryDayTranslationEntity> findByItineraryDayIdInAndSoftDeleteFalse(List<UUID> dayIds);

    Optional<ItineraryDayTranslationEntity> findByItineraryDayIdAndLocale(UUID dayId, String locale);
}
