package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.AdminBookingDetailView;
import vn.travel.booking.admin.dto.AdminBookingQuery;
import vn.travel.booking.admin.dto.AdminBookingRow;
import vn.travel.booking.admin.repository.AdminBookingRepository;
import vn.travel.booking.booking.dto.BookingStatus;
import vn.travel.booking.booking.service.BookingStatuses;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.exception.NotFoundException;

import java.util.UUID;

/**
 * Vận hành đơn — danh sách (docs/22 M6) và chi tiết (M7).
 *
 * <p>Ma trận quyền docs/22 mục 2.1 giữ "xem đơn" và "đổi trạng thái, huỷ, hoàn"
 * ở <b>hai dòng riêng</b>. Ở v1 hai dòng đó trùng vai trò ({@code CONSULTANT} và
 * {@code ADMIN}), nhưng chúng vẫn là hai câu hỏi khác nhau và được kiểm riêng ở
 * controller — ngày đội lớn lên và tách vai trò thì không phải viết lại.
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

    /**
     * Nhân viên đổi trạng thái một đơn — docs/22 mục 6, máy trạng thái docs/23
     * mục 4.
     *
     * <p>Bốn việc trong <b>một</b> transaction, và thứ tự không đổi được:
     * khoá dòng đơn, kiểm bước chuyển, ghi trạng thái mới cùng một dòng nhật ký,
     * rồi trả chỗ về kho nếu đơn vừa rời khỏi nhóm đang chiếm chỗ.
     *
     * <p><b>Ghi nhật ký không phải bước phụ.</b> Tách nó ra ngoài transaction —
     * hay tệ hơn, để chỗ gọi tự nhớ — nghĩa là sẽ có đường đi đổi được trạng
     * thái mà không để lại vết, và đó đúng là thứ khiến `booking_event` mất giá
     * trị khi có tranh chấp.
     *
     * <p>Số chỗ trả về kho lấy từ số dòng {@code booking_passenger}. Lúc đặt,
     * bộ đếm được cộng bằng tổng bản đồ {@code pax} — hai con số này <b>đáng lẽ
     * luôn bằng nhau</b> nhưng chưa có ràng buộc nào bắt buộc thế; xem ghi chú
     * cho người sau ở docs/41 mục 6.2.
     */
    @Transactional
    public AdminBookingDetailView doiTrangThai(String reference, BookingStatus sang,
                                               UUID nhanVienId, String note) {

        AdminBookingRepository.DonDeDoi hienTai = don.khoaDon(reference)
                .orElseThrow(() -> new NotFoundException("không có đơn nào mang mã " + reference));

        // Hàm thuần, không context, không CSDL — 9 test JUnit đã canh nó.
        BookingStatuses.phaiDiDuoc(hienTai.status(), sang);

        don.datTrangThai(hienTai.id(), sang, nhanVienId);
        don.ghiNhatKy(hienTai.id(), hienTai.status(), sang, nhanVienId, note);

        // Rời nhóm đang chiếm chỗ thì trả chỗ NGAY, không chờ hoàn tiền xong
        // (docs/14 mục 6.5). CONFIRMED → CANCELLED trả chỗ; CANCELLED →
        // REFUNDED thì không, vì chỗ đã về kho từ bước trước rồi.
        boolean vuaNhaCho = BookingStatuses.dangChiemCho(hienTai.status())
                && !BookingStatuses.dangChiemCho(sang);
        if (vuaNhaCho && hienTai.departureId() != null) {
            don.traChoVeKho(hienTai.departureId(), hienTai.paxCount());
        }

        return don.findByReference(reference)
                .orElseThrow(() -> new IllegalStateException(
                        "đơn " + reference + " biến mất giữa transaction"));
    }
}
