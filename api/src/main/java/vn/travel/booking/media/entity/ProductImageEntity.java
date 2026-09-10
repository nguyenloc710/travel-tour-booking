package vn.travel.booking.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Bảng nối — <b>không có cột kiểm toán, không có xoá mềm</b> (nhóm C của docs/11
 * mục 11.2). Vòng đời trùng khít với thực thể chủ, và nó đã có
 * {@code ON DELETE CASCADE} về phía đó.
 *
 * <p>Phía {@code asset_id} thì <b>không</b> cascade: gỡ một tệp khỏi danh sách là
 * xoá dòng nối, còn bản thân tệp và chứng từ giấy phép của nó ở lại — docs/12
 * mục 4.6. Một tệp dùng ở nhiều nơi là chuyện bình thường.
 */
@Entity
@Table(name = "product_image")
@IdClass(ProductImageEntity.CompositeId.class)
public class ProductImageEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Id
    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    protected ProductImageEntity() {
    }

    public ProductImageEntity(UUID productId, UUID assetId, short sortOrder) {
        this.productId = productId;
        this.assetId = assetId;
        this.sortOrder = sortOrder;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(short sortOrder) {
        this.sortOrder = sortOrder;
    }

    public static class CompositeId implements Serializable {

        private UUID productId;
        private UUID assetId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CompositeId other)) {
                return false;
            }
            return Objects.equals(productId, other.productId) && Objects.equals(assetId, other.assetId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, assetId);
        }
    }
}
