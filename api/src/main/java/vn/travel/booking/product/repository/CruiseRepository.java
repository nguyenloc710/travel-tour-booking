package vn.travel.booking.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.product.entity.CruiseEntity;

import java.util.UUID;

/** Bảng con {@code product_cruise} — khoá chính là {@code product_id}. */
public interface CruiseRepository extends JpaRepository<CruiseEntity, UUID> {
}
