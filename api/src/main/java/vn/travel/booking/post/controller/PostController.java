package vn.travel.booking.post.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.travel.booking.common.util.RequestScope;
import vn.travel.booking.post.mapper.PostMapper;
import vn.travel.booking.post.service.PostService;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.web.generated.model.PostDetail;
import vn.travel.booking.web.generated.model.PostPage;
import vn.travel.booking.web.generated.model.PostSummary;

import java.time.Duration;
import java.util.List;

@RestController
@Validated
public class PostController {

    private final PostService posts;
    private final PostMapper postMapper;

    public PostController(PostService posts, PostMapper postMapper) {
        this.posts = posts;
        this.postMapper = postMapper;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/posts",
        produces = { "application/json" }
    )
    public ResponseEntity<PostPage> listPosts(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @Valid @RequestParam(value = "tag", required = false) @Nullable List<String> tag,
            @Min(0)  @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(60)  @Valid @RequestParam(value = "size", required = false, defaultValue = "12") Integer size
    ) {

        String locale = RequestScope.locale(acceptLanguage);

        PagedResult<vn.travel.booking.post.dto.PostSummary> pagedResult = posts.list(
                RequestScope.market(market), locale, tag,
                page == null ? 0 : page,
                size == null ? 12 : size);

        return response(locale).body(postMapper.toPage(pagedResult));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/{market}/posts/{slug}",
        produces = { "application/json" }
    )
    public ResponseEntity<PostDetail> getPost(
            @PathVariable("market") String market,
            @NotNull  @RequestHeader(value = "Accept-Language", required = true) String acceptLanguage,
            @PathVariable("slug") String slug
    ) {
        String locale = RequestScope.locale(acceptLanguage);

        vn.travel.booking.post.dto.PostDetail b =
                posts.get(RequestScope.market(market), locale, slug);

        return response(locale).body(postMapper.toDetail(b));
    }

    private static ResponseEntity.BodyBuilder response(String locale) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
                // Nội dung tĩnh: 5 phút — docs/13 mục 8.
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic());
    }
}
