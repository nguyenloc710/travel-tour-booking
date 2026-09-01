package vn.travel.booking.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import vn.travel.booking.application.shared.IdempotencyConflictException;
import vn.travel.booking.application.shared.IdempotencyPort;

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
class Idempotency {

    /** 24 giờ — docs/13 mục 7. */
    private static final Duration HAN = Duration.ofHours(24);

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

    private final IdempotencyPort kho;

    Idempotency(IdempotencyPort kho) {
        this.kho = kho;
    }

    /**
     * Chạy {@code viec} một lần duy nhất cho mỗi khoá.
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
    <T> ResponseEntity<T> chay(UUID khoa, String market, String endpoint, Object than,
                               Class<T> kieu, Supplier<ResponseEntity<T>> viec) {
        String vanTay = vanTay(than);

        var daCo = kho.find(khoa);
        if (daCo.isPresent()) {
            if (!daCo.get().requestHash().equals(vanTay)) {
                throw new IdempotencyConflictException(
                        "khoá " + khoa + " đã dùng cho một yêu cầu khác");
            }
            return ResponseEntity.status(daCo.get().statusCode())
                    .body(doc(daCo.get().responseJson(), kieu));
        }

        ResponseEntity<T> ketQua = viec.get();

        if (ketQua.getStatusCode().is2xxSuccessful()) {
            kho.save(khoa, market, endpoint, vanTay,
                    ketQua.getStatusCode().value(), viet(ketQua.getBody()), HAN);
        }
        return ketQua;
    }

    /** SHA-256 của thân yêu cầu đã chuẩn hoá qua Jackson. */
    private String vanTay(Object than) {
        try {
            MessageDigest bam = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    bam.digest(viet(than).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Máy chạy Java mà không có SHA-256", ex);
        }
    }

    private String viet(Object gia_tri) {
        try {
            return JSON.writeValueAsString(gia_tri);
        } catch (Exception ex) {
            throw new IllegalStateException("Không tuần tự hoá được thân yêu cầu", ex);
        }
    }

    private <T> T doc(String chuoi, Class<T> kieu) {
        try {
            return JSON.readValue(chuoi, kieu);
        } catch (Exception ex) {
            throw new IllegalStateException("Không đọc lại được kết quả đã lưu", ex);
        }
    }
}
