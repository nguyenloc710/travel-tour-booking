package vn.travel.booking.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Một tệp trong kho ảnh, kèm chứng từ giấy phép của nó.
 *
 * <p><b>Không kế thừa {@code BaseEntity}.</b> Bảng {@code media_asset} cố tình
 * không có cột kiểm toán và không có {@code soft_delete} — docs/11 mục 11.2.
 * Ảnh không phải nội dung biên tập theo phiên bản; nó là tệp, và chứng từ giấy
 * phép của nó phải sống lâu hơn mọi bản ghi trỏ vào nó (vì thế
 * {@code product_image.asset_id} không có {@code ON DELETE CASCADE}).
 *
 * <p>{@code path} là đường dẫn <b>tương đối</b>, không bao giờ là URL đầy đủ —
 * ADR-011 mục 2. Ghép thành địa chỉ tải được là việc của {@code DiaChiKho}.
 */
@Entity
@Table(name = "media_asset")
public class MediaAssetEntity {

    @Id
    private UUID id;

    /** {@code IMAGE} hoặc {@code VIDEO} — V9. Ảnh bìa của video là một dòng IMAGE riêng. */
    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String path;

    @Column(name = "content_type")
    private String contentType;

    /** Chỉ {@code VIDEO} có — ràng buộc {@code ck_media_video} cưỡng chế cả hai chiều. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Ảnh bìa, bắt buộc với video.
     *
     * <p>"Phải là một ẢNH, không phải video khác" là điều kiện trên dòng khác nên
     * {@code CHECK} không nói được — luật đó sống ở {@code MediaService}.
     */
    @Column(name = "poster_asset_id")
    private UUID posterAssetId;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "byte_size", nullable = false)
    private Integer byteSize;

    /** {@code SELF} · {@code PARTNER} · {@code PURCHASED} · {@code CUSTOMER}. */
    @Column(nullable = false)
    private String source;

    /**
     * Bắt buộc khi {@code source} khác {@code SELF} — ràng buộc
     * {@code ck_media_licence} cưỡng chế ở tầng CSDL, và tầng nghiệp vụ kiểm
     * trước để lỗi chỉ được đúng ô nhập thay vì thành 500.
     */
    @Column(name = "licence_ref")
    private String licenceRef;

    @Column(name = "licence_scope")
    private String licenceScope;

    @Column(name = "licence_until")
    private LocalDate licenceUntil;

    @Column(name = "person_consent", nullable = false)
    private boolean personConsent;

    protected MediaAssetEntity() {
    }

    public MediaAssetEntity(UUID id, String kind, String path, int width, int height,
                            int byteSize, String source) {
        this.id = id;
        this.kind = kind;
        this.path = path;
        this.width = width;
        this.height = height;
        this.byteSize = byteSize;
        this.source = source;
    }

    public UUID getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public String getPath() {
        return path;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public UUID getPosterAssetId() {
        return posterAssetId;
    }

    public void setPosterAssetId(UUID posterAssetId) {
        this.posterAssetId = posterAssetId;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getByteSize() {
        return byteSize;
    }

    public String getSource() {
        return source;
    }

    public String getLicenceRef() {
        return licenceRef;
    }

    public void setLicenceRef(String licenceRef) {
        this.licenceRef = licenceRef;
    }

    public String getLicenceScope() {
        return licenceScope;
    }

    public void setLicenceScope(String licenceScope) {
        this.licenceScope = licenceScope;
    }

    public LocalDate getLicenceUntil() {
        return licenceUntil;
    }

    public void setLicenceUntil(LocalDate licenceUntil) {
        this.licenceUntil = licenceUntil;
    }

    public boolean isPersonConsent() {
        return personConsent;
    }

    public void setPersonConsent(boolean personConsent) {
        this.personConsent = personConsent;
    }
}
