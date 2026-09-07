package vn.travel.booking.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.travel.booking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một bậc của thang giá {@code PRIVATE_TOUR} — docs/14 mục 4.
 *
 * <p>{@code maxPax} {@code NULL} nghĩa là <b>bậc cuối, không có trần</b>. Đúng
 * một bậc của mỗi thang được phép như vậy; kiểm ở tầng nghiệp vụ, vì CSDL không
 * diễn đạt được "đúng một dòng trong nhóm này có cột đó rỗng" bằng một
 * {@code CHECK}.
 *
 * <p>Thang giá thuộc <b>một thị trường</b>. Hai thị trường là hai thang, và
 * không thang nào tính ra từ thang kia.
 */
@Entity
@Table(name = "price_tier")
public class PriceTierEntity extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "market", nullable = false, updatable = false)
    private String market;

    @Column(name = "min_pax", nullable = false)
    private Short minPax;

    @Column(name = "max_pax")
    private Short maxPax;

    @Column(name = "price_per_person", nullable = false)
    private BigDecimal pricePerPerson;

    @Column(nullable = false)
    private String currency;

    protected PriceTierEntity() {
    }

    public PriceTierEntity(UUID id, UUID productId, String market, Short minPax, Short maxPax,
                           BigDecimal pricePerPerson, String currency) {
        this.id = id;
        this.productId = productId;
        this.market = market;
        this.minPax = minPax;
        this.maxPax = maxPax;
        this.pricePerPerson = pricePerPerson;
        this.currency = currency;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getMarket() {
        return market;
    }

    public Short getMinPax() {
        return minPax;
    }

    public Short getMaxPax() {
        return maxPax;
    }

    public BigDecimal getPricePerPerson() {
        return pricePerPerson;
    }

    public String getCurrency() {
        return currency;
    }
}
