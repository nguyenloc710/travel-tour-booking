package vn.travel.booking.media.service;

import io.minio.StatObjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.config.DiaChiKho;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.ForbiddenException;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.media.dto.MediaView;
import vn.travel.booking.media.entity.MediaAssetEntity;
import vn.travel.booking.media.entity.MediaAssetTranslationEntity;
import vn.travel.booking.media.repository.MediaAssetRepository;
import vn.travel.booking.media.repository.MediaAssetTranslationRepository;
import vn.travel.booking.web.generated.model.FieldRule;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Luồng tải ảnh hai bước — ADR-011 mục 4.
 *
 * <pre>
 *   1. POST /admin/media/upload-url   máy chủ ký, KHÔNG tạo gì
 *   2. trình duyệt PUT thẳng lên kho
 *   3. POST /admin/media              máy chủ hỏi lại kho rồi mới ghi bản ghi
 * </pre>
 *
 * <p>Bước 3 <b>không tin</b> những gì client khai. Chữ ký ở bước 1 chỉ ràng buộc
 * phương thức, đường dẫn và hạn dùng — không ràng buộc nội dung, và cũng không
 * ràng buộc rằng có ai tải lên thật. Một bản ghi trỏ vào tệp không tồn tại sinh
 * ra ảnh vỡ trên trang khách mà không có lỗi nào ở đâu.
 */
@Service
public class MediaService {

    /** Kích thước khai sai lệch quá mức này thì coi như client khai bừa. */
    private static final long SAI_SO_BYTE_CHO_PHEP = 0;

    private final MediaStorage storage;
    private final MediaAssetRepository assets;
    private final MediaAssetTranslationRepository translations;
    private final DiaChiKho publicUrl;

    public MediaService(MediaStorage storage, MediaAssetRepository assets,
                        MediaAssetTranslationRepository translations, DiaChiKho publicUrl) {
        this.storage = storage;
        this.assets = assets;
        this.translations = translations;
        this.publicUrl = publicUrl;
    }

    /**
     * Bước 1 — ký, không tạo gì. Không ai tải lên thì không có gì phải dọn.
     *
     * <p>Trần byte kiểm <b>ở đây</b>, không ở bước ba: ký một URL là hứa nhận
     * từng ấy byte vào đĩa, và sau khi tệp đã nằm trên đĩa thì từ chối nó chỉ còn
     * là dọn rác. Trần khác nhau theo loại tệp — {@link MediaStorage#maxBytesFor}.
     */
    public UploadTicket newUploadUrl(String folder, String contentType, long byteSize) {
        long tran = MediaStorage.maxBytesFor(contentType);
        if (byteSize > tran) {
            throw new AdminErrors.FieldRulesViolated(List.of(
                    new AdminErrors.FieldRulesViolated.Issue(
                            "byteSize", FieldRule.MAX, Map.of("max", tran))));
        }
        String objectKey = storage.newObjectKey(folder, contentType);
        return new UploadTicket(storage.presignPut(objectKey), objectKey, storage.uploadUrlTtl());
    }

    public record UploadTicket(String uploadUrl, String path, Duration ttl) {
    }

