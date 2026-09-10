package vn.travel.booking.media.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.itinerary.repository.ItineraryRefRepository;
import vn.travel.booking.media.dto.MediaView;
import vn.travel.booking.media.entity.DestinationMediaEntity;
import vn.travel.booking.media.entity.MediaAssetEntity;
import vn.travel.booking.media.entity.ProductImageEntity;
import vn.travel.booking.media.repository.DestinationMediaRepository;
import vn.travel.booking.media.repository.ProductImageRepository;
import vn.travel.booking.product.repository.ProductWriteRepository;
import vn.travel.booking.web.generated.model.FieldRule;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gắn tệp vào thực thể — bộ ảnh sản phẩm và danh sách media của điểm đến.
 *
 * <p><b>Thứ tự là thuộc tính của cả danh sách</b>, nên đường ghi là "thay toàn
 * bộ" chứ không "thêm/xoá từng cái". Một endpoint thêm cộng một endpoint xoá thì
 * không diễn đạt được "đổi chỗ hai tấm" — mà đó là việc biên tập viên làm nhiều
 * nhất.
 *
 * <p>Vị trí trong mảng <b>chính là</b> {@code sort_order}. Không có trường thứ tự
 * riêng trong thân yêu cầu: hai nguồn sự thật cho cùng một việc thì sẽ có ngày
 * chúng nói khác nhau.
 */
@Service
public class MediaLinkService {

    private final DestinationMediaRepository destinationMedia;
    private final ProductImageRepository productImages;
    private final MediaService media;
    private final ItineraryRefRepository refs;
    private final ProductWriteRepository products;

    public MediaLinkService(DestinationMediaRepository destinationMedia,
                            ProductImageRepository productImages,
                            MediaService media,
                            ItineraryRefRepository refs,
                            ProductWriteRepository products) {
        this.destinationMedia = destinationMedia;
        this.productImages = productImages;
        this.media = media;
        this.refs = refs;
        this.products = products;
    }

    // ------------------------------------------------------------ điểm đến

    @Transactional(readOnly = true)
    public List<MediaView> destinationMedia(UUID destinationId) {
        requireDestination(destinationId);
        return media.viewsOf(destinationMedia.findByDestinationIdOrderBySortOrder(destinationId).stream()
                .map(DestinationMediaEntity::getAssetId)
                .toList());
    }

    /** Nhận cả ảnh lẫn video — điểm đến có bộ media trộn hai loại (V9). */
    @Transactional
    public List<MediaView> saveDestinationMedia(UUID destinationId, List<UUID> assetIds) {
        requireDestination(destinationId);
        List<MediaAssetEntity> tep = requireAssets(assetIds, false);

        destinationMedia.deleteByDestinationId(destinationId);
        destinationMedia.flush();
        for (short i = 0; i < tep.size(); i++) {
            destinationMedia.save(new DestinationMediaEntity(destinationId, tep.get(i).getId(), (short) (i + 1)));
        }
        destinationMedia.flush();

        return destinationMedia(destinationId);
    }

    // ------------------------------------------------------------ sản phẩm

    @Transactional(readOnly = true)
    public List<MediaView> productImages(UUID productId) {
        requireProduct(productId);
        return media.viewsOf(productImages.findByProductIdOrderBySortOrder(productId).stream()
                .map(ProductImageEntity::getAssetId)
                .toList());
    }

    /**
     * Bộ ảnh sản phẩm — <b>chỉ nhận ảnh</b>.
     *
     * <p>Bảng nối là {@code product_image} và V9 cố tình không đổi tên nó: bộ ảnh
     * sản phẩm hôm nay chỉ có ảnh. Chặn video ở đây để cái tên còn đúng.
     */
    @Transactional
    public List<MediaView> saveProductImages(UUID productId, List<UUID> assetIds) {
        requireProduct(productId);
        List<MediaAssetEntity> tep = requireAssets(assetIds, true);

        productImages.deleteByProductId(productId);
        productImages.flush();
        for (short i = 0; i < tep.size(); i++) {
            productImages.save(new ProductImageEntity(productId, tep.get(i).getId(), (short) (i + 1)));
        }
        productImages.flush();

        return productImages(productId);
    }

    // ------------------------------------------------------------ nội bộ

    /**
     * Mọi {@code assetId} phải tồn tại, không trùng nhau, và đúng loại cho phép.
     *
     * <p>Gom cả ba luật rồi ném <b>một lần</b>, kèm chỉ số phần tử sai — biểu mẫu
     * sắp thứ tự bằng kéo thả, và "phần tử thứ ba sai" là thứ nó tô được.
     */
    private List<MediaAssetEntity> requireAssets(List<UUID> assetIds, boolean chiAnh) {
        List<AdminErrors.FieldRulesViolated.Issue> issues = new ArrayList<>();
        Set<UUID> daGap = new LinkedHashSet<>();
        List<MediaAssetEntity> ra = new ArrayList<>();

        for (int i = 0; i < assetIds.size(); i++) {
            UUID id = assetIds.get(i);
            String o = "assetIds[%d]".formatted(i);

            if (!daGap.add(id)) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        o, FieldRule.INVALID, Map.of("reason", "trungLap")));
                continue;
            }
            MediaAssetEntity e = media.findAsset(id).orElse(null);
            if (e == null) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        o, FieldRule.INVALID, Map.of("reason", "khongTonTai")));
                continue;
            }
            if (chiAnh && !"IMAGE".equals(e.getKind())) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        o, FieldRule.INVALID, Map.of("reason", "chiNhanAnh", "kind", e.getKind())));
                continue;
            }
            ra.add(e);
        }

        AdminErrors.FieldRulesViolated.throwIfAny(issues);
        return ra;
    }

    private void requireDestination(UUID id) {
        if (!refs.destinationExists(id)) {
            throw new NotFoundException("destination id=" + id);
        }
    }

    private void requireProduct(UUID id) {
        if (products.findByIdAndSoftDeleteFalse(id).isEmpty()) {
            throw new NotFoundException("product id=" + id);
        }
    }
}
