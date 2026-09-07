package vn.travel.booking.departure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.travel.booking.common.entity.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Một ngày khởi hành của một sản phẩm <b>trong một thị trường</b>.
 *
 * <p>Một sản phẩm bán ở cả hai thị trường có <b>hai bộ</b> {@code departure},
 * không phải một bộ dùng chung (ADR-006): hai thị trường có lịch khác nhau, sức
 * chứa khác nhau và giá khác nhau. Đây là chỗ số dòng nhân đôi, và cũng là lý do
 * trang quản trị cần chức năng nhân bản lịch.
 *
 * <p>Ba thứ <b>không</b> nhập tay được, mỗi thứ một lý do:
 *
 * <ul>
 *   <li>{@code returnDate} — bằng {@code departDate + days - 1}, ràng buộc
 *       {@code ck_dep_dates} cưỡng chế. Nhận từ client là mời hai giá trị mâu
 *       thuẫn nhau.
 *   <li>{@code seatsBooked} — hệ quả của đơn đặt. Đường ghi duy nhất là luồng
 *       đặt tour, dưới khoá bi quan.
 *   <li>{@code GUARANTEED} — không nằm trong {@code ck_dep_status} vì nó là giá
 *       trị tính ra ({@code seatsBooked >= guaranteedThreshold}, docs/14 mục 5).
 * </ul>
 */
@Entity
@Table(name = "departure")
public class DepartureEntity extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    /** Không cho đổi: đổi thị trường của một ngày đã có đơn là đổi tiền tệ của những đơn đó. */
    @Column(name = "market", nullable = false, updatable = false)
    private String market;

    @Column(name = "depart_date", nullable = false)
    private LocalDate departDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "days", nullable = false)
    private Short days;

    /** Chỉ {@code CRUISE}. */
    @Column(name = "cabin_category")
    private String cabinCategory;

    @Column(name = "base_status", nullable = false)
    private String baseStatus;

    @Column(name = "capacity", nullable = false)
    private Short capacity;

    @Column(name = "seats_booked", nullable = false)
    private Short seatsBooked;

    @Column(name = "departure_origin_id")
    private UUID departureOriginId;

    protected DepartureEntity() {
    }

    public DepartureEntity(UUID id, UUID productId, String market) {
        this.id = id;
        this.productId = productId;
        this.market = market;
        this.seatsBooked = 0;
        this.baseStatus = "OPEN";
    }

    /**
     * Đặt ngày đi và số ngày <b>cùng lúc</b>, và tự suy {@code returnDate}. Hai
     * setter riêng cho phép tồn tại một khoảnh khắc mà ba cột không khớp nhau —
     * và nếu ai đó quên gọi cái thứ ba thì khoảnh khắc đó thành vĩnh viễn.
     */
    public void datLich(LocalDate departDate, Short days) {
        this.departDate = departDate;
        this.days = days;
        this.returnDate = departDate.plusDays(days - 1L);
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

    public LocalDate getDepartDate() {
        return departDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public Short getDays() {
        return days;
    }

    public String getCabinCategory() {
        return cabinCategory;
    }

    public void setCabinCategory(String cabinCategory) {
        this.cabinCategory = cabinCategory;
    }

    public String getBaseStatus() {
        return baseStatus;
    }

    public void setBaseStatus(String baseStatus) {
        this.baseStatus = baseStatus;
    }

    public Short getCapacity() {
        return capacity;
    }

    public void setCapacity(Short capacity) {
        this.capacity = capacity;
    }

    public Short getSeatsBooked() {
        return seatsBooked;
    }

    public UUID getDepartureOriginId() {
        return departureOriginId;
    }

    public void setDepartureOriginId(UUID departureOriginId) {
        this.departureOriginId = departureOriginId;
    }
}
