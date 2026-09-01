package vn.travel.booking.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.travel.booking.auth.entity.StaffUserEntity;

import java.util.List;
import java.util.UUID;

/**
 * Vai trò đọc bằng truy vấn gốc thay vì ánh xạ quan hệ nhiều-nhiều: quan hệ này
 * chỉ đọc một chiều, một lần, lúc đăng nhập. Một {@code @ManyToMany} kéo theo
 * bảng nối trong ánh xạ, tải lười, và một cách vô tình để ai đó ghi vào nó qua
 * entity — trong khi cấp và thu quyền phải đi qua đúng một chỗ có kiểm.
 */
public interface StaffRoleRepository extends JpaRepository<StaffUserEntity, UUID> {

    @Query(value = """
            SELECT role_code FROM staff_user_role
            WHERE staff_user_id = :id AND NOT soft_delete
            """, nativeQuery = true)
    List<String> findRoleCodes(@Param("id") UUID staffUserId);
}
