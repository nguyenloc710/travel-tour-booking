package vn.travel.booking.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.auth.entity.StaffUserEntity;

import java.util.Optional;
import java.util.UUID;

public interface StaffUserRepository extends JpaRepository<StaffUserEntity, UUID> {

    /**
     * Điều kiện {@code softDelete = false} viết <b>tường minh</b> trong tên
     * phương thức, không giấu sau {@code @SQLRestriction} — ADR-003.
     */
    Optional<StaffUserEntity> findByEmailAndActiveTrueAndSoftDeleteFalse(String email);
}
