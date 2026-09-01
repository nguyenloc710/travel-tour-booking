package vn.travel.booking.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.product.entity.ProductEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Đường <b>ghi</b> của sản phẩm — JPA, docs/10 mục 6.
 *
 * <p>Tên có chữ {@code Write} vì {@link ProductRepository} đã là đường
 * <b>đọc</b> bằng SQL thuần. Hai lớp, hai công nghệ, cùng một bảng: đó là quyết
 * định của docs/10 chứ không phải trùng lặp cần dọn.
 */
public interface ProductWriteRepository extends JpaRepository<ProductEntity, UUID> {

    /** Điều kiện xoá mềm viết tường minh trong tên phương thức — ADR-003. */
    Optional<ProductEntity> findByIdAndSoftDeleteFalse(UUID id);
}
