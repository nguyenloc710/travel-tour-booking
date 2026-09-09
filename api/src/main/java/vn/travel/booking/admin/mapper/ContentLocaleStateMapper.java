package vn.travel.booking.admin.mapper;

import org.mapstruct.Mapper;
import vn.travel.booking.admin.dto.ContentLocaleState;
import vn.travel.booking.web.generated.model.AdminContentLocaleState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bảng "locale nào đang ở trạng thái nào" của một mẩu nội dung — thứ M13 hiện
 * lên thành mấy ô màu.
 *
 * <p>Tách riêng vì cả bài viết lẫn buổi thuyết trình đều dùng, và một quy tắc
 * dùng ở hai nơi thì phải có đúng một bản. Hai mapper kia kéo nó vào bằng
 * {@code uses = ContentLocaleStateMapper.class}.
 */
@Mapper(componentModel = "spring")
public interface ContentLocaleStateMapper {

    /**
     * Giữ nguyên thứ tự bằng {@link LinkedHashMap}: thứ tự locale là thứ tự cột
     * trên màn hình, và một bảng đổi thứ tự cột giữa hai lần tải là bảng khó
     * đọc.
     */
    default Map<String, AdminContentLocaleState> toLocaleStates(
            Map<String, ContentLocaleState> source) {

        if (source == null) {
            return null;
        }
        Map<String, AdminContentLocaleState> result = new LinkedHashMap<>();
        source.forEach((locale, state) ->
                result.put(locale, AdminContentLocaleState.fromValue(state.name())));
        return result;
    }
}
