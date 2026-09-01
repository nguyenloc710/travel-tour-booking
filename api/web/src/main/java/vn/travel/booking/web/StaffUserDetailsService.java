package vn.travel.booking.web;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.travel.booking.application.auth.StaffAuthPort;

/**
 * Nạp nhân viên cho Spring Security qua cổng {@code StaffAuthPort}.
 */
@Service
class StaffUserDetailsService implements UserDetailsService {

    private final StaffAuthPort nhanVien;

    StaffUserDetailsService(StaffAuthPort nhanVien) {
        this.nhanVien = nhanVien;
    }

    @Override
    public StaffPrincipal loadUserByUsername(String email) {
        return nhanVien.findByEmail(email)
                .map(StaffPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("không nạp được nhân viên"));
    }
}
