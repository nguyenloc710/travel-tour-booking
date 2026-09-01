package vn.travel.booking.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.pricing.entity.PriceTierEntity;

import java.util.List;
import java.util.UUID;

/** Đường <b>ghi</b> của thang giá {@code PRIVATE_TOUR}. */
public interface PriceTierWriteRepository extends JpaRepository<PriceTierEntity, UUID> {

    List<PriceTierEntity> findByProductIdAndMarketAndSoftDeleteFalseOrderByMinPaxAsc(
            UUID productId, String market);
}
