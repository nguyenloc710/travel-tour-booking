package vn.travel.booking.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.application.post.PostUseCases;
import vn.travel.booking.application.shared.PagedResult;
import vn.travel.booking.web.generated.api.PostsApi;
import vn.travel.booking.web.generated.model.PostDetail;
import vn.travel.booking.web.generated.model.PostPage;
import vn.travel.booking.web.generated.model.PostSummary;

import java.time.Duration;
import java.util.List;

@RestController
public class PostController implements PostsApi {

    private final PostUseCases posts;

    public PostController(PostUseCases posts) {
        this.posts = posts;
    }

    @Override
    public ResponseEntity<PostPage> listPosts(
            String market, String acceptLanguage, List<String> tag, Integer page, Integer size) {

        String locale = RequestScope.locale(acceptLanguage);

        PagedResult<vn.travel.booking.application.post.PostSummary> trang = posts.list(
                RequestScope.market(market), locale, tag,
                page == null ? 0 : page,
                size == null ? 12 : size);

        PostPage than = new PostPage(
                trang.items().stream().map(PostController::sangTomTat).toList(),
                trang.page(), trang.size(), trang.totalItems(), trang.totalPages());

        return phanHoi(locale).body(than);
    }

    @Override
    public ResponseEntity<PostDetail> getPost(String market, String acceptLanguage, String slug) {
        String locale = RequestScope.locale(acceptLanguage);

        vn.travel.booking.application.post.PostDetail b =
                posts.get(RequestScope.market(market), locale, slug);

        PostDetail than = new PostDetail(b.slug(), b.title(), b.excerpt(), b.body(),
                b.tags().stream().map(ProductMapper::sangRef).toList())
                .heroImage(b.heroImage())
                .publishedAt(b.publishedAt());

        return phanHoi(locale).body(than);
    }

    private static PostSummary sangTomTat(vn.travel.booking.application.post.PostSummary b) {
        return new PostSummary(b.slug(), b.title(), b.excerpt(),
                b.tags().stream().map(ProductMapper::sangRef).toList())
                .heroImage(b.heroImage())
                .publishedAt(b.publishedAt());
    }

    private static ResponseEntity.BodyBuilder phanHoi(String locale) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                // Nội dung tĩnh: 5 phút — docs/13 mục 8.
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic());
    }
}
