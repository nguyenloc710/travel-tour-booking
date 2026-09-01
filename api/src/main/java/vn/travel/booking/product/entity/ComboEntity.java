package vn.travel.booking.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Bảng con 1-1 của {@code COMBO} — docs/12 mục 4.2.
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
@Table(name = "product_combo")
public class ComboEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "nights", nullable = false)
    private Short nights;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    protected ComboEntity() {
    }

    public ComboEntity(UUID productId) {
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Short getNights() {
        return nights;
    }

    public void setNights(Short nights) {
        this.nights = nights;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }
}
