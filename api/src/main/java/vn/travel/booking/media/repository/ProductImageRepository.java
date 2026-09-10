package vn.travel.booking.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.media.entity.ProductImageEntity;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository
        extends JpaRepository<ProductImageEntity, ProductImageEntity.CompositeId> {

    List<ProductImageEntity> findByProductIdOrderBySortOrder(UUID productId);

    void deleteByProductId(UUID productId);
}
