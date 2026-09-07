package vn.travel.booking.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Bảng con 1-1 của {@code PRIVATE_TOUR} — docs/12 mục 4.2.
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
@Table(name = "product_private")
public class PrivateTourEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "lead_time_days", nullable = false)
    private Short leadTimeDays;

    @Column(name = "quote_valid_days", nullable = false)
    private Short quoteValidDays;

    protected PrivateTourEntity() {
    }

    public PrivateTourEntity(UUID productId) {
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Short getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Short leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public Short getQuoteValidDays() {
        return quoteValidDays;
    }

    public void setQuoteValidDays(Short quoteValidDays) {
        this.quoteValidDays = quoteValidDays;
    }
}
