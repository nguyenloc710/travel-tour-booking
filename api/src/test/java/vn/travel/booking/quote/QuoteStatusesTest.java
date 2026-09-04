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
        void draftGuiDuoc() {
            assertTrue(QuoteStatuses.diDuoc(QuoteStatus.DRAFT, QuoteStatus.SENT));
        }

        @Test
        @DisplayName("SENT đi được cả ba nhánh: khách nhận, khách từ chối, hết hạn")
        void sentBaNhanh() {
            assertTrue(QuoteStatuses.diDuoc(QuoteStatus.SENT, QuoteStatus.ACCEPTED));
            assertTrue(QuoteStatuses.diDuoc(QuoteStatus.SENT, QuoteStatus.REJECTED));
            assertTrue(QuoteStatuses.diDuoc(QuoteStatus.SENT, QuoteStatus.EXPIRED));
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
        void draftKhongNhayThangSangAccepted() {
            assertFalse(QuoteStatuses.diDuoc(QuoteStatus.DRAFT, QuoteStatus.ACCEPTED));

            QuoteErrors.NotAcceptable ex = assertThrows(QuoteErrors.NotAcceptable.class,
                    () -> QuoteStatuses.phaiDiDuoc(QuoteStatus.DRAFT, QuoteStatus.ACCEPTED));

            assertEquals("DRAFT", ex.params().get("from"));
            assertEquals("ACCEPTED", ex.params().get("to"));
        }

        @Test
        @DisplayName("Không có bước lùi: ba trạng thái cuối không đi đâu được nữa")
        void khongCoBuocLui() {
            for (QuoteStatus cuoi : EnumSet.of(
                    QuoteStatus.ACCEPTED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED)) {

                assertTrue(QuoteStatuses.daChot(cuoi), cuoi + " phải là trạng thái đã chốt");

                for (QuoteStatus sang : QuoteStatus.values()) {
                    assertFalse(QuoteStatuses.diDuoc(cuoi, sang),
                            cuoi + " không được đi sang " + sang);
                }
            }
        }

        /** Quy tắc 4: hết hạn thì khách yêu cầu lại, báo giá cũ không sống lại. */
        @Test
        @DisplayName("EXPIRED không quay lại SENT — không tự gia hạn")
        void expiredKhongGiaHan() {
            assertFalse(QuoteStatuses.diDuoc(QuoteStatus.EXPIRED, QuoteStatus.SENT));
        }

        @Test
        @DisplayName("Không quay ngược về DRAFT từ bất cứ đâu")
        void khongVeLaiDraft() {
            for (QuoteStatus tu : QuoteStatus.values()) {
                assertFalse(QuoteStatuses.diDuoc(tu, QuoteStatus.DRAFT),
                        tu + " không được quay về DRAFT");
            }
        }
    }

    @Nested
    @DisplayName("Sửa bảng giá")
    class SuaBangGia {

        @Test
        @DisplayName("Chỉ DRAFT còn sửa được bảng giá")
        void chiDraftSuaDuoc() {
            assertTrue(QuoteStatuses.suaBangGiaDuoc(QuoteStatus.DRAFT));

            for (QuoteStatus khac : EnumSet.complementOf(EnumSet.of(QuoteStatus.DRAFT))) {
                assertFalse(QuoteStatuses.suaBangGiaDuoc(khac),
                        khac + " không được sửa bảng giá — khách đang cầm bản đã gửi");
            }
        }
    }

    @Test
    @DisplayName("DRAFT và SENT chưa chốt — còn phải theo dõi")
    void haiTrangThaiConSong() {
        assertFalse(QuoteStatuses.daChot(QuoteStatus.DRAFT));
        assertFalse(QuoteStatuses.daChot(QuoteStatus.SENT));
    }
}
