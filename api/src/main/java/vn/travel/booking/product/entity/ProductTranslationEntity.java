package vn.travel.booking.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.travel.booking.common.entity.BaseEntity;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bản dịch của một sản phẩm — khoá chính kép {@code (product_id, locale)}.
 *
 * <p>Đây là entity JPA <b>đầu tiên</b> của dự án, và nó nằm ở đường <b>ghi</b>
 * của trang quản trị. Đường đọc của website khách vẫn là SQL thuần (docs/10
 * mục 6): join bảng dịch kèm lọc theo market và sắp theo collation là chỗ JPA
 * sinh SQL tệ.
 */
@Entity
@Table(name = "product_translation")
@IdClass(ProductTranslationEntity.Khoa.class)
public class ProductTranslationEntity extends BaseEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Id
    private String locale;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "short_description", nullable = false)
    private String shortDescription;

    // TEXT[] của Postgres. Hibernate 6 ánh xạ được qua JdbcTypeCode(ARRAY).
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "long_description", nullable = false)
    private String[] longDescription;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "why_choose_this", nullable = false)
    private String[] whyChooseThis;

    @Column(name = "hero_image_alt", nullable = false)
    private String heroImageAlt;

    @Column(nullable = false)
    private String status;

    @Column(name = "translated_at")
    private OffsetDateTime translatedAt;

    @Column(name = "translated_by")
    private UUID translatedBy;

    protected ProductTranslationEntity() {
    }

    public ProductTranslationEntity(UUID productId, String locale) {
        this.productId = productId;
        this.locale = locale;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getLocale() {
        return locale;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String[] getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String[] longDescription) {
        this.longDescription = longDescription;
    }

    public String[] getWhyChooseThis() {
        return whyChooseThis;
    }

    public void setWhyChooseThis(String[] whyChooseThis) {
        this.whyChooseThis = whyChooseThis;
    }

    public String getHeroImageAlt() {
        return heroImageAlt;
    }

    public void setHeroImageAlt(String heroImageAlt) {
        this.heroImageAlt = heroImageAlt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getTranslatedAt() {
        return translatedAt;
    }

    /**
     * {@code translated_at} KHÁC {@code last_modified_at}: dịch xong khác với sửa
     * chính tả (docs/12 mục 4.3). Trạng thái {@code OUTDATED} tính từ cặp này.
     */
    public void setTranslatedAt(OffsetDateTime translatedAt) {
        this.translatedAt = translatedAt;
    }

    public UUID getTranslatedBy() {
        return translatedBy;
    }

    public void setTranslatedBy(UUID translatedBy) {
        this.translatedBy = translatedBy;
    }

    /** Khoá chính kép. */
    public record Khoa(UUID productId, String locale) implements Serializable {
        public Khoa() {
            this(null, null);
        }
    }
}
