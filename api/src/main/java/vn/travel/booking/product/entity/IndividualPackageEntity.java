package vn.travel.booking.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Bảng con 1-1 của {@code INDIVIDUAL_PACKAGE} — docs/12 mục 4.2.
 *
 * <p>Nhóm C của docs/11 mục 11.2: <b>không có cột kiểm toán, không có
 * {@code soft_delete}</b>. Vòng đời trùng khít với {@code product} và đã có
 * {@code ON DELETE CASCADE}. Thêm {@code last_modified_by} vào đây tạo ra hai
 * câu trả lời cho câu hỏi "ai sửa sản phẩm này", và chúng sẽ khác nhau.
 *
 * <p>Cột {@code product_type} <b>cố tình không ánh xạ</b>: nó có
 * {@code DEFAULT} khoá cứng cộng một {@code CHECK}. Ánh xạ nó là mở đường cho
 * Java ghi một giá trị khác rồi nhận lỗi ràng buộc.
 */
@Entity
@Table(name = "product_individual")
public class IndividualPackageEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "min_party_size", nullable = false)
    private Short minPartySize;

    @Column(name = "flexible_date_window_days", nullable = false)
    private Short flexibleDateWindowDays;

    protected IndividualPackageEntity() {
    }

    public IndividualPackageEntity(UUID productId) {
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Short getMinPartySize() {
        return minPartySize;
    }

    public void setMinPartySize(Short minPartySize) {
        this.minPartySize = minPartySize;
    }

    public Short getFlexibleDateWindowDays() {
        return flexibleDateWindowDays;
    }

    public void setFlexibleDateWindowDays(Short flexibleDateWindowDays) {
        this.flexibleDateWindowDays = flexibleDateWindowDays;
    }
}
