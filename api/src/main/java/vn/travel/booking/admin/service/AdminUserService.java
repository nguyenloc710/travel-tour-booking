package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.StaffUserView;
import vn.travel.booking.admin.repository.AdminUserRepository;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.NotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Người dùng và vai trò — docs/22 M14, <b>chỉ {@code ADMIN}</b>.
 *
 * <p>Lớp này có đúng một luật đáng nói, và nó là luật giữ cho hệ thống mở được
 * cửa: <b>không tự khoá mình ra ngoài</b>.
 */
@Service
public class AdminUserService {

    private static final String ADMIN = "ADMIN";

    private final AdminUserRepository nguoiDung;

    public AdminUserService(AdminUserRepository nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    @Transactional(readOnly = true)
    public List<StaffUserView> danhSach() {
        return nguoiDung.findAll();
    }

    /**
     * Bật hoặc tắt một tài khoản, đổi tên hiển thị.
     *
     * <p><b>Tắt chứ không xoá.</b> Người nghỉ việc vẫn đứng tên trong
     * {@code booking_event} và trong {@code last_modified_by} của mọi bản ghi họ
     * từng sửa; xoá họ đi là làm nhật ký kiểm toán trỏ vào hư không. Đó cũng là
     * lý do M14 không có nút Xoá, kể cả xoá mềm.
     */
    @Transactional
    public StaffUserView sua(UUID id, String displayName, Boolean isActive, UUID nhanVienId) {
        StaffUserView hienTai = phaiCo(id);

        // Tắt người cuối cùng còn mang vai trò ADMIN và đang bật thì không còn
        // ai vào lại được màn hình này, và lối ra duy nhất là UPDATE tay trên
        // cơ sở dữ liệu lúc nửa đêm.
        if (Boolean.FALSE.equals(isActive)
                && hienTai.isActive()
                && hienTai.roles().contains(ADMIN)
                && nguoiDung.demAdminKhac(id) == 0) {

            throw new AdminErrors.LastAdmin("không tắt được ADMIN đang bật cuối cùng");
        }

        nguoiDung.patch(id, displayName, isActive, nhanVienId);
        return phaiCo(id);
    }

    /**
     * Thay toàn bộ tập vai trò.
     *
     * <p>Cùng lưới an toàn với việc tắt tài khoản, và cần nó vì cùng một hậu
     * quả: gỡ vai trò {@code ADMIN} của người cuối cùng cũng đóng cửa hệ thống
     * đúng như tắt tài khoản của họ. Hai đường vào, một luật.
     */
    @Transactional
    public StaffUserView datVaiTro(UUID id, List<String> roles) {
        StaffUserView hienTai = phaiCo(id);

        boolean dangGoAdmin = hienTai.roles().contains(ADMIN) && !roles.contains(ADMIN);
        if (dangGoAdmin && hienTai.isActive() && nguoiDung.demAdminKhac(id) == 0) {
            throw new AdminErrors.LastAdmin("không gỡ được vai trò của ADMIN cuối cùng");
        }

        nguoiDung.datVaiTro(id, roles.stream().distinct().toList());
        return phaiCo(id);
    }

    private StaffUserView phaiCo(UUID id) {
        return nguoiDung.find(id)
                .orElseThrow(() -> new NotFoundException("staff_user id=" + id));
    }
}
