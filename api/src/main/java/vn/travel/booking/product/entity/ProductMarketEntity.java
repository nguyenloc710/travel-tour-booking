package vn.travel.booking.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Sản phẩm đứng ở đâu trong một thị trường — công tắc "bắt đầu bán được"
 * (docs/01 mục 4.4).
 *
 * <p>Nhóm C của docs/11 mục 11.2: không cột kiểm toán, không {@code soft_delete}.
 * Hệ quả cần biết khi ghi: <b>bật hay tắt công tắc này không tự nó ghi lại ai
 * làm</b>. Câu trả lời duy nhất cho "ai sửa sản phẩm này" là
 * {@code product.last_modified_by}, nên service phải chạm cả {@code product}.
 *
 * <p>{@code price_from} là cột <b>vật chất hoá do trigger sở hữu</b> (migration
 * {@code V5}), nên nó {@code insertable = false, updatable = false}: Java đọc
 * được, không ghi được. Cùng khuôn với {@code last_modified_at} ở
 * {@code BaseEntity} — một cột, một chủ sở hữu.
 */
@Entity
@Table(name = "product_market")
@IdClass(ProductMarketEntity.Khoa.class)
public class ProductMarketEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Id
    private String market;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "price_from", insertable = false, updatable = false)
    private BigDecimal priceFrom;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected ProductMarketEntity() {
    }

    public ProductMarketEntity(UUID productId, String market) {
        this.productId = productId;
        this.market = market;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getMarket() {
        return market;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public BigDecimal getPriceFrom() {
        return priceFrom;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    /** Khoá chính kép. */
    public record Khoa(UUID productId, String market) implements Serializable {
        public Khoa() {
            this(null, null);
        }
    }
}
