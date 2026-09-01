package vn.travel.booking.application.post;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.market.Markets;
import vn.travel.booking.application.shared.NotFoundException;
import vn.travel.booking.application.shared.PagedResult;

import java.util.List;

/**
 * Bài viết <b>không</b> có cổng chặn thị trường ở v1 (docs/12 mục 4.11), nhưng
 * vẫn kiểm thị trường có bật hay không: đường dẫn công khai nào cũng mang market,
 * và thị trường chưa mở thì cả site không tồn tại, không riêng gì phần bán hàng.
 */
@Service
public class PostUseCases {

    private final PostQueryPort posts;
    private final Markets markets;

    public PostUseCases(PostQueryPort posts, Markets markets) {
        this.posts = posts;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public PagedResult<PostSummary> list(String market, String locale,
                                         List<String> tagSlugs, int page, int size) {
        markets.requireActive(market);
        return posts.findPosts(locale, tagSlugs == null ? List.of() : tagSlugs, page, size);
    }

    @Transactional(readOnly = true)
    public PostDetail get(String market, String locale, String slug) {
        markets.requireActive(market);
        return posts.findPost(locale, slug)
                .orElseThrow(() -> new NotFoundException("post slug=" + slug + " locale=" + locale));
    }
}
