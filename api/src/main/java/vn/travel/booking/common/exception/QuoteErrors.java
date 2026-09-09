package vn.travel.booking.common.exception;

import vn.travel.booking.quote.dto.QuoteStatus;

import java.time.LocalDate;
import java.util.Map;

/**
 * Bốn tình huống nghiệp vụ của luồng báo giá — docs/13 mục 5.1.
 *
 * <p>Cùng khuôn với {@link BookingErrors}: một lớp cho một mã lỗi, để bắt nhầm
 * một loại là lỗi biên dịch chứ không phải một chuỗi gõ sai.
 */
public final class QuoteErrors {

    private QuoteErrors() {
    }

    /**
     * Loại sản phẩm không đi qua luồng báo giá. HTTP 422.
     *
     * <p>Ngược hẳn với {@link BookingErrors.ProductNotBookable}: cái kia là "loại
     * này phải hỏi giá trước", cái này là "loại này đặt thẳng được, đừng hỏi
     * giá". Hai mã riêng vì frontend dẫn khách đi hai hướng khác nhau.
     */
    public static class ProductNotQuotable extends RuntimeException {
        public ProductNotQuotable(String detail) {
            super(detail);
        }
    }

    /**
     * Ngày khách yêu cầu sớm hơn {@code leadTimeDays} của sản phẩm. HTTP 422 —
     * docs/14 mục 7 quy tắc 1.
     *
     * <p>Mang theo hai tham số để frontend dựng được câu có ích: "tour này cần
     * báo trước 7 ngày, sớm nhất là 12/09" thay vì "ngày không hợp lệ". Backend
     * vẫn không trả câu tiếng người nào.
     */
    public static class LeadTimeNotMet extends RuntimeException {

        private final transient Map<String, Object> params;

        public LeadTimeNotMet(int leadTimeDays, LocalDate somNhat) {
            super("cần báo trước " + leadTimeDays + " ngày, sớm nhất là " + somNhat);
            this.params = Map.of("leadTimeDays", leadTimeDays, "earliestDate", somNhat.toString());
        }

        public Map<String, Object> params() {
            return params;
        }
    }

    /**
     * Bảng giá ghi bằng tiền tệ không phải của thị trường báo giá. HTTP 400.
     *
     * <p>Không phải chuyện gõ nhầm ba chữ cái: một báo giá thị trường {@code VN}
     * ghi bằng {@code DKK} là một lần quy đổi tỷ giá đi vào hệ thống bằng cửa
     * sau, và dự án này <b>không có tỷ giá ở đâu cả</b> (CLAUDE.md điều 4).
     */
    public static class CurrencyMismatch extends RuntimeException {
        public CurrencyMismatch(String detail) {
            super(detail);
        }
    }

    /** Báo giá đã quá {@code valid_until}. HTTP 409 — quy tắc 4, không tự gia hạn. */
    public static class QuoteExpired extends RuntimeException {
        public QuoteExpired(String detail) {
            super(detail);
        }
    }

    /**
     * Báo giá không ở trạng thái nhận được thao tác này. HTTP 409.
     *
     * <p>Gộp ba tình huống, vì với người dùng cả ba đều là "báo giá này không
     * còn ở bước đó nữa": bước chuyển ngoài máy trạng thái, sửa bảng giá sau khi
     * đã gửi, và gửi một báo giá chưa có dòng nào.
     */
    public static class NotAcceptable extends RuntimeException {

        private final transient Map<String, Object> params;

        public NotAcceptable(QuoteStatus tu, QuoteStatus sang) {
            super("không đi từ " + tu + " sang " + sang + " được");
            this.params = Map.of("from", tu.name(), "to", sang.name());
        }

        public NotAcceptable(QuoteStatus tu, String lyDo) {
            super(lyDo);
            this.params = Map.of("from", tu.name());
        }

        public Map<String, Object> params() {
            return params;
        }
    }
}
