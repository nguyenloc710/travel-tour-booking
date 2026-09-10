package vn.travel.booking.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Chữ thay ảnh, theo từng ngôn ngữ — khoá chính kép {@code (asset_id, locale)}.
 *
 * <p>Ảnh dùng chung cho mọi ngôn ngữ; {@code alt} thì <b>không</b> (docs/11 mục
 * 6). Và đây không phải siêu dữ liệu kỹ thuật: nó là thứ người khiếm thị đọc
 * thay cho ảnh, nên nó là <b>nội dung phải dịch</b> (docs/24 mục 6).
 *
 * <p>Hệ quả dễ gây ngạc nhiên: ảnh không có {@code alt} ở locale nào thì
 * <b>biến mất</b> khỏi bộ ảnh ở locale đó — đúng luật không fallback cho nội
 * dung bán hàng. Hiện nó kèm {@code alt} tiếng Đan giữa trang tiếng Việt còn tệ
 * hơn: trình đọc màn hình sẽ đọc đúng câu đó.
 */
@Entity
@Table(name = "media_asset_translation")
@IdClass(MediaAssetTranslationEntity.CompositeId.class)
public class MediaAssetTranslationEntity {

    @Id
    @Column(name = "asset_id")
    private UUID assetId;

    @Id
    private String locale;

    @Column(nullable = false)
    private String alt;

    protected MediaAssetTranslationEntity() {
    }

    public MediaAssetTranslationEntity(UUID assetId, String locale, String alt) {
        this.assetId = assetId;
        this.locale = locale;
        this.alt = alt;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public String getLocale() {
        return locale;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public static class CompositeId implements Serializable {

        private UUID assetId;
        private String locale;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CompositeId other)) {
                return false;
            }
            return Objects.equals(assetId, other.assetId) && Objects.equals(locale, other.locale);
        }

        @Override
        public int hashCode() {
            return Objects.hash(assetId, locale);
        }
    }
}
