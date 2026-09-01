package vn.travel.booking.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vn.travel.booking.application.shared.NotFoundException;
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
class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

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

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> khongTimThay(NotFoundException ex) {
        log.debug("404 NOT_FOUND: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(loi("NOT_FOUND"));
    }

    @ExceptionHandler(UnsupportedLocaleException.class)
    ResponseEntity<ErrorResponse> locale(UnsupportedLocaleException ex) {
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
            ConstraintViolationException.class
    })
    ResponseEntity<ErrorResponse> dauVaoSai(Exception ex) {
        log.debug("400 VALIDATION_FAILED: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(loi("VALIDATION_FAILED"));
    }

    /** Đường dẫn không tồn tại — vẫn phải là JSON, không phải trang lỗi HTML. */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> khongCoDuongDan(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(loi("NOT_FOUND"));
    }

    /**
     * {@code traceId} chỉ có ở 500 và đây là chỗ duy nhất sinh ra nó: khách đọc
     * mã đó qua điện thoại cho tổng đài, tổng đài tra log. Không kèm thông điệp
     * ngoại lệ vào phản hồi — nó lộ cấu trúc bên trong.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> loiKhongLuong(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("500 INTERNAL_ERROR traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(loi("INTERNAL_ERROR").traceId(traceId));
    }
}
