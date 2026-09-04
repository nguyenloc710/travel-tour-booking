package vn.travel.booking.admin.dto;

import java.util.List;
import java.util.UUID;

/**
 * Một người dùng của trang quản trị — docs/22 M14.
 *
 * <p><b>Không có trường nào cho mật khẩu</b>, kể cả dạng băm. Đưa băm ra khỏi
 * máy chủ là biến một lần rò rỉ log thành một lần rò rỉ mật khẩu.
 */
public record StaffUserView(
        UUID id,
        String email,
        String displayName,
        boolean isActive,
        List<String> roles) {
}
