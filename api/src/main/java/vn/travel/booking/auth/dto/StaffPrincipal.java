package vn.travel.booking.auth.dto;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.travel.booking.auth.dto.StaffCredentials;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Nhân viên đang đăng nhập, dưới dạng Spring Security hiểu được.
 *
 * <p>Nằm ở tầng {@code web} vì {@code UserDetails} là khái niệm của Spring
 * Security, và Spring Security là chi tiết của tầng này. Tầng dưới chỉ biết tới
 * {@link StaffCredentials} — một bản ghi thuần.
 *
 * <p>Là lớp chứ không phải record vì nó phải <b>xoá được mật khẩu</b>: Spring
 * gọi {@link #eraseCredentials()} ngay sau khi xác thực xong, nên chuỗi băm
 * không nằm lại trong phiên suốt cả buổi làm việc.
 */
public final class StaffPrincipal implements UserDetails, CredentialsContainer {

    private final StaffCredentials thongTin;
    private String passwordHash;

    public StaffPrincipal(StaffCredentials thongTin) {
        this.thongTin = thongTin;
        this.passwordHash = thongTin.passwordHash();
    }

    public UUID id() {
        return thongTin.id();
    }

    public String email() {
        return thongTin.email();
    }

    public String displayName() {
        return thongTin.displayName();
    }

    public List<String> roleList() {
        return thongTin.roles().stream().sorted().toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Tiền tố ROLE_ là quy ước của Spring Security cho hasRole(...).
        return thongTin.roles().stream()
                .map(v -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + v))
                .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return thongTin.email();
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
