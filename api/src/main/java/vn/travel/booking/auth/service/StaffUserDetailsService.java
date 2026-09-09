package vn.travel.booking.auth.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.auth.dto.StaffCredentials;
import vn.travel.booking.auth.dto.StaffPrincipal;
import vn.travel.booking.auth.entity.StaffUserEntity;
import vn.travel.booking.auth.repository.StaffRoleRepository;
import vn.travel.booking.auth.repository.StaffUserRepository;

import java.util.LinkedHashSet;
import java.util.Locale;

/**
 * Nạp nhân viên cho Spring Security.
 *
 * <p>Trả <b>cùng một ngoại lệ</b> cho "không có tài khoản này", "tài khoản đã
 * tắt" và "tài khoản đã xoá mềm": phân biệt ba câu đó ra ngoài là cho phép dò
 * xem địa chỉ email nào có trong hệ thống.
 */
@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffUserRepository staff;
    private final StaffRoleRepository roles;

    public StaffUserDetailsService(StaffUserRepository staff, StaffRoleRepository roles) {
        this.staff = staff;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffPrincipal loadUserByUsername(String email) {
        StaffUserEntity u = staff
                .findByEmailAndActiveTrueAndSoftDeleteFalse(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("không nạp được nhân viên"));

        return new StaffPrincipal(new StaffCredentials(
                u.getId(), u.getEmail(), u.getDisplayName(),
                new LinkedHashSet<>(roles.findRoleCodes(u.getId())),
                u.getPasswordHash()));
    }
}
