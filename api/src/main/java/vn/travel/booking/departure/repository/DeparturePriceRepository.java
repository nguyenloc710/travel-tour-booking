package vn.travel.booking.departure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.departure.entity.DeparturePriceEntity;

import java.util.List;
import java.util.UUID;

public interface DeparturePriceRepository
        extends JpaRepository<DeparturePriceEntity, DeparturePriceEntity.CompositeId> {

    List<DeparturePriceEntity> findByDepartureId(UUID departureId);

    void deleteByDepartureId(UUID departureId);
}
