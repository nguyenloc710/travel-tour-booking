package vn.travel.booking.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import vn.travel.booking.common.exception.IdempotencyConflictException;
import vn.travel.booking.common.repository.IdempotencyRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Gọi lại cùng khoá trả về cùng kết quả cũ — docs/13 mục 7.
 *
 * <p>Chống ba thứ có thật: khách bấm nút hai lần, mạng di động gửi lại yêu cầu
 * sau khi máy chủ đã xử lý xong nhưng phản hồi chưa về, và khách bấm Back rồi
 * bấm lại nút trả tiền.
 */
@Component
public class Idempotency {

    /** 24 giờ — docs/13 mục 7. */
    private static final Duration RETENTION_PERIOD = Duration.ofHours(24);

    /**
     * Bộ tuần tự hoá <b>riêng</b> của lớp này, không dùng chung với tầng web.
     *
     * <p>Hai việc ở đây khác hẳn việc trả JSON cho khách: băm vân tay yêu cầu, và
     * cất rồi lấy lại kết quả. Dùng chung một bộ cấu hình nghĩa là đổi cách trả
     * JSON cho khách sẽ làm mọi vân tay cũ hết khớp, và mọi khoá đang sống mất
     * tác dụng — một lần triển khai thành một lần cho phép đặt trùng.
     */
    private static final ObjectMapper JSON = JsonMapper.builder()
            // Đăng ký thẳng thay vì findAndAddModules(): nạp theo ServiceLoader
            // im lặng không tìm thấy gì thì lỗi chỉ lộ ra lúc chạy, ở đúng dòng
            // có kiểu ngày giờ đầu tiên.
            .addModule(new JavaTimeModule())
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    private final IdempotencyRepository repository;

    Idempotency(IdempotencyRepository repository) {
        this.repository = repository;
    }

    /**
     * Chạy {@code action} một lần duy nhất cho mỗi khoá.
     *
     * <p>Ba đường:
     *
     * <ul>
     *   <li>Chưa có khoá → chạy, lưu kết quả, trả kết quả
     *   <li>Có khoá, <b>cùng</b> vân tay yêu cầu → trả lại kết quả cũ, không chạy lại
     *   <li>Có khoá, <b>khác</b> vân tay → 409. Client đang dùng lại khoá cho việc
     *       khác, và trả kết quả cũ khi đó là im lặng nuốt mất một đơn hàng thật
     * </ul>
     *
     * <p>Chỉ nhớ kết quả <b>thành công</b>: một lần thử thất bại vì hết chỗ không
     * được khoá vĩnh viễn câu trả lời đó — chỗ có thể được trả về kho ngay sau đó.
     */
    public <T> ResponseEntity<T> run(UUID key, String market, String endpoint, Object body,
                               Class<T> responseType, Supplier<ResponseEntity<T>> action) {
        String fingerprint = fingerprint(body);

        var existing = repository.find(key);
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(fingerprint)) {
    throw new IdempotencyConflictException(
                        "khoá " + key + " đã dùng cho một yêu cầu khác");
            }
            return ResponseEntity.status(existing.get().statusCode())
                    .body(mapRow(existing.get().responseJson(), responseType));
        }

        ResponseEntity<T> result = action.get();

        if (result.getStatusCode().is2xxSuccessful()) {
            repository.save(key, market, endpoint, fingerprint,
                    result.getStatusCode().value(), toJson(result.getBody()), RETENTION_PERIOD);
        }
        return result;
    }

    /** SHA-256 của thân yêu cầu đã chuẩn hoá qua Jackson. */
    private String fingerprint(Object body) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    hash.digest(toJson(body).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
    throw new IllegalStateException("Máy chạy Java mà không có SHA-256", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception ex) {
    throw new IllegalStateException("Không tuần tự hoá được thân yêu cầu", ex);
        }
    }

    private <T> T mapRow(String json, Class<T> targetType) {
        try {
            return JSON.readValue(json, targetType);
        } catch (Exception ex) {
    throw new IllegalStateException("Không đọc lại được kết quả đã lưu", ex);
        }
    }
}
