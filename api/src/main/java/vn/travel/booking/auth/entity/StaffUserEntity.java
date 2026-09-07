package vn.travel.booking.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.travel.booking.common.entity.BaseEntity;

import java.util.UUID;

/**
 * Bảng {@code staff_user} có từ {@code V1}; lược đồ do Flyway sở hữu và JPA chạy
 * {@code ddl-auto: validate} — Hibernate không được tạo hay sửa bảng nào.
 */
@Entity
@Table(name = "staff_user")
public class StaffUserEntity extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected StaffUserEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }
}
