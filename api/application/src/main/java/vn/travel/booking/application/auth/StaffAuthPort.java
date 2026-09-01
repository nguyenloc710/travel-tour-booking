package vn.travel.booking.application.auth;

import java.util.Optional;

public interface StaffAuthPort {

    /**
     * Rỗng khi: không có tài khoản, tài khoản đã tắt, hoặc đã xoá mềm. Ba tình
     * huống trả cùng một kết quả — phân biệt chúng ra ngoài là cho phép dò xem
     * địa chỉ email nào có trong hệ thống.
     */
    Optional<StaffCredentials> findByEmail(String email);
}
