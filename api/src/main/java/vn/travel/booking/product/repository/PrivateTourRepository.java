package vn.travel.booking.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.product.entity.PrivateTourEntity;

import java.util.UUID;

/** Bảng con {@code product_private} — khoá chính là {@code product_id}. */
public interface PrivateTourRepository extends JpaRepository<PrivateTourEntity, UUID> {
}
