package vn.travel.booking.infrastructure.auth;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.auth.StaffAuthPort;
import vn.travel.booking.application.auth.StaffCredentials;
import vn.travel.booking.infrastructure.auth.repository.StaffRoleRepository;
import vn.travel.booking.infrastructure.auth.repository.StaffUserRepository;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;

@Repository
public class JpaStaffAuthAdapter implements StaffAuthPort {

    private final StaffUserRepository nhanVien;
    private final StaffRoleRepository vaiTro;

    public JpaStaffAuthAdapter(StaffUserRepository nhanVien, StaffRoleRepository vaiTro) {
        this.nhanVien = nhanVien;
        this.vaiTro = vaiTro;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StaffCredentials> findByEmail(String email) {
        return nhanVien.findByEmailAndActiveTrueAndSoftDeleteFalse(email.toLowerCase(Locale.ROOT))
                .map(u -> new StaffCredentials(
                        u.getId(), u.getEmail(), u.getDisplayName(),
                        new LinkedHashSet<>(vaiTro.findRoleCodes(u.getId())),
                        u.getPasswordHash()));
    }
}
