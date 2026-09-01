package vn.travel.booking.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.product.entity.GroupTourEntity;

import java.util.UUID;

/** Bảng con {@code product_group_tour} — khoá chính là {@code product_id}. */
public interface GroupTourRepository extends JpaRepository<GroupTourEntity, UUID> {
}
