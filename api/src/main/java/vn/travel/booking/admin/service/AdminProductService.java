package vn.travel.booking.admin.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.MarketState;
import vn.travel.booking.admin.dto.ProductCreateInput;
import vn.travel.booking.admin.dto.ProductDetailView;
import vn.travel.booking.admin.dto.ProductPatchInput;
import vn.travel.booking.admin.dto.ProductTranslationView;
import vn.travel.booking.admin.dto.TranslationState;
import vn.travel.booking.admin.repository.AdminProductTranslationRepository;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.common.exception.SinglePriceMissingException;
import vn.travel.booking.common.repository.LocaleRepository;
import vn.travel.booking.market.repository.MarketRepository;
import vn.travel.booking.product.dto.ProductTypeBlocks;
import vn.travel.booking.product.entity.ProductEntity;
import vn.travel.booking.product.entity.ProductMarketEntity;
import vn.travel.booking.product.repository.ProductMarketRepository;
import vn.travel.booking.product.repository.ProductTypeBlockStore;
import vn.travel.booking.product.repository.ProductWriteRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tạo, sửa, xoá mềm sản phẩm; gán sản phẩm vào thị trường (docs/22 M3 và M5).
 *
 * <p>Đây là chỗ quyết định tiêu chí ra số 7 của cổng G4: nhân viên mở bán được
 * một tour mới mà không cần lập trình viên.
 */
@Service
public class AdminProductService {

    private final ProductWriteRepository product;
    private final ProductTypeBlockStore blockStore;
    private final ProductMarketRepository productMarketRepository;
    private final AdminProductTranslationRepository translationService;
    private final LocaleRepository locale;
    private final MarketRepository market;
    private final JdbcTemplate jdbc;

