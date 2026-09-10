package vn.travel.booking.media.mapper;

import org.mapstruct.Mapper;
import vn.travel.booking.media.dto.MediaView;
import vn.travel.booking.web.generated.model.AdminMedia;
import vn.travel.booking.web.generated.model.AdminMediaList;

/**
 * View của tầng nghiệp vụ sang model của hợp đồng.
 *
 * <p>Không map tay trong service — api/CLAUDE.md mục 0b. Mọi trường tính ra
 * ({@code url}, {@code translatedLocales}) đã được tính ở service, nên chỗ này
 * chỉ còn là đổi kiểu.
 */
@Mapper(componentModel = "spring")
public interface MediaWebMapper {

    AdminMedia toWeb(MediaView v);

    java.util.List<AdminMedia> toWebList(java.util.List<MediaView> v);

    default AdminMediaList toList(java.util.List<MediaView> v) {
        return new AdminMediaList(toWebList(v));
    }
}
