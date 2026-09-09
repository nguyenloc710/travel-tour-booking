package vn.travel.booking.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.StaffUserView;
import vn.travel.booking.web.generated.model.AdminStaffUser;

/**
 * {@code StaffUserView} sang DTO của hợp đồng — M14 của {@code docs/22}.
 *
 * <p>Bằng MapStruct chứ không map tay: thêm một trường vào
 * {@link StaffUserView} rồi quên thêm vào phép ánh xạ thì trường đó lặng lẽ
 * không bao giờ ra tới khách. MapStruct sinh mã lúc biên dịch và cảnh báo cho
 * mọi trường không được ánh xạ, nên "quên" trở thành thứ nhìn thấy được.
 *
 * <p>Vẫn <b>không có trường nào cho mật khẩu</b>, kể cả dạng băm — nguồn không
 * có, nên đích cũng không thể có.
 */
@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    AdminStaffUser toView(StaffUserView view);

    default vn.travel.booking.web.generated.model.StaffProfile toProfile(vn.travel.booking.auth.dto.StaffPrincipal staff) {
        if (staff == null) {
            return null;
        }
        return new vn.travel.booking.web.generated.model.StaffProfile(
                staff.id(),
                staff.email(),
                staff.displayName(),
                staff.roleList().stream().map(this::toProfileRole).toList());
    }

    /**
     * Vai trò lưu dạng chuỗi, hợp đồng khai dạng enum. Mã lạ ném
     * {@code IllegalArgumentException} thay vì rơi xuống {@code null} — một vai
     * trò không đọc được là dữ liệu hỏng, không phải "không có vai trò".
     */
    default AdminStaffUser.RolesEnum toRole(String code) {
        return code == null ? null : AdminStaffUser.RolesEnum.fromValue(code);
    }

    default vn.travel.booking.web.generated.model.StaffProfile.RolesEnum toProfileRole(String code) {
        return code == null ? null : vn.travel.booking.web.generated.model.StaffProfile.RolesEnum.fromValue(code);
    }
}
