package vn.travel.booking.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.product.entity.ProductMarketEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gán sản phẩm vào thị trường.
 *
 * <p>Không có {@code AndSoftDeleteFalse} ở đây, và đó là đúng: {@code
 * product_market} thuộc nhóm C của docs/11 mục 11.2 nên <b>không có cột
 * {@code soft_delete}</b>. Vòng đời của nó trùng khít với sản phẩm cha.
 */
public interface ProductMarketRepository
        extends JpaRepository<ProductMarketEntity, ProductMarketEntity.CompositeId> {

    List<ProductMarketEntity> findByProductIdOrderByMarketAsc(UUID productId);

    Optional<ProductMarketEntity> findByProductIdAndMarket(UUID productId, String market);
}
