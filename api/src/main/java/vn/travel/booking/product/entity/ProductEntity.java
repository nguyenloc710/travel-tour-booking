package vn.travel.booking.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.travel.booking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Phần chung của một sản phẩm. Phần riêng của loại nằm ở bảng con 1-1.
 *
 * <p><b>{@code productType} là {@code updatable = false}</b>, và đó là quy tắc
 * nghiệp vụ chứ không phải tối ưu: mỗi loại một bảng con, đổi loại là mất dữ
 * liệu riêng của loại cũ (docs/22 mục 7 và mục 10). Cần đổi thì tạo sản phẩm
 * mới. Khoá nó ở tầng ORM nghĩa là một lệnh {@code setProductType()} viết nhầm
 * cũng không sinh ra được câu {@code UPDATE} nào.
 *
 * <p>{@code rating} và {@code reviewCount} <b>không sửa từ trang quản trị</b>:
 * chúng là kết quả tổng hợp từ đánh giá của khách. Entity mang chúng để đọc, và
 * mapper của đường ghi bỏ qua cả hai.
 */
@Entity
@Table(name = "product")
public class ProductEntity extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "product_type", nullable = false, updatable = false)
    private String productType;

    @Column(name = "primary_destination_id", nullable = false)
    private UUID primaryDestinationId;

    /** {@code NULL} với {@code DAY_TOUR}, và chỉ với nó — ràng buộc {@code ck_product_duration}. */
    @Column(name = "duration_days")
    private Short durationDays;

    @Column(name = "hero_image", nullable = false)
    private String heroImage;

    @Column(name = "map_image")
    private String mapImage;

    /** Template trang chi tiết. NULL = chưa chọn, frontend dùng mặc định của loại. */
    @Column(name = "layout")
    private String layout;

    @Column(name = "is_new", nullable = false)
    private boolean isNew;

    @Column(name = "rating")
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "consultant_id")
    private UUID consultantId;

    protected ProductEntity() {
    }

    public ProductEntity(UUID id, String productType) {
        this.id = id;
        this.productType = productType;
    }

    public UUID getId() {
        return id;
    }

    public String getProductType() {
        return productType;
    }

    public UUID getPrimaryDestinationId() {
        return primaryDestinationId;
    }

    public void setPrimaryDestinationId(UUID primaryDestinationId) {
        this.primaryDestinationId = primaryDestinationId;
    }

    public Short getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Short durationDays) {
        this.durationDays = durationDays;
    }

    public String getHeroImage() {
        return heroImage;
    }

    public void setHeroImage(String heroImage) {
        this.heroImage = heroImage;
    }

    public String getMapImage() {
        return mapImage;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public void setMapImage(String mapImage) {
        this.mapImage = mapImage;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public UUID getConsultantId() {
        return consultantId;
    }

    public void setConsultantId(UUID consultantId) {
        this.consultantId = consultantId;
    }
}
