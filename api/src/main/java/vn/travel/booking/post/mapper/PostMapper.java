package vn.travel.booking.post.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.product.dto.NamedRef;
import vn.travel.booking.web.generated.model.PostDetail;
import vn.travel.booking.web.generated.model.PostPage;
import vn.travel.booking.web.generated.model.PostSummary;
import vn.travel.booking.web.generated.model.Ref;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "tags", source = "tags")
    PostSummary toSummary(vn.travel.booking.post.dto.PostSummary b);

    @Mapping(target = "tags", source = "tags")
    PostDetail toDetail(vn.travel.booking.post.dto.PostDetail b);

    Ref toRef(NamedRef ref);

    List<PostSummary> toSummaryList(List<vn.travel.booking.post.dto.PostSummary> items);

    default PostPage toPage(PagedResult<vn.travel.booking.post.dto.PostSummary> pagedResult) {
        if (pagedResult == null) {
            return null;
        }
        return new PostPage(
                toSummaryList(pagedResult.items()),
                pagedResult.page(),
                pagedResult.size(),
                pagedResult.totalItems(),
                pagedResult.totalPages());
    }
}
