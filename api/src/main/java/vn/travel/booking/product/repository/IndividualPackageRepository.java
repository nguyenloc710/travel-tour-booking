package vn.travel.booking.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.product.entity.IndividualPackageEntity;

import java.util.UUID;

/** Bảng con {@code product_individual} — khoá chính là {@code product_id}. */
public interface IndividualPackageRepository extends JpaRepository<IndividualPackageEntity, UUID> {
}
