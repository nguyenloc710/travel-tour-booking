package vn.travel.booking.itinerary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.travel.booking.common.entity.BaseEntity;

import java.util.UUID;

/**
 * Một ngày trong lịch trình — phần <b>cấu trúc</b>, không có chữ.
 *
 * <p>Chữ nằm ở {@link ItineraryDayTranslationEntity}, một dòng cho mỗi ngôn ngữ.
 * Tách như vậy vì nơi ngủ đêm và khách sạn là <b>sự thật chung</b> cho mọi
 * ngôn ngữ, còn tiêu đề và mô tả thì không.
 *
 * <p>{@code destinationId} và {@code hotelId} đều cho phép rỗng, và đó là trạng
 * thái hợp lệ chứ không phải dữ liệu thiếu: ngày bay hoặc ngày trên tàu không
 * ngủ ở điểm đến nào.
 */
@Entity
@Table(name = "itinerary_day")
public class ItineraryDayEntity extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "day_number", nullable = false)
    private Short dayNumber;

    @Column(name = "destination_id")
    private UUID destinationId;

    @Column(name = "hotel_id")
    private UUID hotelId;

    protected ItineraryDayEntity() {
    }

    public ItineraryDayEntity(UUID id, UUID productId, short dayNumber) {
        this.id = id;
        this.productId = productId;
        this.dayNumber = dayNumber;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public Short getDayNumber() {
        return dayNumber;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(UUID destinationId) {
        this.destinationId = destinationId;
    }

    public UUID getHotelId() {
        return hotelId;
    }

    public void setHotelId(UUID hotelId) {
        this.hotelId = hotelId;
    }
}
