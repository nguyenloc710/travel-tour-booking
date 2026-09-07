package vn.travel.booking.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Bảng con 1-1 của {@code GROUP_TOUR} — docs/12 mục 4.2.
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
@Table(name = "product_group_tour")
public class GroupTourEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "min_pax", nullable = false)
    private Short minPax;

    @Column(name = "max_pax", nullable = false)
    private Short maxPax;

    @Column(name = "guaranteed_threshold", nullable = false)
    private Short guaranteedThreshold;

    @Column(name = "tour_leader_language", nullable = false)
    private String tourLeaderLanguage;

    @Column(name = "fitness_level", nullable = false)
    private Short fitnessLevel;

    protected GroupTourEntity() {
    }

    public GroupTourEntity(UUID productId) {
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Short getMinPax() {
        return minPax;
    }

    public void setMinPax(Short minPax) {
        this.minPax = minPax;
    }

    public Short getMaxPax() {
        return maxPax;
    }

    public void setMaxPax(Short maxPax) {
        this.maxPax = maxPax;
    }

    public Short getGuaranteedThreshold() {
        return guaranteedThreshold;
    }

    public void setGuaranteedThreshold(Short guaranteedThreshold) {
        this.guaranteedThreshold = guaranteedThreshold;
    }

    public String getTourLeaderLanguage() {
        return tourLeaderLanguage;
    }

    public void setTourLeaderLanguage(String tourLeaderLanguage) {
        this.tourLeaderLanguage = tourLeaderLanguage;
    }

    public Short getFitnessLevel() {
        return fitnessLevel;
    }

    public void setFitnessLevel(Short fitnessLevel) {
        this.fitnessLevel = fitnessLevel;
    }
}
