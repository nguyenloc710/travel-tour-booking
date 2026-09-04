package vn.travel.booking.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import vn.travel.booking.common.exception.ForbiddenException;
import vn.travel.booking.common.exception.BookingErrors;
import vn.travel.booking.common.exception.IdempotencyConflictException;
import vn.travel.booking.common.exception.PartySizeOutOfRangeException;
import vn.travel.booking.common.exception.QuoteErrors;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.web.generated.model.ErrorResponse;

import java.util.UUID;

/**
 * Biến ngoại lệ thành mã lỗi — <b>không bao giờ thành câu tiếng người</b>.
 *
 * <p>Trả {@code "Afgangen er udsolgt"} từ backend là sai: bản dịch sẽ tồn tại ở
 * hai nơi và lệch nhau. Mã lỗi còn dùng được cho log, thống kê và test; câu chữ
 * thì không (docs/13 mục 5).
 *
 * <p>Danh mục mã ở docs/13 mục 5.1. Thân ngoại lệ (câu tiếng Việt trong
 * {@code getMessage()}) chỉ đi vào log, không đi ra khách.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * <b>404 gộp ba tình huống có chủ ý</b>: không tồn tại, chưa dịch cho locale
     * này, chưa gán vào thị trường này. Phân biệt ra ngoài là rò rỉ thông tin về
     * sản phẩm chưa mở bán — log nội bộ vẫn phân biệt được.
     */
    /**
     * Lỗi không có tham số nào.
     *
     * {@code params(null)} là cố ý: lớp sinh ra khởi tạo {@code params} bằng một
     * map rỗng, nên nếu để nguyên thì mọi phản hồi lỗi đều kèm {@code "params":{}}
     * — trường rỗng phải bỏ hẳn khỏi JSON (docs/13 mục 4). Không đổi
     * {@code default-property-inclusion} thành {@code non_empty} để chữa: làm thế
     * thì một trang listing rỗng cũng mất luôn trường {@code items}, và frontend
     * vỡ đúng ở trạng thái rỗng — chỗ ít ai thử nhất.
     */
    private static ErrorResponse loi(String ma) {
        return new ErrorResponse(ma).params(null);
    }

    /**
     * Lỗi <b>kèm tham số</b> — dùng cho đường ghi quản trị.
     *
     * <p>Tham số là dữ liệu, không phải câu tiếng người: {@code {"expectedBlock":
     * "groupTour"}} chứ không phải "Bạn thiếu khối tour đoàn". Frontend vẫn là
     * chỗ dịch (docs/13 mục 5).
     */
    private static ErrorResponse loi(String ma, AdminErrors.CoThamSo ex) {
        return new ErrorResponse(ma).params(ex.params());
    }

    // ---------------------------------------------------- ghi quản trị: 400
    //
    // Cả bốn là luật LIÊN TRƯỜNG hoặc luật cần tra CSDL — thứ schema của OpenAPI
    // không diễn đạt được, nên `@Valid` không bắt hộ. Luật một trường vẫn để
    // `@Valid` bắt và trả VALIDATION_FAILED như cũ.

    @ExceptionHandler(AdminErrors.ProductTypeBlockMismatch.class)
    public ResponseEntity<ErrorResponse> saiKhoiLoai(AdminErrors.ProductTypeBlockMismatch ex) {
        log.debug("400 PRODUCT_TYPE_BLOCK_MISMATCH: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(loi("PRODUCT_TYPE_BLOCK_MISMATCH", ex));
    }

    @ExceptionHandler(AdminErrors.DurationDaysRuleViolated.class)
    public ResponseEntity<ErrorResponse> saiSoNgay(AdminErrors.DurationDaysRuleViolated ex) {
        log.debug("400 DURATION_DAYS_RULE_VIOLATED: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(loi("DURATION_DAYS_RULE_VIOLATED", ex));
    }

    @ExceptionHandler(AdminErrors.CabinCategoryNotAllowed.class)
    public ResponseEntity<ErrorResponse> cabinSaiLoai(AdminErrors.CabinCategoryNotAllowed ex) {
        log.debug("400 CABIN_CATEGORY_NOT_ALLOWED: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(loi("CABIN_CATEGORY_NOT_ALLOWED", ex));
    }

    @ExceptionHandler(AdminErrors.UnknownPaxType.class)
    public ResponseEntity<ErrorResponse> laLoaiKhach(AdminErrors.UnknownPaxType ex) {
        log.debug("400 UNKNOWN_PAX_TYPE: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(loi("UNKNOWN_PAX_TYPE", ex));
    }

    @ExceptionHandler(AdminErrors.PriceTierNotContiguous.class)
    public ResponseEntity<ErrorResponse> thangGiaHo(AdminErrors.PriceTierNotContiguous ex) {
        log.debug("400 PRICE_TIER_NOT_CONTIGUOUS: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(loi("PRICE_TIER_NOT_CONTIGUOUS", ex));
    }

    // ---------------------------------------------------- ghi quản trị: 409

    @ExceptionHandler(AdminErrors.CapacityBelowBooked.class)
    public ResponseEntity<ErrorResponse> sucChuaThapHonDaBan(AdminErrors.CapacityBelowBooked ex) {
        log.debug("409 CAPACITY_BELOW_BOOKED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(loi("CAPACITY_BELOW_BOOKED", ex));
    }

    @ExceptionHandler(AdminErrors.ProductHasActiveBookings.class)
    public ResponseEntity<ErrorResponse> conDonChuaXong(AdminErrors.ProductHasActiveBookings ex) {
        log.warn("409 PRODUCT_HAS_ACTIVE_BOOKINGS: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(loi("PRODUCT_HAS_ACTIVE_BOOKINGS", ex));
    }

    /**
     * Bước chuyển trạng thái đơn không nằm trong máy trạng thái (docs/23 mục 4).
     *
     * <p>409 chứ không 400: dữ liệu gửi lên <b>đúng dạng</b> — `CONFIRMED` là
     * một trạng thái có thật — chỉ là nó xung đột với trạng thái hiện tại của
     * đơn. Đây cũng là câu trả lời khi hai nhân viên cùng bấm một nút: người thứ
     * hai nhận 409 vì bước chuyển đã xảy ra rồi.
     */
    @ExceptionHandler(IllegalBookingTransitionException.class)
    public ResponseEntity<ErrorResponse> buocChuyenSai(IllegalBookingTransitionException ex) {
        log.debug("409 BOOKING_TRANSITION_NOT_ALLOWED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("BOOKING_TRANSITION_NOT_ALLOWED").params(ex.params()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> khongTimThay(NotFoundException ex) {
        log.debug("404 NOT_FOUND: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(loi("NOT_FOUND"));
    }

    /**
     * Chưa đăng nhập. Trả 401 kèm mã, <b>không</b> chuyển hướng tới trang đăng
     * nhập — đây là API, không phải trang web có form.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> chuaDangNhap(AuthenticationException ex) {
        log.debug("401 UNAUTHENTICATED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loi("UNAUTHENTICATED"));
    }

    /**
     * Đã đăng nhập nhưng không đủ quyền.
     *
     * <p>Hai nguồn: {@code @PreAuthorize} ở controller (sai vai trò) và
     * {@code ForbiddenException} ở tầng nghiệp vụ (đúng vai trò nhưng sai việc —
     * ví dụ người dịch sửa bản ngôn ngữ nguồn). Cùng một mã ra ngoài, vì với
     * người dùng thì cả hai đều là "bạn không được làm việc này".
     */
    @ExceptionHandler({AccessDeniedException.class, ForbiddenException.class})
    public ResponseEntity<ErrorResponse> khongDuQuyen(Exception ex) {
        log.debug("403 FORBIDDEN: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(loi("FORBIDDEN"));
    }

    @ExceptionHandler(UnsupportedLocaleException.class)
    public ResponseEntity<ErrorResponse> locale(UnsupportedLocaleException ex) {
        log.debug("400 UNSUPPORTED_LOCALE: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(loi("UNSUPPORTED_LOCALE"));
    }

    /**
     * Tham số sai kiểu (ví dụ {@code productType=KHONG_CO}) hoặc vi phạm ràng
     * buộc của spec ({@code size=999} khi spec nói tối đa 60).
     */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            // Ràng buộc @Min/@Max/@Size trên tham số của controller nổ ở tầng
            // AOP với ngoại lệ NÀY, không phải HandlerMethodValidationException.
            // Thiếu dòng này thì size=999 trả 500 kèm câu tiếng Đan Mạch trong log.
            ConstraintViolationException.class,
            // THIẾU hẳn một header hoặc tham số bắt buộc — ví dụ gọi
            // /dk/pricing/preview mà quên Accept-Language. Không có hai dòng này
            // thì nó rơi xuống bộ bắt cuối và trả 500 kèm traceId: client gọi sai
            // bị báo là lỗi máy chủ, và log đầy tiếng kêu cho một chuyện bình
            // thường. Đây là lỗi CỦA NGƯỜI GỌI, và 400 nói đúng điều đó.
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            // THÂN yêu cầu không đọc được: JSON hỏng, hoặc một giá trị enum
            // không có trong spec — ví dụ toStatus="EXPIRED" khi
            // AdminBookingTargetStatus chỉ nhận bốn giá trị. Jackson ném ngoại
            // lệ này TRƯỚC khi controller chạy, nên @Valid không bao giờ thấy
            // nó, và thiếu dòng này thì mọi thân yêu cầu sai đều trả 500.
            HttpMessageNotReadableException.class,
            // Bảng giá báo giá ghi bằng tiền tệ không phải của thị trường đó.
            // 400 chứ không 409: không có "lúc khác thì được" ở đây — thị
            // trường VN không bao giờ báo giá bằng DKK, vì hệ thống này không
            // có tỷ giá ở đâu cả (CLAUDE.md điều 4).
            QuoteErrors.CurrencyMismatch.class
    })
    public ResponseEntity<ErrorResponse> dauVaoSai(Exception ex) {
        log.debug("400 VALIDATION_FAILED: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(loi("VALIDATION_FAILED"));
    }

    /**
     * Bốn tình huống nghiệp vụ của đường đặt tour — docs/13 mục 5.1.
     *
     * <p>Tất cả là <b>409</b>: yêu cầu hợp lệ, nhưng trạng thái hiện tại của hệ
     * thống không cho phép. Khác 422 ở chỗ đó — 422 là "yêu cầu này không bao giờ
     * hợp lệ", 409 là "lúc khác thì được".
     */
    @ExceptionHandler(BookingErrors.DepartureSoldOut.class)
    public ResponseEntity<ErrorResponse> hetCho(BookingErrors.DepartureSoldOut ex) {
        log.debug("409 DEPARTURE_SOLD_OUT: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(loi("DEPARTURE_SOLD_OUT"));
    }

    @ExceptionHandler(BookingErrors.SeatHoldExpired.class)
    public ResponseEntity<ErrorResponse> giuChoHetHan(BookingErrors.SeatHoldExpired ex) {
        log.debug("409 SEAT_HOLD_EXPIRED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(loi("SEAT_HOLD_EXPIRED"));
    }

    @ExceptionHandler(BookingErrors.DepartureClosed.class)
    public ResponseEntity<ErrorResponse> daDongBan(BookingErrors.DepartureClosed ex) {
        log.debug("409 DEPARTURE_CLOSED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(loi("DEPARTURE_CLOSED"));
    }

    // ---------------------------------------------------- luồng báo giá
    //
    // docs/14 mục 7. Cùng cách chia HTTP với đường đặt tour: 409 là "lúc khác
    // thì được", 422 là "yêu cầu này không bao giờ hợp lệ".

    @ExceptionHandler(QuoteErrors.QuoteExpired.class)
    public ResponseEntity<ErrorResponse> baoGiaHetHan(QuoteErrors.QuoteExpired ex) {
        log.debug("409 QUOTE_EXPIRED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(loi("QUOTE_EXPIRED"));
    }

    /**
     * Gộp ba tình huống, vì với người dùng cả ba là "báo giá này không còn ở
     * bước đó nữa": bước chuyển ngoài máy trạng thái, sửa bảng giá sau khi đã
     * gửi, và gửi một báo giá chưa có dòng nào. Tham số {@code from} đi kèm để
     * frontend dựng được câu cụ thể.
     */
    @ExceptionHandler(QuoteErrors.NotAcceptable.class)
    public ResponseEntity<ErrorResponse> baoGiaSaiTrangThai(QuoteErrors.NotAcceptable ex) {
        log.debug("409 QUOTE_NOT_ACCEPTABLE: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("QUOTE_NOT_ACCEPTABLE").params(ex.params()));
    }

    @ExceptionHandler(QuoteErrors.LeadTimeNotMet.class)
    public ResponseEntity<ErrorResponse> chuaDuHanBaoTruoc(QuoteErrors.LeadTimeNotMet ex) {
        log.debug("422 LEAD_TIME_NOT_MET: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse("LEAD_TIME_NOT_MET").params(ex.params()));
    }

    @ExceptionHandler(QuoteErrors.ProductNotQuotable.class)
    public ResponseEntity<ErrorResponse> khongHoiGiaDuoc(QuoteErrors.ProductNotQuotable ex) {
        log.debug("422 PRODUCT_NOT_QUOTABLE: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(loi("PRODUCT_NOT_QUOTABLE"));
    }

    /**
     * Cùng khoá gọi lại nhưng thân yêu cầu khác — client dùng lại khoá cho việc
     * khác. Trả kết quả cũ trong tình huống đó là im lặng nuốt mất một đơn thật.
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> trungKhoa(IdempotencyConflictException ex) {
        log.warn("409 IDEMPOTENCY_KEY_REUSED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(loi("IDEMPOTENCY_KEY_REUSED"));
    }

    /**
     * Hai tình huống 422: yêu cầu đúng cú pháp nhưng nghiệp vụ không cho phép,
     * và lúc khác cũng vẫn không cho phép.
     */
    @ExceptionHandler(BookingErrors.ProductNotBookable.class)
    public ResponseEntity<ErrorResponse> khongDatDuoc(BookingErrors.ProductNotBookable ex) {
        log.debug("422 PRODUCT_NOT_BOOKABLE: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(loi("PRODUCT_NOT_BOOKABLE"));
    }

    @ExceptionHandler(PartySizeOutOfRangeException.class)
    public ResponseEntity<ErrorResponse> soKhachNgoaiBac(PartySizeOutOfRangeException ex) {
        log.debug("422 PARTY_SIZE_OUT_OF_RANGE: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(loi("PARTY_SIZE_OUT_OF_RANGE"));
    }

    /** Đường dẫn không tồn tại — vẫn phải là JSON, không phải trang lỗi HTML. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> khongCoDuongDan(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(loi("NOT_FOUND"));
    }

    /**
     * {@code traceId} chỉ có ở 500 và đây là chỗ duy nhất sinh ra nó: khách đọc
     * mã đó qua điện thoại cho tổng đài, tổng đài tra log. Không kèm thông điệp
     * ngoại lệ vào phản hồi — nó lộ cấu trúc bên trong.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> loiKhongLuong(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("500 INTERNAL_ERROR traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(loi("INTERNAL_ERROR").traceId(traceId));
    }
}
