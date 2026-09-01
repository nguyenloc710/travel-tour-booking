package vn.travel.booking.domain.departure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vn.travel.booking.domain.departure.BaseDepartureStatus.FEW_SEATS;
import static vn.travel.booking.domain.departure.BaseDepartureStatus.OPEN;
import static vn.travel.booking.domain.departure.BaseDepartureStatus.PENDING;
import static vn.travel.booking.domain.departure.BaseDepartureStatus.SOLD_OUT;

/**
 * JUnit thuần, không context Spring, không cơ sở dữ liệu — chạy trong vài mili
 * giây. Đây là loại test mà docs/10 mục 3 muốn có nhiều nhất.
 */
class DepartureStatusesTest {

    private static final int NGUONG_IT_CHO = 3;   // docs/14 mục 2.4

    @Test
    @DisplayName("Còn nhiều chỗ, chưa đủ khách đảm bảo → OPEN")
    void mo() {
        assertEquals(DepartureStatus.OPEN, giai(OPEN, 4, 10, 12));
    }

    @Test
    @DisplayName("PENDING thắng mọi thứ — chưa mở bán thì số chỗ không có nghĩa")
    void pendingThangHet() {
        assertEquals(DepartureStatus.PENDING, giai(PENDING, 20, 10, 12));
    }

    @Test
    @DisplayName("Nhân viên đóng bán tay thì SOLD_OUT, dù còn chỗ")
    void nhanVienDongBan() {
        assertEquals(DepartureStatus.SOLD_OUT, giai(SOLD_OUT, 2, 18, 12));
    }

    @Test
    @DisplayName("Hết chỗ khả dụng thì SOLD_OUT, dù cơ sở dữ liệu ghi OPEN")
    void hetChoThucTe() {
        assertEquals(DepartureStatus.SOLD_OUT, giai(OPEN, 20, 0, 12));
    }

    @Test
    @DisplayName("GUARANTEED xét TRƯỚC FEW_SEATS — vừa đủ khách vừa còn ít chỗ thì hiện đảm bảo khởi hành")
    void guaranteedTruocFewSeats() {
        // 14 khách đã đặt, còn đúng 2 chỗ: cả hai điều kiện cùng đúng.
        assertEquals(DepartureStatus.GUARANTEED, giai(OPEN, 14, 2, 12),
                "Đảo hai bước này không làm gãy gì, chỉ làm mất doanh thu âm thầm");
    }

    @Test
    @DisplayName("Đủ khách đảm bảo, còn nhiều chỗ → vẫn GUARANTEED")
    void duKhachConNhieuCho() {
        assertEquals(DepartureStatus.GUARANTEED, giai(OPEN, 12, 8, 12));
    }

    @Test
    @DisplayName("Chưa đủ khách đảm bảo, còn ít chỗ → FEW_SEATS")
    void conItCho() {
        assertEquals(DepartureStatus.FEW_SEATS, giai(OPEN, 8, 3, 12));
    }

    @Test
    @DisplayName("Nhân viên ghi đè FEW_SEATS được, dù còn nhiều chỗ")
    void ghiDeFewSeats() {
        assertEquals(DepartureStatus.FEW_SEATS, giai(FEW_SEATS, 5, 15, 12));
    }

    @Test
    @DisplayName("Nhân viên KHÔNG ghi đè được GUARANTEED xuống FEW_SEATS")
    void khongGhiDeDuocGuaranteed() {
        assertEquals(DepartureStatus.GUARANTEED, giai(FEW_SEATS, 14, 6, 12),
                "Đã đủ khách để chắc chắn đi thì không ai đóng nhẹ nó lại");
    }

    @Test
    @DisplayName("Loại sản phẩm không có ngưỡng đảm bảo thì bỏ qua bước GUARANTEED")
    void khongCoNguongDamBao() {
        // Du thuyền: product_cruise không có cột guaranteed_threshold.
        assertEquals(DepartureStatus.OPEN, giai(OPEN, 30, 10, null));
        assertEquals(DepartureStatus.FEW_SEATS, giai(OPEN, 30, 2, null));
    }

    @Test
    @DisplayName("Ngưỡng ít chỗ là tham số, không phải hằng số nằm trong lõi")
    void nguongLaThamSo() {
        assertEquals(DepartureStatus.OPEN,
                DepartureStatuses.resolve(OPEN, 4, 5, 12, 3));
        assertEquals(DepartureStatus.FEW_SEATS,
                DepartureStatuses.resolve(OPEN, 4, 5, 12, 5),
                "Đổi ngưỡng của một thị trường không được kéo theo sửa lõi nghiệp vụ");
    }

    private static DepartureStatus giai(BaseDepartureStatus co_ban, int daDat, int conLai, Integer nguong) {
        return DepartureStatuses.resolve(co_ban, daDat, conLai, nguong, NGUONG_IT_CHO);
    }
}
