package vn.travel.booking.itinerary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import vn.travel.booking.common.entity.BaseEntity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Tiêu đề và mô tả của một ngày, theo từng ngôn ngữ. */
@Entity
@Table(name = "itinerary_day_translation")
@IdClass(ItineraryDayTranslationEntity.CompositeId.class)
public class ItineraryDayTranslationEntity extends BaseEntity {

    @Id
    @Column(name = "itinerary_day_id")
    private UUID itineraryDayId;

    @Id
    private String locale;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    protected ItineraryDayTranslationEntity() {
    }

    public ItineraryDayTranslationEntity(UUID itineraryDayId, String locale, String title, String description) {
        this.itineraryDayId = itineraryDayId;
        this.locale = locale;
        this.title = title;
        this.description = description;
    }

    public UUID getItineraryDayId() {
        return itineraryDayId;
    }

    public String getLocale() {
        return locale;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static class CompositeId implements Serializable {

        private UUID itineraryDayId;
        private String locale;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CompositeId other)) {
                return false;
            }
            return Objects.equals(itineraryDayId, other.itineraryDayId)
                    && Objects.equals(locale, other.locale);
        }

        @Override
        public int hashCode() {
            return Objects.hash(itineraryDayId, locale);
        }
    }
}
