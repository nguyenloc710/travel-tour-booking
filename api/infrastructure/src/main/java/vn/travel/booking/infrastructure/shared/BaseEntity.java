package vn.travel.booking.infrastructure.shared;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bốn cột kiểm toán cộng cờ xoá mềm, dùng chung cho mọi entity nhóm A
 * (docs/11 mục 11.2).
 *
 * <p><b>Cố tình KHÔNG có {@code @CreatedDate} lẫn {@code @LastModifiedDate}.</b>
 * Hai cột thời gian do <b>cơ sở dữ liệu</b> đặt: {@code created_at} bằng
 * {@code DEFAULT now()}, {@code last_modified_at} bằng trigger
 * {@code tg_*_last_modified} (docs/11 mục 11.1). Bật thêm ở tầng Java nghĩa là
 * hai chỗ cùng ghi một cột, và chỗ nào thắng thì phụ thuộc thứ tự — đúng loại
 * lỗi không ai truy ra được. Vì thế cả hai trường
 * {@code insertable = false, updatable = false}: Java đọc được, không ghi được.
 *
 * <p>{@code created_by} và {@code last_modified_by} thì ngược lại — CSDL không
 * biết ai đang thao tác, nên ứng dụng phải điền, qua {@code AuditorAware}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "last_modified_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime lastModifiedAt;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private UUID lastModifiedBy;

    @Column(name = "soft_delete", nullable = false)
    private boolean softDelete;

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    public boolean isSoftDelete() {
        return softDelete;
    }

    public void setSoftDelete(boolean softDelete) {
        this.softDelete = softDelete;
    }
}
