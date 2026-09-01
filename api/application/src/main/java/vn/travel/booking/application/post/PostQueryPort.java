package vn.travel.booking.application.post;

import vn.travel.booking.application.shared.PagedResult;

import java.util.List;
import java.util.Optional;

public interface PostQueryPort {

    /** {@code tagSlugs} rỗng thì không lọc; nhiều giá trị nghĩa là HOẶC. */
    PagedResult<PostSummary> findPosts(String locale, List<String> tagSlugs, int page, int size);

    Optional<PostDetail> findPost(String locale, String slug);
}
