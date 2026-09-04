package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.AdminBookingDetailView;
import vn.travel.booking.admin.dto.AdminBookingQuery;
import vn.travel.booking.admin.dto.AdminBookingRow;
import vn.travel.booking.admin.repository.AdminBookingRepository;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.exception.NotFoundException;

/**
 * Vận hành đơn — danh sách (docs/22 M6) và chi tiết (M7).
 *
 * <p>Chỉ <b>đọc</b>. Đổi trạng thái đơn là đường ghi, và ma trận quyền docs/22
 * mục 2.1 tách hẳn hai việc: {@code CONSULTANT} và {@code ADMIN} đều xem được
 * đơn, nhưng "đổi trạng thái, huỷ, hoàn" là một dòng riêng trong ma trận đó.
 * Gộp hai thứ vào một service là bước đầu để gộp luôn quyền.
 */
@Service
public class AdminBookingService {

    private final AdminBookingRepository don;

    public AdminBookingService(AdminBookingRepository don) {
        this.don = don;
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminBookingRow> danhSach(AdminBookingQuery query) {
        return don.findBookings(query);
    }

    @Transactional(readOnly = true)
    public AdminBookingDetailView chiTiet(String reference) {
        return don.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("không có đơn nào mang mã " + reference));
    }
}
