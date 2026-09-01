package vn.travel.booking.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.admin.repository.AdminProductTranslationRepository;
import vn.travel.booking.admin.dto.ProductTranslationInput;
import vn.travel.booking.admin.dto.ProductTranslationView;
import vn.travel.booking.product.entity.ProductTranslationEntity;
import vn.travel.booking.product.mapper.ProductTranslationMapper;
import vn.travel.booking.product.repository.ProductTranslationRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Đường <b>ghi</b> của trang quản trị: JPA, đúng như docs/10 mục 6 đã chốt.
 *
 * <p>Ngôn ngữ nguồn không hardcode: nó đọc từ bảng {@code locale}, cột
 * {@code is_source} — cùng nguồn sự thật mà ADR-004 và ràng buộc
 * {@code ux_locale_single_source} cưỡng chế.
 */
@Repository
public class AdminProductTranslationRepository {

    private final ProductTranslationRepository repository;
    private final ProductTranslationMapper mapper;
    private final JdbcTemplate jdbc;

    public AdminProductTranslationRepository(ProductTranslationRepository repository,
                                             ProductTranslationMapper mapper,
                                             JdbcTemplate jdbc) {
        this.repository = repository;
        this.mapper = mapper;
        this.jdbc = jdbc;
    }
    public boolean productExists(UUID productId) {
        Integer so = jdbc.queryForObject(
                "SELECT count(*) FROM product WHERE id = ? AND NOT soft_delete",
                Integer.class, productId);
        return so != null && so > 0;
    }

    /**
     * Ngôn ngữ nguồn xếp <b>trước</b>: màn hình dịch song song đặt bản nguồn bên
     * trái, ô nhập bên phải (docs/22 mục 4.2), nên thứ tự này là hợp đồng chứ
     * không phải tình cờ.
     */
    public List<ProductTranslationView> findAll(UUID productId) {
        String nguon = localeNguon();
        Map<String, OffsetPair> moc = mocThoiGian(productId);

        return repository.findByProductIdAndSoftDeleteFalse(productId).stream()
                .sorted(Comparator.comparing((ProductTranslationEntity e) ->
                        e.getLocale().equals(nguon) ? 0 : 1).thenComparing(ProductTranslationEntity::getLocale))
                .map(e -> dienThemTinhRa(e, nguon, moc))
                .toList();
    }
    public ProductTranslationView save(UUID productId, String locale, ProductTranslationInput input) {
        ProductTranslationEntity entity = repository
                .findByProductIdAndLocaleAndSoftDeleteFalse(productId, locale)
                .orElseGet(() -> new ProductTranslationEntity(productId, locale));

        mapper.ghiVao(input, entity);

        ProductTranslationEntity daLuu = repository.saveAndFlush(entity);

        // Mốc thời gian đọc LẠI TỪ CSDL sau khi lưu: cột last_modified_at do
        // TRIGGER đặt chứ không do Java, nên giá trị còn trong bộ nhớ là giá trị
        // trước lần lưu này. Đây là cái giá của việc để CSDL sở hữu cột đó — và
        // vẫn rẻ hơn nhiều so với hai chỗ cùng ghi một cột.
        return dienThemTinhRa(daLuu, localeNguon(), mocThoiGian(productId));
    }

    // ------------------------------------------------------------ tính ra

    private ProductTranslationView dienThemTinhRa(ProductTranslationEntity e,
                                                  String localeNguon,
                                                  Map<String, OffsetPair> moc) {
        ProductTranslationView co_ban = mapper.sangView(e);
        boolean laNguon = e.getLocale().equals(localeNguon);
        OffsetPair cuaChinhNo = moc.get(e.getLocale());

        return new ProductTranslationView(
                co_ban.locale(), co_ban.slug(), co_ban.title(), co_ban.shortDescription(),
                co_ban.longDescription(), co_ban.whyChooseThis(), co_ban.heroImageAlt(),
                co_ban.status(), laNguon,
                laNguon ? null : quaHan(e, moc.get(localeNguon)),
                cuaChinhNo != null ? cuaChinhNo.lastModifiedAt() : co_ban.lastModifiedAt(),
                co_ban.lastModifiedBy());
    }

    /**
     * {@code OUTDATED} = bản nguồn sửa <b>sau</b> lần dịch gần nhất. Chưa dịch
     * lần nào thì cũng là quá hạn — nó chưa bao giờ khớp bản nguồn.
     */
    private static Boolean quaHan(ProductTranslationEntity ban, OffsetPair nguon) {
        if (nguon == null) {
            return Boolean.FALSE;
        }
        if (ban.getTranslatedAt() == null) {
            return Boolean.TRUE;
        }
        return nguon.lastModifiedAt().isAfter(ban.getTranslatedAt());
    }

    private String localeNguon() {
        return jdbc.queryForObject(
                "SELECT code FROM locale WHERE is_source AND is_active", String.class);
    }

    private Map<String, OffsetPair> mocThoiGian(UUID productId) {
        return jdbc.query("""
                SELECT locale, last_modified_at
                FROM product_translation
                WHERE product_id = ? AND NOT soft_delete
                """,
                (rs, i) -> new OffsetPair(
                        rs.getString("locale"),
                        rs.getObject("last_modified_at", java.time.OffsetDateTime.class)),
                productId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(OffsetPair::locale, Function.identity()));
    }

    private record OffsetPair(String locale, java.time.OffsetDateTime lastModifiedAt) {
    }
}
