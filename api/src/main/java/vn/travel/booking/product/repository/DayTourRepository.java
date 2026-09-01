package vn.travel.booking.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.product.entity.DayTourEntity;

import java.util.UUID;

/** Bảng con {@code product_day_tour} — khoá chính là {@code product_id}. */
public interface DayTourRepository extends JpaRepository<DayTourEntity, UUID> {
}
