package vn.travel.booking.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một việc dịch đang chờ (docs/22 M10).
 *
 * @param locale   locale <b>đích</b> — thứ đang thiếu, không phải thứ đang có
 * @param priority bậc ưu tiên của docs/22 mục 4.1.1, thấp là gấp
 */
public record QueueItem(
        String entityType,
        UUID id,
        String locale,
        String gap,
        int priority,
        String sourceTitle,
        OffsetDateTime sourceLastModifiedAt,
        OffsetDateTime translatedAt) {
}
