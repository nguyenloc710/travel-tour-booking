package vn.travel.booking.booking;

import vn.travel.booking.booking.dto.BookingStatus;
import vn.travel.booking.booking.service.BookingStatuses;
import vn.travel.booking.common.exception.IllegalBookingTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vn.travel.booking.booking.dto.BookingStatus.CANCELLED;
import static vn.travel.booking.booking.dto.BookingStatus.COMPLETED;
import static vn.travel.booking.booking.dto.BookingStatus.CONFIRMED;
import static vn.travel.booking.booking.dto.BookingStatus.DRAFT;
import static vn.travel.booking.booking.dto.BookingStatus.EXPIRED;
import static vn.travel.booking.booking.dto.BookingStatus.PENDING_CONFIRMATION;
import static vn.travel.booking.booking.dto.BookingStatus.PENDING_PAYMENT;
import static vn.travel.booking.booking.dto.BookingStatus.REFUNDED;

class BookingStatusesTest {

    @Test
    @DisplayName("Đường đi bình thường của một đơn có tồn kho")
    void normalPathWithInventory() {
        assertTrue(BookingStatuses.canTransitionTo(DRAFT, PENDING_PAYMENT));
        assertTrue(BookingStatuses.canTransitionTo(PENDING_PAYMENT, CONFIRMED));
        assertTrue(BookingStatuses.canTransitionTo(CONFIRMED, COMPLETED));
    }

    @Test
    @DisplayName("INDIVIDUAL_PACKAGE: trả tiền xong vẫn phải qua PENDING_CONFIRMATION")
    void individualPackageNeedsManualConfirmation() {
        assertTrue(BookingStatuses.canTransitionTo(PENDING_PAYMENT, PENDING_CONFIRMATION));
        assertTrue(BookingStatuses.canTransitionTo(PENDING_CONFIRMATION, CONFIRMED));
    }

    @Test
    @DisplayName("KHÔNG có bước lùi: CANCELLED không quay lại CONFIRMED")
    void noBackwardTransitions() {
        assertFalse(BookingStatuses.canTransitionTo(CANCELLED, CONFIRMED));
        assertFalse(BookingStatuses.canTransitionTo(EXPIRED, PENDING_PAYMENT));
        assertFalse(BookingStatuses.canTransitionTo(COMPLETED, CONFIRMED));
        assertFalse(BookingStatuses.canTransitionTo(REFUNDED, CANCELLED));
    }

    @Test
    @DisplayName("Không đường nào nhảy cóc từ DRAFT thẳng tới CONFIRMED")
    void noSkippingFromDraftToConfirmed() {
        assertFalse(BookingStatuses.canTransitionTo(DRAFT, CONFIRMED),
                "Chưa qua bước thanh toán thì không có gì để xác nhận");
        assertFalse(BookingStatuses.canTransitionTo(DRAFT, COMPLETED));
    }

    @Test
    @DisplayName("Huỷ được ở mọi trạng thái còn sống, không huỷ được ở trạng thái đã chốt")
    void cancellableOnlyWhileOpen() {
        for (BookingStatus s : List.of(DRAFT, PENDING_PAYMENT, PENDING_CONFIRMATION, CONFIRMED)) {
            assertTrue(BookingStatuses.canTransitionTo(s, CANCELLED), "Phải huỷ được từ " + s);
        }
        assertFalse(BookingStatuses.canTransitionTo(COMPLETED, CANCELLED),
                "Đi xong rồi thì không huỷ được nữa — đó là việc hoàn tiền, không phải huỷ");
    }

    @Test
    @DisplayName("Hoàn tiền chỉ đi sau khi đã huỷ")
    void refundOnlyAfterCancel() {
        assertTrue(BookingStatuses.canTransitionTo(CANCELLED, REFUNDED));
        assertFalse(BookingStatuses.canTransitionTo(CONFIRMED, REFUNDED),
                "Hoàn tiền cho một đơn còn hiệu lực là bỏ sót bước huỷ");
    }

    @Test
    @DisplayName("Bốn trạng thái cuối là ngõ cụt")
    void fourFinalStatesAreDeadEnds() {
        assertTrue(BookingStatuses.isClosed(COMPLETED));
        assertTrue(BookingStatuses.isClosed(REFUNDED));
        assertTrue(BookingStatuses.isClosed(EXPIRED));
        assertFalse(BookingStatuses.isClosed(CANCELLED), "CANCELLED còn một đường: hoàn tiền");
        assertFalse(BookingStatuses.isClosed(CONFIRMED));
    }

    @Test
    @DisplayName("Bước chuyển sai ném lỗi, không im lặng bỏ qua")
    void invalidTransitionThrows() {
        assertThrows(IllegalBookingTransitionException.class,
                () -> BookingStatuses.requireTransition(CANCELLED, CONFIRMED));
    }

    @Test
    @DisplayName("Trạng thái nào đang chiếm chỗ trong kho — huỷ là trả chỗ ngay")
    void statesHoldingInventoryReleaseOnCancel() {
        assertTrue(BookingStatuses.dangChiemCho(PENDING_PAYMENT));
        assertTrue(BookingStatuses.dangChiemCho(CONFIRMED));

        assertFalse(BookingStatuses.dangChiemCho(CANCELLED),
                "Chỗ về kho ngay lúc huỷ, không chờ hoàn tiền xong — docs/14 mục 6.5");
        assertFalse(BookingStatuses.dangChiemCho(EXPIRED));
        assertFalse(BookingStatuses.dangChiemCho(DRAFT),
                "Đơn nháp chưa chiếm chỗ; chỗ đang nằm ở seat_hold");
    }
}