    /**
     * Bước 3 — ghi bản ghi, sau khi đã hỏi kho.
     *
     * <p>Ba luật liên trường ở đây, cả ba đi ra bằng {@code VALIDATION_FAILED}
     * kèm {@code fields} để biểu mẫu tô đúng ô:
     *
     * <ul>
     *   <li>tệp phải có thật trong kho</li>
     *   <li>{@code byteSize} phải khớp kích thước kho báo — client khai sai là
     *       dấu hiệu nó đang mô tả một tệp khác</li>
     *   <li>{@code source} khác {@code SELF} thì {@code licenceRef} bắt buộc —
     *       ràng buộc {@code ck_media_licence}, docs/24 mục 8</li>
     * </ul>
     */
    @Transactional
    public MediaView create(NewMedia input, String sourceLocale) {
        List<AdminErrors.FieldRulesViolated.Issue> issues = new ArrayList<>();

        Optional<StatObjectResponse> trongKho = storage.stat(input.path());
        if (trongKho.isEmpty()) {
            issues.add(new AdminErrors.FieldRulesViolated.Issue(
                    "path", FieldRule.INVALID, Map.of("reason", "khongCoTrongKho")));
        } else {
            long thatSu = trongKho.get().size();
            if (Math.abs(thatSu - input.byteSize()) > SAI_SO_BYTE_CHO_PHEP) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "byteSize", FieldRule.INVALID, Map.of("actual", thatSu)));
            }
        }

        if (!"SELF".equals(input.source()) && (input.licenceRef() == null || input.licenceRef().isBlank())) {
            issues.add(new AdminErrors.FieldRulesViolated.Issue(
                    "licenceRef", FieldRule.NOT_NULL, Map.of("source", input.source())));
        }

        issues.addAll(kiemLuatVideo(input));

        if (assets.existsByPath(input.path())) {
            // Đường dẫn do máy chủ đặt bằng UUID nên chuyện này gần như không xảy
            // ra — trừ khi ai đó gọi bước 3 hai lần cho cùng một tệp.
            issues.add(new AdminErrors.FieldRulesViolated.Issue(
                    "path", FieldRule.INVALID, Map.of("reason", "daGhiRoi")));
        }

        AdminErrors.FieldRulesViolated.throwIfAny(issues);

        MediaAssetEntity e = new MediaAssetEntity(UUID.randomUUID(), input.kind(), input.path(),
                input.width(), input.height(), input.byteSize(), input.source());
        e.setContentType(blankToNull(input.contentType()));
        e.setDurationSeconds(input.durationSeconds());
        e.setPosterAssetId(input.posterAssetId());
        e.setLicenceRef(blankToNull(input.licenceRef()));
        e.setLicenceScope(blankToNull(input.licenceScope()));
        e.setLicenceUntil(input.licenceUntil());
        e.setPersonConsent(input.personConsent());
        assets.saveAndFlush(e);

        // Chữ thay ảnh nhập bằng ngôn ngữ NGUỒN. Bản dịch đặt sau, và tới lúc đó
        // ảnh mới hiện ở locale kia — ADR-004 cộng luật không fallback.
        translations.saveAndFlush(new MediaAssetTranslationEntity(e.getId(), sourceLocale, input.alt()));

        return view(e);
    }

    public record NewMedia(
            String kind, String path, int width, int height, int byteSize, String source,
            String licenceRef, String licenceScope, LocalDate licenceUntil,
            boolean personConsent, String alt,
            String contentType, Integer durationSeconds, UUID posterAssetId) {
    }

    /**
     * Đặt {@code alt} cho một ngôn ngữ.
     *
     * <p>Luật quyền phụ thuộc locale, chép đúng ma trận của docs/22 mục 2.1 —
     * và chép có chủ ý, cùng lý do đã ghi ở {@code AdminContentService}: hai bề
     * mặt, hai dòng trong ma trận, hai chỗ kiểm.
     */
    @Transactional
    public MediaView saveTranslation(UUID id, String locale, String sourceLocale,
                                     Set<String> roles, String alt) {
        boolean isSource = sourceLocale.equals(locale);
        boolean allowed = roles.contains("ADMIN")
                || (isSource ? roles.contains("EDITOR") : roles.contains("TRANSLATOR"));
        if (!allowed) {
            throw new ForbiddenException(isSource
                    ? "chỉ EDITOR hoặc ADMIN sửa được bản ngôn ngữ nguồn"
                    : "chỉ TRANSLATOR hoặc ADMIN sửa được bản dịch");
        }

        MediaAssetEntity e = assets.findById(id)
                .orElseThrow(() -> new NotFoundException("media asset id=" + id));

        translations.findByAssetIdAndLocale(id, locale)
                .ifPresentOrElse(
                        t -> t.setAlt(alt),
                        () -> translations.save(new MediaAssetTranslationEntity(id, locale, alt)));
        translations.flush();

        return view(e);
    }

    /**
     * Luật của video — {@code ck_media_video} của V9 cưỡng chế cùng một thứ ở
     * tầng CSDL, và tầng này bắt trước để lỗi chỉ đúng ô thay vì thành 500.
     *
     * <p>Luật "ảnh bìa phải là một ẢNH" thì {@code CHECK} không nói được: nó là
     * điều kiện trên <b>dòng khác</b>. Đó là lý do nó chỉ có ở đây.
     */
    private List<AdminErrors.FieldRulesViolated.Issue> kiemLuatVideo(NewMedia input) {
        List<AdminErrors.FieldRulesViolated.Issue> issues = new ArrayList<>();
        boolean laVideo = "VIDEO".equals(input.kind());

        if (laVideo) {
            if (input.contentType() == null || input.contentType().isBlank()) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "contentType", FieldRule.NOT_NULL, Map.of()));
            }
            if (input.durationSeconds() == null || input.durationSeconds() <= 0) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "durationSeconds", FieldRule.NOT_NULL, Map.of()));
            }
            if (input.posterAssetId() == null) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "posterAssetId", FieldRule.NOT_NULL, Map.of()));
            } else {
                MediaAssetEntity bia = assets.findById(input.posterAssetId()).orElse(null);
                if (bia == null || !"IMAGE".equals(bia.getKind())) {
                    issues.add(new AdminErrors.FieldRulesViolated.Issue(
                            "posterAssetId", FieldRule.INVALID,
                            Map.of("reason", bia == null ? "khongTonTai" : "khongPhaiAnh")));
                }
            }
        } else {
            // Chiều ngược lại cũng là luật: ảnh mang thời lượng hay ảnh bìa nghĩa
            // là ai đó đang gửi sai `kind`.
            if (input.durationSeconds() != null) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "durationSeconds", FieldRule.INVALID, Map.of("kind", input.kind())));
            }
            if (input.posterAssetId() != null) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "posterAssetId", FieldRule.INVALID, Map.of("kind", input.kind())));
            }
        }
        return issues;
    }

    /** Tra một tệp — dùng bởi {@link MediaLinkService} để kiểm loại trước khi gắn. */
    @Transactional(readOnly = true)
    public Optional<MediaAssetEntity> findAsset(UUID id) {
        return assets.findById(id);
    }

    /**
     * Nhiều tệp theo đúng thứ tự id truyền vào.
     *
     * <p>Giữ thứ tự của tham số chứ không của CSDL: người gọi là bảng nối, và thứ
     * tự nó quan tâm là {@code sort_order} nó vừa đọc ra.
     */
    @Transactional(readOnly = true)
    public List<MediaView> viewsOf(List<UUID> ids) {
        return ids.stream()
                .map(assets::findById)
                .flatMap(Optional::stream)
                .map(this::view)
                .toList();
    }

    private MediaView view(MediaAssetEntity e) {
        List<String> locales = translations.findByAssetIdOrderByLocale(e.getId()).stream()
                .map(MediaAssetTranslationEntity::getLocale)
                .toList();
        String posterUrl = e.getPosterAssetId() == null ? null
                : assets.findById(e.getPosterAssetId())
                        .map(bia -> publicUrl.urlOf(bia.getPath()))
                        .orElse(null);

        return new MediaView(e.getId(), e.getKind(), e.getPath(), publicUrl.urlOf(e.getPath()),
                e.getWidth(), e.getHeight(), e.getByteSize(), e.getSource(),
                e.getLicenceRef(), e.getContentType(), e.getDurationSeconds(), posterUrl,
                locales);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
