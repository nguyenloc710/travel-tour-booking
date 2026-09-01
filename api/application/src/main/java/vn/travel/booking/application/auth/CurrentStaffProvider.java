package vn.travel.booking.application.auth;

import java.util.Optional;
import java.util.UUID;

/**
 * Ai đang thao tác. Tầng {@code web} biết câu trả lời (nó giữ ngữ cảnh bảo mật);
 * tầng {@code infrastructure} cần câu trả lời (để điền {@code created_by} và
 * {@code last_modified_by}).
 *
 * <p>Cổng này tồn tại để hai tầng đó không phải nhìn thấy nhau — chiều phụ thuộc
 * ở docs/10 mục 3 không cho phép, và ArchUnit sẽ báo đỏ nếu ai đó thử.
 */
public interface CurrentStaffProvider {

    /** Rỗng với job nền và migration — không có người nào đứng sau chúng. */
    Optional<UUID> currentStaffId();
}
