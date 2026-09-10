package vn.travel.booking.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
import vn.travel.booking.web.generated.model.ErrorCode;
import vn.travel.booking.web.generated.model.ErrorResponse;
import vn.travel.booking.web.generated.model.FieldError;
import vn.travel.booking.web.generated.model.FieldRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static ErrorResponse error(ErrorCode code) {
        return error(code, (Map<String, Object>) null);
    }

    /**
     * Lỗi <b>kèm tham số</b> — dùng cho đường ghi quản trị.
     *
     * <p>Tham số là dữ liệu, không phải câu tiếng người: {@code {"expectedBlock":
     * "groupTour"}} chứ không phải "Bạn thiếu khối tour đoàn". Frontend vẫn là
     * chỗ dịch (docs/13 mục 5).
     */
    private static ErrorResponse error(ErrorCode code, AdminErrors.WithParams ex) {
        return error(code, ex.params());
    }

    /**
     * Chỗ <b>duy nhất</b> dựng thân lỗi.
     *
     * <p>{@code fields(null)} cùng lý do với {@code params(null)}: lớp sinh ra
     * khởi tạo nó bằng một danh sách rỗng, nên để nguyên thì MỌI phản hồi lỗi đều
     * kèm {@code "fields":[]} — kể cả 404. Chỉ {@link #invalidInput} đặt lại nó.
     */
    private static ErrorResponse error(ErrorCode code, Map<String, Object> params) {
        return new ErrorResponse(code)
                .params(params == null || params.isEmpty() ? null : params)
                .fields(null);
    }

    // ---------------------------------------------------- ghi quản trị: 400
    //
    // Cả bốn là luật LIÊN TRƯỜNG hoặc luật cần tra CSDL — thứ schema của OpenAPI
    // không diễn đạt được, nên `@Valid` không bắt hộ. Luật một trường vẫn để
    // `@Valid` bắt và trả VALIDATION_FAILED như cũ.

    @ExceptionHandler(AdminErrors.ProductTypeBlockMismatch.class)
    public ResponseEntity<ErrorResponse> handleProductTypeBlockMismatch(AdminErrors.ProductTypeBlockMismatch ex) {
        log.debug("400 PRODUCT_TYPE_BLOCK_MISMATCH: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error(ErrorCode.PRODUCT_TYPE_BLOCK_MISMATCH, ex));
    }

    @ExceptionHandler(AdminErrors.DurationDaysRuleViolated.class)
    public ResponseEntity<ErrorResponse> handleDurationDaysRuleViolated(AdminErrors.DurationDaysRuleViolated ex) {
        log.debug("400 DURATION_DAYS_RULE_VIOLATED: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error(ErrorCode.DURATION_DAYS_RULE_VIOLATED, ex));
    }

    @ExceptionHandler(AdminErrors.CabinCategoryNotAllowed.class)
    public ResponseEntity<ErrorResponse> cabinWrongProductType(AdminErrors.CabinCategoryNotAllowed ex) {
        log.debug("400 CABIN_CATEGORY_NOT_ALLOWED: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error(ErrorCode.CABIN_CATEGORY_NOT_ALLOWED, ex));
    }

    @ExceptionHandler(AdminErrors.UnknownPaxType.class)
    public ResponseEntity<ErrorResponse> handleUnknownPaxType(AdminErrors.UnknownPaxType ex) {
        log.debug("400 UNKNOWN_PAX_TYPE: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error(ErrorCode.UNKNOWN_PAX_TYPE, ex));
    }

    @ExceptionHandler(AdminErrors.PriceTierNotContiguous.class)
    public ResponseEntity<ErrorResponse> priceTierGap(AdminErrors.PriceTierNotContiguous ex) {
        log.debug("400 PRICE_TIER_NOT_CONTIGUOUS: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error(ErrorCode.PRICE_TIER_NOT_CONTIGUOUS, ex));
    }

    // ---------------------------------------------------- ghi quản trị: 409

    @ExceptionHandler(AdminErrors.CapacityBelowBooked.class)
    public ResponseEntity<ErrorResponse> handleCapacityBelowBooked(AdminErrors.CapacityBelowBooked ex) {
        log.debug("409 CAPACITY_BELOW_BOOKED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.CAPACITY_BELOW_BOOKED, ex));
    }

    @ExceptionHandler(AdminErrors.DestinationInUse.class)
    public ResponseEntity<ErrorResponse> destinationStillInUse(AdminErrors.DestinationInUse ex) {
        log.debug("409 DESTINATION_IN_USE: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.DESTINATION_IN_USE, ex));
    }

    /**
     * Tắt {@code ADMIN} cuối cùng. 409 chứ không 403: người gọi <b>có</b> quyền
     * làm việc này, chỉ là trạng thái hiện tại của hệ thống không cho phép — và
     * lúc khác, khi đã có ADMIN thứ hai, thì được.
     */
    @ExceptionHandler(AdminErrors.LastAdmin.class)
    public ResponseEntity<ErrorResponse> lastAdmin(AdminErrors.LastAdmin ex) {
        log.warn("409 LAST_ADMIN: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.LAST_ADMIN, ex));
    }

    /**
     * Ngày khởi hành thiếu giá phòng đơn — cả lúc bật bán lẫn lúc tính giá.
     *
     * <p>409 chứ không 400: dữ liệu gửi lên đúng dạng và người gọi có quyền —
     * chỉ là trạng thái hiện tại của sản phẩm chưa cho phép. Nhập nốt bảng giá
     * rồi bấm lại là xong, không phải sửa lời gọi.
     *
     * <p>{@code warn} chứ không {@code debug} ở cả hai đường: một lần mở bán bị
     * chặn nghĩa là có người đang đợi ở đầu bên kia màn hình, còn lần chặn ở
     * đường khách thì tệ hơn — nó nghĩa là có sản phẩm đang bán với dữ liệu
     * thiếu, và cửa trước đã không giữ được nó.
     */
    @ExceptionHandler(SinglePriceMissingException.class)
    public ResponseEntity<ErrorResponse> handleSinglePriceMissing(SinglePriceMissingException ex) {
        log.warn("409 SINGLE_PRICE_MISSING: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.SINGLE_PRICE_MISSING, ex.params()));
    }

    @ExceptionHandler(AdminErrors.ProductHasActiveBookings.class)
    public ResponseEntity<ErrorResponse> handleProductHasActiveBookings(AdminErrors.ProductHasActiveBookings ex) {
        log.warn("409 PRODUCT_HAS_ACTIVE_BOOKINGS: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.PRODUCT_HAS_ACTIVE_BOOKINGS, ex));
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
    public ResponseEntity<ErrorResponse> invalidTransition(IllegalBookingTransitionException ex) {
        log.debug("409 BOOKING_TRANSITION_NOT_ALLOWED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.BOOKING_TRANSITION_NOT_ALLOWED, ex.params()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException ex) {
        log.debug("404 NOT_FOUND: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ErrorCode.NOT_FOUND));
    }

    /**
     * Chưa đăng nhập. Trả 401 kèm mã, <b>không</b> chuyển hướng tới trang đăng
     * nhập — đây là API, không phải trang web có form.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> notLoggedIn(AuthenticationException ex) {
        log.debug("401 UNAUTHENTICATED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(ErrorCode.UNAUTHENTICATED));
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
    public ResponseEntity<ErrorResponse> forbidden(Exception ex) {
        log.debug("403 FORBIDDEN: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(UnsupportedLocaleException.class)
    public ResponseEntity<ErrorResponse> locale(UnsupportedLocaleException ex) {
        log.debug("400 UNSUPPORTED_LOCALE: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error(ErrorCode.UNSUPPORTED_LOCALE));
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
    public ResponseEntity<ErrorResponse> invalidInput(Exception ex) {
        log.debug("400 VALIDATION_FAILED: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error(ErrorCode.VALIDATION_FAILED).fields(fieldErrors(ex)));
    }

    /**
     * Luật <b>liên trường</b> của tầng nghiệp vụ, trả ra <b>cùng hình dạng</b>
     * với luật một trường của {@code @Valid}: cùng mã {@code VALIDATION_FAILED},
     * cùng {@code fields}, cùng đường dẫn tới ô nhập.
     *
     * <p>Biểu mẫu ở frontend vì thế không phải biết lỗi đến từ Bean Validation
     * hay từ một hàm kiểm trong service — nó chỉ đọc {@code fields} rồi tô đúng
     * dòng.
     */
    @ExceptionHandler(AdminErrors.FieldRulesViolated.class)
    public ResponseEntity<ErrorResponse> crossFieldRule(AdminErrors.FieldRulesViolated ex) {
        log.debug("400 VALIDATION_FAILED (liên trường): {}", ex.getMessage());
        List<FieldError> fields = ex.issues().stream()
                .map(issue -> new FieldError(issue.path(), issue.rule())
                        .params(issue.params().isEmpty() ? null : issue.params()))
                .toList();
        return ResponseEntity.badRequest().body(error(ErrorCode.VALIDATION_FAILED).fields(fields));
    }

    /**
     * Bóc danh sách trường sai cho {@code VALIDATION_FAILED} — docs/13 mục 5.
     *
     * <p>Chỉ HAI trong số các ngoại lệ trên định vị được tới từng trường:
     * {@code @Valid} trên thân yêu cầu, và ràng buộc trên tham số của controller.
     * Số còn lại — JSON hỏng, thiếu header, sai kiểu — không có trường nào để
     * chỉ; trả một danh sách rỗng ở đó là nói dối rằng đã biết chỗ sai.
     *
     * <p>Trả {@code null} chứ không phải danh sách rỗng: trường rỗng bỏ hẳn khỏi
     * JSON (docs/13 mục 4).
     */
    private static List<FieldError> fieldErrors(Exception ex) {
        List<FieldError> result = new ArrayList<>();

        if (ex instanceof MethodArgumentNotValidException e) {
            // getField() đã là đường dẫn có dấu chấm và chỉ số mảng —
            // `source.longDescription`, `lines[2].amount`. Tên trường ở đây trùng
            // tên trong JSON vì model do openapi.yaml sinh ra.
            for (org.springframework.validation.FieldError error : e.getBindingResult().getFieldErrors()) {
                result.add(new FieldError(error.getField(), ruleCode(error.getCode())).params(ruleParams(error)));
            }
        } else if (ex instanceof ConstraintViolationException e) {
            for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
                result.add(new FieldError(propertyPath(violation), ruleCode(constraintName(violation)))
                        .params(ruleParams(violation)));
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Annotation Bean Validation → luật trong danh mục đóng của hợp đồng.
     *
     * <p>Ánh xạ <b>tường minh</b>, không suy ra từ tên annotation nữa:
     * {@code FieldRule} là danh mục đóng, nên một ràng buộc lạ phải rơi về
     * {@code INVALID} chứ không được đẻ ra một giá trị mà frontend chưa từng
     * thấy — đúng thứ mà enum trong hợp đồng sinh ra để chặn.
     *
     * <p>Cái giá của việc tường minh: thêm một kiểu ràng buộc mới thì phải thêm
     * một dòng ở đây. Quên thì người dùng vẫn biết trường nào sai, chỉ là chưa
     * có câu riêng cho luật đó.
     *
     * <p>{@code DecimalMin} và {@code DecimalMax} gộp vào {@code MIN} và
     * {@code MAX}: với người nhập liệu chúng là cùng một chuyện.
     */
    private static FieldRule ruleCode(String annotationName) {
        if (annotationName == null) {
            return FieldRule.INVALID;
        }
        return switch (annotationName) {
            case "NotNull" -> FieldRule.NOT_NULL;
            case "Size" -> FieldRule.SIZE;
            case "Min", "DecimalMin" -> FieldRule.MIN;
            case "Max", "DecimalMax" -> FieldRule.MAX;
            case "Pattern" -> FieldRule.PATTERN;
            default -> FieldRule.INVALID;
        };
    }

    private static String constraintName(ConstraintViolation<?> violation) {
        return violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
    }

    /**
     * Bỏ đoạn đầu của đường dẫn ở ràng buộc mức tham số: Hibernate Validator ghi
     * {@code listAdminProducts.size}, mà tên phương thức Java thì frontend
     * không biết và cũng không nên biết.
     */
    private static String propertyPath(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int dot = path.indexOf('.');
        return dot > 0 ? path.substring(dot + 1) : path;
    }

    private static Map<String, Object> ruleParams(org.springframework.validation.ObjectError error) {
        try {
            return ruleParams(error.unwrap(ConstraintViolation.class));
        } catch (RuntimeException notFromBeanValidation) {
            // Lỗi ràng buộc không đến từ Bean Validation (ví dụ lỗi ép kiểu lúc
            // bind). Vẫn có `path` và `code` — chỉ là không có tham số.
            return null;
        }
    }

    /**
     * Tham số của chính luật đó, để frontend dựng câu mà không phải viết cứng con
     * số trong ràng buộc: {@code {"min":2}}, {@code {"regexp":"…"}}.
     */
    private static Map<String, Object> ruleParams(ConstraintViolation<?> violation) {
        String rule = constraintName(violation);
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> attribute : violation.getConstraintDescriptor().getAttributes().entrySet()) {
            String name = attribute.getKey();
            Object value = attribute.getValue();

            // Ba thuộc tính chung của mọi annotation, không phải tham số của luật.
            if (name.equals("message") || name.equals("groups") || name.equals("payload")) {
                continue;
            }
            // @Size không đặt thì min=0 và max=Integer.MAX_VALUE. Trả chúng ra là
            // đẩy `2147483647` tới tận màn hình người dùng.
            if (rule.equals("Size") && name.equals("min") && Integer.valueOf(0).equals(value)) {
                continue;
            }
            if (rule.equals("Size") && name.equals("max") && Integer.valueOf(Integer.MAX_VALUE).equals(value)) {
                continue;
            }
            // @Min và @Max gọi thuộc tính của mình là `value`; docs/13 mục 5 viết
            // `min` và `max`, và đó mới là thứ frontend đọc được.
            if (name.equals("value") && (rule.equals("Min") || rule.equals("DecimalMin"))) {
                result.put("min", value);
            } else if (name.equals("value") && (rule.equals("Max") || rule.equals("DecimalMax"))) {
                result.put("max", value);
            } else {
                result.put(name, value);
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Bốn tình huống nghiệp vụ của đường đặt tour — docs/13 mục 5.1.
     *
     * <p>Tất cả là <b>409</b>: yêu cầu hợp lệ, nhưng trạng thái hiện tại của hệ
     * thống không cho phép. Khác 422 ở chỗ đó — 422 là "yêu cầu này không bao giờ
     * hợp lệ", 409 là "lúc khác thì được".
     */
    @ExceptionHandler(BookingErrors.DepartureSoldOut.class)
    public ResponseEntity<ErrorResponse> handleDepartureSoldOut(BookingErrors.DepartureSoldOut ex) {
        log.debug("409 DEPARTURE_SOLD_OUT: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.DEPARTURE_SOLD_OUT));
    }

    @ExceptionHandler(BookingErrors.SeatHoldExpired.class)
    public ResponseEntity<ErrorResponse> holdExpired(BookingErrors.SeatHoldExpired ex) {
        log.debug("409 SEAT_HOLD_EXPIRED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.SEAT_HOLD_EXPIRED));
    }

    @ExceptionHandler(BookingErrors.DepartureClosed.class)
    public ResponseEntity<ErrorResponse> handleDepartureClosed(BookingErrors.DepartureClosed ex) {
        log.debug("409 DEPARTURE_CLOSED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.DEPARTURE_CLOSED));
    }

    // ---------------------------------------------------- luồng báo giá
    //
    // docs/14 mục 7. Cùng cách chia HTTP với đường đặt tour: 409 là "lúc khác
    // thì được", 422 là "yêu cầu này không bao giờ hợp lệ".

    @ExceptionHandler(QuoteErrors.QuoteExpired.class)
    public ResponseEntity<ErrorResponse> quoteExpired(QuoteErrors.QuoteExpired ex) {
        log.debug("409 QUOTE_EXPIRED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.QUOTE_EXPIRED));
    }

    /**
     * Gộp ba tình huống, vì với người dùng cả ba là "báo giá này không còn ở
     * bước đó nữa": bước chuyển ngoài máy trạng thái, sửa bảng giá sau khi đã
     * gửi, và gửi một báo giá chưa có dòng nào. Tham số {@code from} đi kèm để
     * frontend dựng được câu cụ thể.
     */
    @ExceptionHandler(QuoteErrors.NotAcceptable.class)
    public ResponseEntity<ErrorResponse> quoteWrongStatus(QuoteErrors.NotAcceptable ex) {
        log.debug("409 QUOTE_NOT_ACCEPTABLE: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.QUOTE_NOT_ACCEPTABLE, ex.params()));
    }

    @ExceptionHandler(QuoteErrors.LeadTimeNotMet.class)
    public ResponseEntity<ErrorResponse> notEnoughNotice(QuoteErrors.LeadTimeNotMet ex) {
        log.debug("422 LEAD_TIME_NOT_MET: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity()
                .body(error(ErrorCode.LEAD_TIME_NOT_MET, ex.params()));
    }

    @ExceptionHandler(QuoteErrors.ProductNotQuotable.class)
    public ResponseEntity<ErrorResponse> cannotQuote(QuoteErrors.ProductNotQuotable ex) {
        log.debug("422 PRODUCT_NOT_QUOTABLE: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error(ErrorCode.PRODUCT_NOT_QUOTABLE));
    }

    /**
     * Cùng khoá gọi lại nhưng thân yêu cầu khác — client dùng lại khoá cho việc
     * khác. Trả kết quả cũ trong tình huống đó là im lặng nuốt mất một đơn thật.
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> duplicateKey(IdempotencyConflictException ex) {
        log.warn("409 IDEMPOTENCY_KEY_REUSED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.IDEMPOTENCY_KEY_REUSED));
    }

    /**
     * Hai tình huống 422: yêu cầu đúng cú pháp nhưng nghiệp vụ không cho phép,
     * và lúc khác cũng vẫn không cho phép.
     */
    @ExceptionHandler(BookingErrors.ProductNotBookable.class)
    public ResponseEntity<ErrorResponse> cannotBook(BookingErrors.ProductNotBookable ex) {
        log.debug("422 PRODUCT_NOT_BOOKABLE: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error(ErrorCode.PRODUCT_NOT_BOOKABLE));
    }

    @ExceptionHandler(PartySizeOutOfRangeException.class)
    public ResponseEntity<ErrorResponse> paxOutsideNorth(PartySizeOutOfRangeException ex) {
        log.debug("422 PARTY_SIZE_OUT_OF_RANGE: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error(ErrorCode.PARTY_SIZE_OUT_OF_RANGE));
    }

    /** Đường dẫn không tồn tại — vẫn phải là JSON, không phải trang lỗi HTML. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noPath(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ErrorCode.NOT_FOUND));
    }

    /**
     * Ràng buộc CSDL nổ mà không hàm kiểm nào đón — lưới <b>cuối</b>, và cố ý
     * vẫn là {@code 500}.
     *
     * <p>Ba luật liên trường với tới được từ API đã có chỗ kiểm ở tầng nghiệp vụ
     * ({@code AdminProductService.validateBlockFieldRules}), nên rơi tới đây
     * nghĩa là một ràng buộc mà API chưa biết vừa bị vi phạm. Đó là lỗi của
     * chúng ta chứ không phải của người nhập liệu, và {@code 500} nói đúng điều
     * đó — đổi nó thành {@code 400} là đổ lỗi cho người dùng về một thứ họ không
     * sửa được.
     *
     * <p>Cái nó thêm so với bộ bắt cuối là <b>tên ràng buộc trong log</b>. Không
     * có dòng này thì việc đầu tiên phải làm là lần một stack trace dài để tìm
     * chữ {@code ck_…} nằm ở dòng {@code Caused by} cuối cùng.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolated(DataIntegrityViolationException ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("500 INTERNAL_ERROR traceId={} ràng buộc={} — chưa có hàm kiểm nào đón nó",
                traceId, constraintName(ex), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(ErrorCode.INTERNAL_ERROR).traceId(traceId));
    }

    /** Tên ràng buộc Postgres, moi từ ngoại lệ Hibernate bọc bên trong. */
    private static String constraintName(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof org.hibernate.exception.ConstraintViolationException hibernate
                && hibernate.getConstraintName() != null) {
            return hibernate.getConstraintName();
        }
        return "không xác định";
    }

    /**
     * {@code traceId} chỉ có ở 500 và đây là chỗ duy nhất sinh ra nó: khách đọc
     * mã đó qua điện thoại cho tổng đài, tổng đài tra log. Không kèm thông điệp
     * ngoại lệ vào phản hồi — nó lộ cấu trúc bên trong.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUncaughtException(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("500 INTERNAL_ERROR traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(ErrorCode.INTERNAL_ERROR).traceId(traceId));
    }
}
