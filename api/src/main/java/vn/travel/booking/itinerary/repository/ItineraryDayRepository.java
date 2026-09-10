package vn.travel.booking.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.itinerary.entity.ItineraryDayEntity;

import java.util.List;
import java.util.UUID;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDayEntity, UUID> {

    /** Điều kiện xoá mềm viết tường minh trong tên phương thức — ADR-003. */
    List<ItineraryDayEntity> findByProductIdAndSoftDeleteFalseOrderByDayNumber(UUID productId);
}
