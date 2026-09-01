package vn.travel.booking.application.shared;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Nhớ kết quả của một lời gọi để lần gọi lại trả về đúng kết quả đó — docs/13 mục 7.
 */
public interface IdempotencyPort {

    Optional<StoredResponse> find(UUID key);

    void save(UUID key, String market, String endpoint, String requestHash,
              int statusCode, String responseJson, Duration ttl);

    /**
     * @param requestHash vân tay thân yêu cầu. Cùng khoá nhưng thân khác là lỗi
     *                    phía client dùng lại khoá cho việc khác, không phải một
     *                    lần gọi lại — trả kết quả cũ khi đó là im lặng nuốt mất
     *                    một đơn hàng thật.
     */
    record StoredResponse(String requestHash, int statusCode, String responseJson) {
    }
}
