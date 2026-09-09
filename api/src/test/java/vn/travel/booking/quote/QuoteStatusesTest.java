package vn.travel.booking.quote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.travel.booking.common.exception.QuoteErrors;
import vn.travel.booking.quote.dto.QuoteStatus;
import vn.travel.booking.quote.service.QuoteStatuses;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Máy trạng thái báo giá — docs/14 mục 7.
 *
 * <p>JUnit thuần: không context Spring, không CSDL, không đồng hồ. Luật đi được
 * hay không là luật nghiệp vụ, và nó phải kiểm được trong vài mili giây.
 */
@DisplayName("Máy trạng thái báo giá")
class QuoteStatusesTest {

    @Nested
    @DisplayName("Đường đi hợp lệ")
    class DuongDiHopLe {

        @Test
        @DisplayName("DRAFT gửi được")
        void draftCanBeSent() {
            assertTrue(QuoteStatuses.canTransitionTo(QuoteStatus.DRAFT, QuoteStatus.SENT));
        }

        @Test
        @DisplayName("SENT đi được cả ba nhánh: khách nhận, khách từ chối, hết hạn")
        void sentHasThreeBranches() {
            assertTrue(QuoteStatuses.canTransitionTo(QuoteStatus.SENT, QuoteStatus.ACCEPTED));
            assertTrue(QuoteStatuses.canTransitionTo(QuoteStatus.SENT, QuoteStatus.REJECTED));
            assertTrue(QuoteStatuses.canTransitionTo(QuoteStatus.SENT, QuoteStatus.EXPIRED));
        }
    }

    @Nested
    @DisplayName("Đường đi bị chặn")
    class DuongDiBiChan {

        /**
         * Bài quan trọng nhất của lớp này.
         *
         * <p>Nghe thì tiện — khách gọi điện đồng ý ngay, sao phải bấm "Gửi"
         * trước — nhưng một báo giá chưa gửi thì chưa có giá nào để đồng ý, và
         * {@code valid_until} chỉ sinh ra ở bước gửi. Bỏ qua {@code SENT} là tạo
         * ra một cam kết giá <b>không có hạn</b>.
         */
        @Test
        @DisplayName("DRAFT KHÔNG nhảy thẳng sang ACCEPTED — cam kết giá phải có hạn")
        void draftCannotJumpToAccepted() {
            assertFalse(QuoteStatuses.canTransitionTo(QuoteStatus.DRAFT, QuoteStatus.ACCEPTED));

            QuoteErrors.NotAcceptable ex = assertThrows(QuoteErrors.NotAcceptable.class,
                    () -> QuoteStatuses.requireTransition(QuoteStatus.DRAFT, QuoteStatus.ACCEPTED));

            assertEquals("DRAFT", ex.params().get("from"));
            assertEquals("ACCEPTED", ex.params().get("to"));
        }

        @Test
        @DisplayName("Không có bước lùi: ba trạng thái cuối không đi đâu được nữa")
        void noBackwardTransitions() {
            for (QuoteStatus last : EnumSet.of(
                    QuoteStatus.ACCEPTED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED)) {

                assertTrue(QuoteStatuses.isClosed(last), last + " phải là trạng thái đã chốt");

                for (QuoteStatus to : QuoteStatus.values()) {
                    assertFalse(QuoteStatuses.canTransitionTo(last, to),
                            last + " không được đi sang " + to);
                }
            }
        }

        /** Quy tắc 4: hết hạn thì khách yêu cầu lại, báo giá cũ không sống lại. */
        @Test
        @DisplayName("EXPIRED không quay lại SENT — không tự gia hạn")
        void expiredDoesNotRenew() {
            assertFalse(QuoteStatuses.canTransitionTo(QuoteStatus.EXPIRED, QuoteStatus.SENT));
        }

        @Test
        @DisplayName("Không quay ngược về DRAFT từ bất cứ đâu")
        void cannotReturnToDraft() {
            for (QuoteStatus from : QuoteStatus.values()) {
                assertFalse(QuoteStatuses.canTransitionTo(from, QuoteStatus.DRAFT),
                        from + " không được quay về DRAFT");
            }
        }
    }

    @Nested
    @DisplayName("Sửa bảng giá")
    class SuaBangGia {

        @Test
        @DisplayName("Chỉ DRAFT còn sửa được bảng giá")
        void onlyDraftCanEditPriceTiers() {
            assertTrue(QuoteStatuses.canEditPriceTiers(QuoteStatus.DRAFT));

            for (QuoteStatus other : EnumSet.complementOf(EnumSet.of(QuoteStatus.DRAFT))) {
                assertFalse(QuoteStatuses.canEditPriceTiers(other),
                        other + " không được sửa bảng giá — khách đang cầm bản đã gửi");
            }
        }
    }

    @Test
    @DisplayName("DRAFT và SENT chưa chốt — còn phải theo dõi")
    void draftAndSentAreStillOpen() {
        assertFalse(QuoteStatuses.isClosed(QuoteStatus.DRAFT));
        assertFalse(QuoteStatuses.isClosed(QuoteStatus.SENT));
    }
}
