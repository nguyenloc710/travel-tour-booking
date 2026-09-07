package vn.travel.booking.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.product.entity.ComboEntity;

import java.util.UUID;

/** Bảng con {@code product_combo} — khoá chính là {@code product_id}. */
public interface ComboRepository extends JpaRepository<ComboEntity, UUID> {
}
