package vn.travel.booking.auth.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Nhân viên cùng chuỗi băm mật khẩu, đọc từ cơ sở dữ liệu để xác thực.
 *
 * <p>Bản ghi thuần, không biết Spring Security tồn tại: tầng {@code web} bọc nó
 * lại thành {@code UserDetails}, tầng {@code infrastructure} đọc nó ra từ JPA.
 * Không tầng nào phải biết tầng kia.
 */
public record StaffCredentials(
        UUID id,
        String email,
        String displayName,
        Set<String> roles,
        String passwordHash) {
}
