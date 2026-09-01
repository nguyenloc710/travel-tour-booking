package vn.travel.booking.infrastructure.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.infrastructure.product.entity.ProductTranslationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductTranslationRepository
        extends JpaRepository<ProductTranslationEntity, ProductTranslationEntity.Khoa> {

    /** Điều kiện xoá mềm viết tường minh trong tên phương thức — ADR-003. */
    List<ProductTranslationEntity> findByProductIdAndSoftDeleteFalse(UUID productId);

    Optional<ProductTranslationEntity> findByProductIdAndLocaleAndSoftDeleteFalse(
            UUID productId, String locale);
}
