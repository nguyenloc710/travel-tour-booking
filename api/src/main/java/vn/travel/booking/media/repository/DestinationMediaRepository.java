package vn.travel.booking.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.media.entity.DestinationMediaEntity;

import java.util.List;
import java.util.UUID;

public interface DestinationMediaRepository
        extends JpaRepository<DestinationMediaEntity, DestinationMediaEntity.CompositeId> {

    List<DestinationMediaEntity> findByDestinationIdOrderBySortOrder(UUID destinationId);

    void deleteByDestinationId(UUID destinationId);
}