    public AdminProductService(ProductWriteRepository product,
                               ProductTypeBlockStore blockStore,
                               ProductMarketRepository productMarketRepository,
                               AdminProductTranslationRepository translationService,
                               LocaleRepository locale,
                               MarketRepository market,
                               JdbcTemplate jdbc) {
        this.product = product;
        this.blockStore = blockStore;
        this.productMarketRepository = productMarketRepository;
        this.translationService = translationService;
        this.locale = locale;
        this.market = market;
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------ tạo

    /**
     * Tạo sản phẩm: dòng {@code product}, dòng bảng con của loại, và bản dịch
     * <b>ngôn ngữ nguồn</b> — <b>một transaction</b>.
     *
     * <p>Không tách ba lời gọi được, và đó không phải lựa chọn thiết kế: ràng
     * buộc {@code ct_product_source_translation} là {@code DEFERRABLE INITIALLY
     * DEFERRED}, nên một {@code product} không có bản dịch nguồn không tồn tại
     * được qua hết transaction. Cột của bảng con thì {@code NOT NULL} và không
     * có mặc định.
     */
    @Transactional
    public ProductDetailView tao(ProductCreateInput input) {
        String type = input.productType();
        validateTypeBlocks(type, input.blocks());
        validateDayCount(type, input.durationDays());

        UUID id = UUID.randomUUID();
        ProductEntity e = new ProductEntity(id, type);
        writeCommonFields(e, input.primaryDestinationId(), input.durationDays(), input.heroImage(),
                input.mapImage(), input.layout(), input.isNew(), input.consultantId());
        product.save(e);

        blockStore.save(id, type, input.blocks());
        translationService.save(id, locale.localeNguon(), input.source());

        // flush() ngay để mọi ràng buộc TỨC THÌ — NOT NULL, CHECK, khoá ngoại
        // kép (product_id, product_type) — nổ ngay tại đây thay vì lẫn vào một
        // lời gọi sau. Ràng buộc HOÃN ct_product_source_translation thì vẫn chỉ
        // kiểm lúc commit; nó không kiểm được sớm hơn, và đó chính là lý do ba
        // lần ghi trên phải nằm trong một transaction.
        product.flush();
        return detail(id);
    }

    // ------------------------------------------------------------ đọc

    @Transactional(readOnly = true)
    public ProductDetailView detail(UUID id) {
        ProductEntity e = product.findByIdAndSoftDeleteFalse(id)
                .orElseThrow(() -> new NotFoundException("product id=" + id));

        List<MarketState> marketStates = productMarketRepository.findByProductIdOrderByMarketAsc(id).stream()
                .map(m -> new MarketState(m.getMarket(), m.isPublished()))
                .toList();

        List<TranslationState> translations = translationService.findAll(id).stream()
                .map(AdminProductService::toState)
                .toList();

        return new ProductDetailView(
                e.getId(), e.getProductType(), e.getPrimaryDestinationId(), e.getDurationDays(),
                e.getHeroImage(), e.getMapImage(), e.getLayout(), e.isNew(), e.getRating(),
                e.getReviewCount(),
                e.getConsultantId(), marketStates, translations,
                blockStore.mapRow(id, e.getProductType()),
                e.getLastModifiedAt(), e.getLastModifiedBy());
    }

    // ------------------------------------------------------------ sửa

    /**
     * Trường {@code null} nghĩa là <b>không đụng tới</b>. Không có cách đặt một
     * giá trị về rỗng qua endpoint này, và đó là chủ ý: xoá nhầm
     * {@code heroImage} bằng một trường vắng mặt là lỗi im lặng.
     */
    @Transactional
    public ProductDetailView sua(UUID id, ProductPatchInput input) {
        ProductEntity e = product.findByIdAndSoftDeleteFalse(id)
                .orElseThrow(() -> new NotFoundException("product id=" + id));

        if (!input.blocks().trong()) {
            validateTypeBlocks(e.getProductType(), input.blocks());
            blockStore.save(id, e.getProductType(), input.blocks());
        }
        if (input.durationDays() != null) {
            validateDayCount(e.getProductType(), input.durationDays());
        }
        writeCommonFields(e, input.primaryDestinationId(), input.durationDays(), input.heroImage(),
                input.mapImage(), input.layout(), input.isNew(), input.consultantId());

        product.saveAndFlush(e);
        return detail(id);
    }

    // ------------------------------------------------------------ xoá mềm

    /**
     * Xoá mềm sản phẩm <b>và cả chùm</b>.
     *
     * <p>Xoá mềm không lan xuống dưới như {@code ON DELETE CASCADE}: đặt
     * {@code product.soft_delete = TRUE} không đụng gì tới bản dịch, ngày khởi
     * hành hay bậc giá (api/CLAUDE.md mục 7b). Bỏ sót một bảng là để lại dữ liệu
     * mồ côi mà không truy vấn nào lọc ra — và nó sẽ lộ ra ở chỗ khó ngờ nhất,
     * ví dụ một khoá duy nhất bộ phận từ chối slug của tour vừa "xoá".
     *
     * <p>{@code product_market} <b>không</b> có trong danh sách: nhóm C không có
     * cột {@code soft_delete} (docs/11 mục 11.2). Nó bị xoá cứng theo sản phẩm
     * khi nào sản phẩm bị xoá cứng — và điều đó không xảy ra ở v1.
     */
    @Transactional
    public void softDelete(UUID id) {
        ProductEntity e = product.findByIdAndSoftDeleteFalse(id)
                .orElseThrow(() -> new NotFoundException("product id=" + id));

        // NOT IN của bốn trạng thái ĐÃ KẾT THÚC, chứ không phải IN của bốn trạng
        // thái đang chạy. Hai cách cho cùng kết quả hôm nay, nhưng chúng hỏng
        // theo hai hướng khác nhau: thêm một trạng thái mới vào máy trạng thái
        // (docs/23 mục 4) thì bản NOT IN mặc định coi nó là CHƯA xong và chặn
        // xoá, bản IN mặc định coi nó là xong và cho xoá. Chặn nhầm là phiền;
        // cho xoá nhầm là mất dữ liệu của khách đang chờ đi.
        Integer donChuaKetThuc = jdbc.queryForObject("""
                SELECT count(*) FROM booking b
                JOIN departure d ON d.id = b.departure_id
                WHERE d.product_id = ?
                  AND NOT b.soft_delete
                  AND b.status NOT IN ('COMPLETED', 'CANCELLED', 'REFUNDED', 'EXPIRED')
                """, Integer.class, id);

        if (donChuaKetThuc != null && donChuaKetThuc > 0) {
            throw new AdminErrors.ProductHasActiveBookings(donChuaKetThuc);
        }

        // Tắt bán trước, rồi mới xoá mềm: giữa hai câu lệnh vẫn còn một khoảnh
        // khắc, nhưng cả hai nằm trong một transaction nên không ai thấy nó.
        productMarketRepository.findByProductIdOrderByMarketAsc(id)
                .forEach(m -> m.setPublished(false));

        jdbc.update("UPDATE product_translation SET soft_delete = TRUE WHERE product_id = ?", id);
        jdbc.update("UPDATE departure SET soft_delete = TRUE WHERE product_id = ?", id);
        jdbc.update("UPDATE price_tier SET soft_delete = TRUE WHERE product_id = ?", id);

        e.setSoftDelete(true);
        product.saveAndFlush(e);
    }

    // ------------------------------------------------------------ thị trường

    /**
     * Gán sản phẩm vào một thị trường, và bật hoặc tắt bán.
     *
     * <p><b>Thao tác này chạm cả {@code product}, có chủ ý.</b>
     * {@code product_market} thuộc nhóm C nên không có cột kiểm toán, và docs/11
     * mục 11.2 nói rõ vì sao: "ai sửa sản phẩm này" phải có đúng một câu trả
     * lời, và câu đó là {@code product.last_modified_by}. Không chạm
     * {@code product} thì lần bật công tắc doanh thu là thay đổi duy nhất trong
     * hệ thống không ai đứng tên.
     *
     * <p>Gọi lại với cùng giá trị là bất biến: {@code published_at} chỉ đặt ở
     * <b>lần bật đầu tiên</b>, nên bật–tắt–bật không làm mất ngày mở bán gốc.
     */
    @Transactional
    public MarketState ganThiTruong(UUID id, String marketCode, boolean onSale) {
        ProductEntity sp = product.findByIdAndSoftDeleteFalse(id)
                .orElseThrow(() -> new NotFoundException("product id=" + id));

        // Thị trường đã tắt trả 404 chứ không phải một mã riêng: với bề mặt quản
        // trị thì "không có" và "đã tắt" dẫn tới cùng một việc phải làm.
        market.config(marketCode)
                .orElseThrow(() -> new NotFoundException("market=" + marketCode));

        if (onSale) {
            validateSingleRoomPrice(id, marketCode);
        }

        ProductMarketEntity pm = productMarketRepository.findByProductIdAndMarket(id, marketCode)
                .orElseGet(() -> new ProductMarketEntity(id, marketCode));

        pm.setPublished(onSale);
        if (onSale && pm.getPublishedAt() == null) {
            pm.setPublishedAt(OffsetDateTime.now());
        }
        productMarketRepository.saveAndFlush(pm);

        // Chạm product để lần đổi này có người đứng tên — xem javadoc trên.
        product.saveAndFlush(sp);

        return new MarketState(pm.getMarket(), pm.isPublished());
    }

    // ------------------------------------------------------------ kiểm

    /**
     * Mọi ngày khởi hành của sản phẩm có lưu trú qua đêm phải có giá phòng đơn.
     *
     * <p>Quy tắc kiểm 23 của docs/12 mục 9, cưỡng chế ngay ở công tắc mở bán.
     * Bộ kiểm ở {@code scripts/kiem-nhat-quan.sql} vẫn chạy trong CI, nhưng CI
     * chỉ nói cho lập trình viên biết — công tắc này nói cho đúng người đang mở
     * bán, đúng lúc họ mở, và đó là chỗ duy nhất sửa được ngay.
     *
     * <p><b>Chỉ kiểm khi BẬT.</b> Chặn cả đường tắt là giam một sản phẩm dữ liệu
     * sai ở trạng thái đang bán — đúng điều ngược lại với thứ quy tắc này muốn.
     *
     * <p>{@code DAY_TOUR} không kiểm: tour trong ngày không có đêm nào để ở
     * phòng, nên phụ thu phòng đơn không có nghĩa.
     */
    private void validateSingleRoomPrice(UUID id, String marketCode) {
        List<String> thieu = jdbc.queryForList("""
                SELECT d.depart_date::text
                FROM departure d
                JOIN product p ON p.id = d.product_id
                WHERE d.product_id = ? AND d.market = ? AND NOT d.soft_delete
                  AND p.product_type <> 'DAY_TOUR'
                  AND NOT EXISTS (
                        SELECT 1 FROM departure_price dp
                        WHERE dp.departure_id = d.id AND dp.occupancy = 'SINGLE')
                ORDER BY d.depart_date
                """, String.class, id, marketCode);

        if (!thieu.isEmpty()) {
            throw SinglePriceMissingException.forProduct(
                    marketCode, thieu.size(), thieu.getFirst());
        }
    }

    /**
     * Đúng <b>một</b> khối, và khối đó khớp {@code productType}.
     *
     * <p>Bắt ở đây chứ không để CSDL ném: thiếu khối thì lỗi CSDL là một vi phạm
     * {@code NOT NULL} ở bảng con, nổi lên thành {@code 500} kèm
     * {@code traceId}. Biên tập viên không đọc được gì từ đó, còn
     * {@code PRODUCT_TYPE_BLOCK_MISMATCH} kèm {@code expectedBlock} thì sửa được
     * ngay.
     */
    private static void validateTypeBlocks(String type, ProductTypeBlocks blocks) {
        int count = blocks.blockCount();
        if (count != 1 || blocks.blocksFor(type) == null) {
            throw new AdminErrors.ProductTypeBlockMismatch(type, ProductTypeBlocks.blockName(type), count);
        }
    }

    /**
     * {@code DAY_TOUR} không có số ngày; mọi loại khác bắt buộc có. Ràng buộc
     * {@code ck_product_duration} cưỡng chế <b>cả hai chiều</b>, nên cả hai
     * chiều phải kiểm ở đây.
     */
    private static void validateDayCount(String type, Short dayCount) {
        boolean laDayTour = "DAY_TOUR".equals(type);
        if (laDayTour == (dayCount != null)) {
            throw new AdminErrors.DurationDaysRuleViolated(type);
        }
    }

    private static void writeCommonFields(ProductEntity e, UUID destination, Short dayCount, String image,
                                     String anhBanDo, String khung, Boolean moi, UUID tuVanVien) {
        if (destination != null) {
            e.setPrimaryDestinationId(destination);
        }
        if (dayCount != null) {
            e.setDurationDays(dayCount);
        }
        if (image != null) {
            e.setHeroImage(image);
        }
        if (anhBanDo != null) {
            e.setMapImage(anhBanDo);
        }
        if (khung != null) {
            e.setLayout(khung);
        }
        if (moi != null) {
            e.setNew(moi);
        }
        if (tuVanVien != null) {
            e.setConsultantId(tuVanVien);
        }
    }

    private static TranslationState toState(ProductTranslationView v) {
        return new TranslationState(v.locale(), v.status(), v.isSource(),
                Boolean.TRUE.equals(v.outdated()));
    }
}
