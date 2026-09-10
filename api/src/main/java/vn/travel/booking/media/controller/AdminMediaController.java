package vn.travel.booking.media.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.common.util.AcceptLanguages;
import vn.travel.booking.common.util.SecurityUtils;
import vn.travel.booking.media.dto.MediaView;
import vn.travel.booking.media.mapper.MediaWebMapper;
import vn.travel.booking.media.service.MediaLinkService;
import vn.travel.booking.media.service.MediaService;
import vn.travel.booking.web.generated.model.AdminMedia;
import vn.travel.booking.web.generated.model.AdminMediaCreate;
import vn.travel.booking.web.generated.model.AdminMediaList;
import vn.travel.booking.web.generated.model.MediaOrder;
import vn.travel.booking.web.generated.model.AdminMediaTranslationInput;
import vn.travel.booking.web.generated.model.MediaKind;
import vn.travel.booking.web.generated.model.MediaUploadUrl;
import vn.travel.booking.web.generated.model.MediaUploadUrlRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static vn.travel.booking.common.util.AdminResponses.noCache;

/**
 * Kho ảnh, bề mặt quản trị — ADR-011 mục 4, ADR-013.
 *
 * <p>Ba endpoint, và không cái nào nhận byte của tệp: backend chỉ ký URL, kiểm
 * lại kho, rồi ghi bản ghi. Đẩy tệp qua đây nghĩa là một tour 40 ảnh đi qua bộ
 * nhớ của API hai lần.
 *
 * <p>{@code @PreAuthorize} chỉ trả lời "ai được vào cửa". Luật "vào rồi thì sửa
 * được bản dịch nào" phụ thuộc locale và nằm ở {@link MediaService}.
 */
@RestController
public class AdminMediaController {

    private final MediaService media;
    private final MediaLinkService links;
    private final MediaWebMapper mapper;

    public AdminMediaController(MediaService media, MediaLinkService links, MediaWebMapper mapper) {
        this.media = media;
        this.links = links;
        this.mapper = mapper;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/media/upload-url",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<MediaUploadUrl> createMediaUploadUrl(
            @Valid @RequestBody MediaUploadUrlRequest input
    ) {
        String folder = input.getFolder() == null
                ? MediaUploadUrlRequest.FolderEnum.TOUR.getValue()
                : input.getFolder().getValue();

        MediaService.UploadTicket ticket = media.newUploadUrl(
                folder, input.getContentType().getValue(), input.getByteSize());

        return noCache().body(new MediaUploadUrl(
                ticket.uploadUrl(),
                ticket.path(),
                OffsetDateTime.now().plus(ticket.ttl())));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/api/v1/admin/media",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminMedia> createMedia(
            @Valid @RequestBody AdminMediaCreate input
    ) {
        MediaView saved = media.create(new MediaService.NewMedia(
                input.getKind() == null ? MediaKind.IMAGE.getValue() : input.getKind().getValue(),
                input.getPath(),
                input.getWidth(),
                input.getHeight(),
                Math.toIntExact(input.getByteSize()),
                input.getSource().getValue(),
                input.getLicenceRef(),
                input.getLicenceScope(),
                input.getLicenceUntil(),
                Boolean.TRUE.equals(input.getPersonConsent()),
                input.getAlt(),
                input.getContentType(),
                input.getDurationSeconds(),
                input.getPosterAssetId()), AcceptLanguages.SOURCE);

        return noCache(HttpStatus.CREATED).body(mapper.toWeb(saved));
    }

    // ------------------------------------------- gắn tệp vào thực thể
    //
    // Hai bề mặt, một khuôn: `PUT` thay toàn bộ danh sách, và vị trí trong mảng
    // chính là `sort_order`. Khác nhau đúng một luật — bộ ảnh sản phẩm chỉ nhận
    // ảnh, còn điểm đến nhận cả video (V9).

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/destinations/{id}/media",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN','CONSULTANT')")
    public ResponseEntity<AdminMediaList> getDestinationMedia(@PathVariable("id") UUID id) {
        return noCache().body(mapper.toList(links.destinationMedia(id)));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/destinations/{id}/media",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminMediaList> saveDestinationMedia(
            @PathVariable("id") UUID id,
            @Valid @RequestBody MediaOrder input
    ) {
        return noCache().body(mapper.toList(links.saveDestinationMedia(id, input.getAssetIds())));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/api/v1/admin/products/{id}/images",
        produces = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN','CONSULTANT')")
    public ResponseEntity<AdminMediaList> getProductImages(@PathVariable("id") UUID id) {
        return noCache().body(mapper.toList(links.productImages(id)));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/products/{id}/images",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AdminMediaList> saveProductImages(
            @PathVariable("id") UUID id,
            @Valid @RequestBody MediaOrder input
    ) {
        return noCache().body(mapper.toList(links.saveProductImages(id, input.getAssetIds())));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/api/v1/admin/media/{id}/translations/{locale}",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    @PreAuthorize("hasAnyRole('EDITOR','TRANSLATOR','ADMIN')")
    public ResponseEntity<AdminMedia> saveMediaTranslation(
            @PathVariable("id") UUID id,
            @PathVariable("locale") String locale,
            @Valid @RequestBody AdminMediaTranslationInput input
    ) {
        MediaView saved = media.saveTranslation(
                id, locale, AcceptLanguages.SOURCE, SecurityUtils.roles(), input.getAlt());

        return noCache().body(mapper.toWeb(saved));
    }
}
