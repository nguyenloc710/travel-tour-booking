package vn.travel.booking.admin.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.PostDetailView;
import vn.travel.booking.admin.dto.PostRow;
import vn.travel.booking.admin.dto.PostTranslationInput;
import vn.travel.booking.admin.dto.PostTranslationView;
import vn.travel.booking.admin.dto.TagView;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.web.generated.model.AdminPostDetail;
import vn.travel.booking.web.generated.model.AdminPostPage;
import vn.travel.booking.web.generated.model.AdminPostSummary;
import vn.travel.booking.web.generated.model.AdminPostTranslation;
import vn.travel.booking.web.generated.model.AdminPostTranslationInput;
import vn.travel.booking.web.generated.model.AdminTag;
import vn.travel.booking.web.generated.model.TranslationStatus;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ContentLocaleStateMapper.class})
public interface AdminPostMapper {

    AdminTag toTag(TagView t);

    List<AdminTag> toTagList(List<TagView> tags);

    @Mapping(target = "locales", source = "locales")
    AdminPostSummary toSummary(PostRow p);

    List<AdminPostSummary> toSummaryList(List<PostRow> posts);

    AdminPostDetail toDetail(PostDetailView p);

    AdminPostTranslation toTranslation(PostTranslationView t);

    List<AdminPostTranslation> toTranslationList(List<PostTranslationView> translations);

    PostTranslationInput toInput(AdminPostTranslationInput input);

    default AdminPostPage toPage(PagedResult<PostRow> result) {
        if (result == null) {
            return null;
        }
        return new AdminPostPage(
                toSummaryList(result.items()),
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages());
    }

    default TranslationStatus toTranslationStatus(String status) {
        return status == null ? null : TranslationStatus.fromValue(status);
    }
}
