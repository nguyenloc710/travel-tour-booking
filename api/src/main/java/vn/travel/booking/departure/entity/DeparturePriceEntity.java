package vn.travel.booking.departure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một ô của ma trận <b>loại khách × kiểu phòng</b> cho một ngày khởi hành.
 *
 * <p>{@code currency} nằm ngay cạnh {@code amount} chứ không suy ra lúc đọc: một
 * số tiền không kèm tiền tệ là một con số vô nghĩa, và hai thị trường ở đây dùng
 * hai tiền tệ có số chữ số thập phân khác nhau (DKK 2, VND 0).
 *
 * <p>Giá trị này <b>không quy đổi</b> từ thị trường kia. Không có tỷ giá ở bất
 * kỳ đâu trong hệ thống này (CLAUDE.md điều 4).
 *
 * <p>Ghi vào bảng này làm trigger {@code tg_departure_price_price_from} tính lại
 * {@code product_market.price_from} (migration {@code V5}).
 */
@Entity
@Table(name = "departure_price")
@IdClass(DeparturePriceEntity.CompositeId.class)
public class DeparturePriceEntity {

    @Id
    @Column(name = "departure_id")
    private UUID departureId;

    @Id
    @Column(name = "pax_type_id")
    private UUID paxTypeId;

    @Id
    private String occupancy;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    protected DeparturePriceEntity() {
    }

    public DeparturePriceEntity(UUID departureId, UUID paxTypeId, String occupancy,
                                BigDecimal amount, String currency) {
        this.departureId = departureId;
        this.paxTypeId = paxTypeId;
        this.occupancy = occupancy;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID getDepartureId() {
        return departureId;
    }

    public UUID getPaxTypeId() {
        return paxTypeId;
    }

    public String getOccupancy() {
        return occupancy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    /** Khoá chính ba phần. */
    public record CompositeId(UUID departureId, UUID paxTypeId, String occupancy) implements Serializable {
        public CompositeId() {
            this(null, null, null);
        }
    }
}
