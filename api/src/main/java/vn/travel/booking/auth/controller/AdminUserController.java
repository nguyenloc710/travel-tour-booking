package vn.travel.booking.auth.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.admin.service.AdminUserService;
import vn.travel.booking.auth.mapper.AdminUserMapper;
import vn.travel.booking.common.util.SecurityUtils;
import vn.travel.booking.web.generated.model.AdminRoleAssignment;
import vn.travel.booking.web.generated.model.AdminStaffUser;
import vn.travel.booking.web.generated.model.AdminStaffUserPatch;

import java.util.List;
import java.util.UUID;

import static vn.travel.booking.common.util.AdminResponses.noCache;

/**
 * Người dùng và vai trò — M14 của {@code docs/22}.
 *
 * <p>Dòng cuối cùng của ma trận {@code docs/22} mục 2.1: <b>chỉ {@code ADMIN}</b>,
 * cả đọc lẫn ghi. Ba vai trò kia không thấy màn hình — danh sách người dùng là
 * bản đồ của chính hệ thống phân quyền, và đọc được nó là biết nên nhắm vào ai.
 *
 * <p>Không có endpoint <b>tạo</b> người dùng, và đó là trạng thái hiện tại của
 * hợp đồng chứ không phải thiếu sót ở lớp này — {@code docs/22} mục 9.1.
 */
@RestController
@Validated
public class AdminUserController {

    private final AdminUserService users;
    private final AdminUserMapper mapper;

    public AdminUserController(AdminUserService users, AdminUserMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api/v1/admin/users",
            produces = {"application/json"}
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminStaffUser>> listUsers() {
        return noCache().body(users.list().stream().map(mapper::toView).toList());
    }

    @RequestMapping(
            method = RequestMethod.PATCH,
            value = "/api/v1/admin/users/{id}",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStaffUser> updateUser(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminStaffUserPatch input
    ) {
        return noCache().body(mapper.toView(users.update(
                id, input.getDisplayName(), input.getIsActive(),
                SecurityUtils.currentStaff().id())));
    }

    @RequestMapping(
            method = RequestMethod.PUT,
            value = "/api/v1/admin/users/{id}/roles",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStaffUser> setRoles(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminRoleAssignment input
    ) {
        return noCache().body(mapper.toView(users.setRoles(
                id, input.getRoles().stream()
                        .map(AdminRoleAssignment.RolesEnum::getValue).toList())));
    }
}
