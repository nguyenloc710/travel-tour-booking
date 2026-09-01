package vn.travel.booking.domain.booking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vn.travel.booking.domain.booking.BookingStatus.CANCELLED;
import static vn.travel.booking.domain.booking.BookingStatus.COMPLETED;
import static vn.travel.booking.domain.booking.BookingStatus.CONFIRMED;
import static vn.travel.booking.domain.booking.BookingStatus.DRAFT;
import static vn.travel.booking.domain.booking.BookingStatus.EXPIRED;
import static vn.travel.booking.domain.booking.BookingStatus.PENDING_CONFIRMATION;
import static vn.travel.booking.domain.booking.BookingStatus.PENDING_PAYMENT;
import static vn.travel.booking.domain.booking.BookingStatus.REFUNDED;

class BookingStatusesTest {

    @Test
    @DisplayName("Đường đi bình thường của một đơn có tồn kho")
    void duongDiBinhThuong() {
        assertTrue(BookingStatuses.diDuoc(DRAFT, PENDING_PAYMENT));
        assertTrue(BookingStatuses.diDuoc(PENDING_PAYMENT, CONFIRMED));
        assertTrue(BookingStatuses.diDuoc(CONFIRMED, COMPLETED));
    }

    @Test
    @DisplayName("INDIVIDUAL_PACKAGE: trả tiền xong vẫn phải qua PENDING_CONFIRMATION")
    void duongDiCanXacNhanTay() {
        assertTrue(BookingStatuses.diDuoc(PENDING_PAYMENT, PENDING_CONFIRMATION));
        assertTrue(BookingStatuses.diDuoc(PENDING_CONFIRMATION, CONFIRMED));
    }

    @Test
    @DisplayName("KHÔNG có bước lùi: CANCELLED không quay lại CONFIRMED")
    void khongCoBuocLui() {
        assertFalse(BookingStatuses.diDuoc(CANCELLED, CONFIRMED));
        assertFalse(BookingStatuses.diDuoc(EXPIRED, PENDING_PAYMENT));
        assertFalse(BookingStatuses.diDuoc(COMPLETED, CONFIRMED));
        assertFalse(BookingStatuses.diDuoc(REFUNDED, CANCELLED));
    }

    @Test
    @DisplayName("Không đường nào nhảy cóc từ DRAFT thẳng tới CONFIRMED")
    void khongNhayCoc() {
        assertFalse(BookingStatuses.diDuoc(DRAFT, CONFIRMED),
                "Chưa qua bước thanh toán thì không có gì để xác nhận");
        assertFalse(BookingStatuses.diDuoc(DRAFT, COMPLETED));
    }

    @Test
    @DisplayName("Huỷ được ở mọi trạng thái còn sống, không huỷ được ở trạng thái đã chốt")
    void huyDuocOTrangThaiConSong() {
        for (BookingStatus s : List.of(DRAFT, PENDING_PAYMENT, PENDING_CONFIRMATION, CONFIRMED)) {
            assertTrue(BookingStatuses.diDuoc(s, CANCELLED), "Phải huỷ được từ " + s);
        }
        assertFalse(BookingStatuses.diDuoc(COMPLETED, CANCELLED),
                "Đi xong rồi thì không huỷ được nữa — đó là việc hoàn tiền, không phải huỷ");
    }

    @Test
    @DisplayName("Hoàn tiền chỉ đi sau khi đã huỷ")
    void hoanTienSauKhiHuy() {
        assertTrue(BookingStatuses.diDuoc(CANCELLED, REFUNDED));
        assertFalse(BookingStatuses.diDuoc(CONFIRMED, REFUNDED),
                "Hoàn tiền cho một đơn còn hiệu lực là bỏ sót bước huỷ");
    }

    @Test
    @DisplayName("Bốn trạng thái cuối là ngõ cụt")
    void trangThaiDaChot() {
        assertTrue(BookingStatuses.daChot(COMPLETED));
        assertTrue(BookingStatuses.daChot(REFUNDED));
        assertTrue(BookingStatuses.daChot(EXPIRED));
        assertFalse(BookingStatuses.daChot(CANCELLED), "CANCELLED còn một đường: hoàn tiền");
        assertFalse(BookingStatuses.daChot(CONFIRMED));
    }

    @Test
    @DisplayName("Bước chuyển sai ném lỗi, không im lặng bỏ qua")
    void buocSaiNemLoi() {
        assertThrows(IllegalBookingTransitionException.class,
                () -> BookingStatuses.phaiDiDuoc(CANCELLED, CONFIRMED));
    }

    @Test
    @DisplayName("Trạng thái nào đang chiếm chỗ trong kho — huỷ là trả chỗ ngay")
    void trangThaiDangChiemCho() {
        assertTrue(BookingStatuses.dangChiemCho(PENDING_PAYMENT));
        assertTrue(BookingStatuses.dangChiemCho(CONFIRMED));

        assertFalse(BookingStatuses.dangChiemCho(CANCELLED),
                "Chỗ về kho ngay lúc huỷ, không chờ hoàn tiền xong — docs/14 mục 6.5");
        assertFalse(BookingStatuses.dangChiemCho(EXPIRED));
        assertFalse(BookingStatuses.dangChiemCho(DRAFT),
                "Đơn nháp chưa chiếm chỗ; chỗ đang nằm ở seat_hold");
    }
}
