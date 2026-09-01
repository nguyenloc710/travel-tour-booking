package vn.travel.booking.infrastructure.product;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.travel.booking.application.product.NamedRef;
import vn.travel.booking.application.product.ProductDetail;
import vn.travel.booking.application.product.ProductQuery;
import vn.travel.booking.application.product.ProductQueryPort;
import vn.travel.booking.application.product.ProductSort;
import vn.travel.booking.application.product.ProductSummary;
import vn.travel.booking.application.product.ProductType;
import vn.travel.booking.application.product.ProductVariant;
import vn.travel.booking.application.shared.PagedResult;
import vn.travel.booking.domain.shared.Money;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Truy vấn danh mục bằng SQL thuần, không JPA — docs/10 mục 6.
 *
 * <p><b>Ba điều kiện dưới đây là chính sách nghiệp vụ chứ không phải tối ưu:</b>
 *
 * <ul>
 *   <li>{@code JOIN product_translation … AND status = 'PUBLISHED'} — sản phẩm
 *       thiếu bản dịch <b>biến mất</b> khỏi locale đó. Đổi thành {@code LEFT JOIN}
 *       là phá chính sách không-fallback của docs/02 mục 4, và không có test nào
 *       đỏ ngay lập tức nếu dữ liệu thử toàn sản phẩm đã dịch đủ.
 *   <li>{@code JOIN product_market … AND pm.is_published} — cổng chặn thị trường.
 *       Sản phẩm chưa gán vào thị trường này thì không tồn tại ở thị trường này.
 *   <li>{@code AND NOT x.soft_delete} trên <b>mọi</b> bảng có cột đó. Xoá mềm
 *       hoạt động âm thầm: quên một chỗ là rò dữ liệu đã xoá ra khách mà không
 *       có lỗi nào nổ — đúng tính chất ADR-003 đã bác bỏ khi từ chối Hibernate
 *       {@code @Filter}.
 * </ul>
 *
 * <p>Bản dịch của <b>điểm đến và miền</b> cũng {@code INNER JOIN}: thẻ sản phẩm
 * hiển thị tên điểm đến, nên điểm đến chưa dịch thì không có cách nào hiện thẻ
 * đó ở locale này mà không chèn một mẩu tiếng Đan vào trang tiếng Việt.
 */
@Repository
public class JdbcProductQueryAdapter implements ProductQueryPort {

    /**
     * Phần {@code FROM … JOIN} dùng chung cho cả listing, đếm tổng và chi tiết.
     * Một chuỗi chứ không ba: ba bản sao của cùng bộ điều kiện là ba cơ hội để
     * một bản quên mất {@code NOT soft_delete}.
     */
    private static final String NGUON = """
            FROM product p
            JOIN product_market pm
              ON pm.product_id = p.id
             AND pm.market = ?
             AND pm.is_published
            JOIN market m
              ON m.code = pm.market
             AND m.is_active
            JOIN product_translation pt
              ON pt.product_id = p.id
             AND pt.locale = ?
             AND pt.status = 'PUBLISHED'
             AND NOT pt.soft_delete
            JOIN destination d
              ON d.id = p.primary_destination_id
             AND NOT d.soft_delete
            JOIN destination_translation dt
              ON dt.destination_id = d.id
             AND dt.locale = ?
             AND NOT dt.soft_delete
            JOIN region r
              ON r.id = d.region_id
             AND NOT r.soft_delete
            JOIN region_translation rt
              ON rt.region_id = r.id
             AND rt.locale = ?
             AND NOT rt.soft_delete
            WHERE NOT p.soft_delete
            """;

    private static final String CHON = """
            SELECT pt.slug,
                   pt.title,
                   p.product_type,
                   pt.short_description,
                   p.hero_image,
                   pt.hero_image_alt,
                   p.duration_days,
                   rt.slug AS region_slug,
                   rt.name AS region_name,
                   dt.slug AS destination_slug,
                   dt.name AS destination_name,
                   pm.price_from,
                   m.currency,
                   m.fraction_digits,
                   p.is_new,
                   p.rating,
                   p.review_count
            """;

    /** Tên collation chỉ gồm chữ, số và gạch nối — chặn mọi thứ khác trước khi nối chuỗi. */
    private static final Pattern TEN_COLLATION = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final JdbcTemplate jdbc;

    public JdbcProductQueryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PagedResult<ProductSummary> findProducts(ProductQuery query) {
        List<Object> thamSo = new ArrayList<>();
        String loc = dungMenhDeLoc(query, thamSo);

        Long tong = jdbc.queryForObject(
                "SELECT count(*) " + NGUON + loc, Long.class, thamSo.toArray());

        long totalItems = tong == null ? 0L : tong;
        if (totalItems == 0 || query.offset() >= totalItems) {
            return new PagedResult<>(List.of(), query.page(), query.size(), totalItems);
        }

        List<Object> thamSoTrang = new ArrayList<>(thamSo);
        thamSoTrang.add(query.size());
        thamSoTrang.add(query.offset());

        List<ProductSummary> items = jdbc.query(
                CHON + NGUON + loc + sapXep(query) + " LIMIT ? OFFSET ?",
                (rs, i) -> docTomTat(rs),
                thamSoTrang.toArray());

        return new PagedResult<>(items, query.page(), query.size(), totalItems);
    }

    @Override
    public Optional<ProductDetail> findProduct(String market, String locale, String slug) {
        String sql = CHON + """
                     , pt.long_description,
                       pt.why_choose_this,
                       p.map_image,
                       p.id AS product_id
                """ + NGUON + " AND pt.slug = ?";

        List<ProductDetail> ket_qua = jdbc.query(
                sql,
                (RowMapper<ProductDetail>) (rs, i) -> docChiTiet(rs),
                market, locale, locale, locale, slug);

        return ket_qua.stream().findFirst();
    }

    // ------------------------------------------------------------------ lọc

    private String dungMenhDeLoc(ProductQuery query, List<Object> thamSo) {
        // Bốn tham số của NGUON, đúng thứ tự dấu ? xuất hiện.
        thamSo.add(query.market());
        thamSo.add(query.locale());
        thamSo.add(query.locale());
        thamSo.add(query.locale());

        StringBuilder loc = new StringBuilder();

        if (query.regionSlug() != null && !query.regionSlug().isBlank()) {
            loc.append(" AND rt.slug = ?");
            thamSo.add(query.regionSlug());
        }
        if (query.productType() != null) {
            loc.append(" AND p.product_type = ?");
            thamSo.add(query.productType().name());
        }
        if (query.q() != null && !query.q().isBlank()) {
            // f_unaccent ở CẢ HAI VẾ: gõ "hoi an" phải ra "Hội An", mà gõ
            // "Hội An" cũng phải ra. ILIKE lo phần chữ hoa chữ thường.
            loc.append(" AND f_unaccent(pt.title) ILIKE '%' || f_unaccent(?) || '%'");
            thamSo.add(query.q().trim());
        }
        return loc.toString();
    }

    /**
     * Sắp xếp theo tiêu đề phải dùng <b>collation của locale đang xem</b>, và
     * collation không truyền được bằng tham số {@code ?} — nó là một phần của cú
     * pháp, không phải một giá trị. Nên lấy tên từ chính bảng {@code locale}
     * (CSDL vẫn là nguồn sự thật) rồi kiểm bằng biểu thức chính quy trước khi
     * nối chuỗi. Nối thẳng chuỗi do người dùng gửi vào đây là lỗ SQL injection.
     */
    private String sapXep(ProductQuery query) {
        String chieu = switch (query.sort()) {
            case TITLE_DESC, PRICE_FROM_DESC, DURATION_DAYS_DESC -> "DESC";
            default -> "ASC";
        };

        String cot = switch (query.sort()) {
            case TITLE_ASC, TITLE_DESC -> "pt.title COLLATE \"" + collationCua(query.locale()) + "\"";
            case PRICE_FROM_ASC, PRICE_FROM_DESC -> "pm.price_from";
            case DURATION_DAYS_ASC, DURATION_DAYS_DESC -> "p.duration_days";
        };

        // NULLS LAST: sản phẩm chưa có giá xuống cuối chứ không lên đầu.
        // Tiêu chí phụ theo slug để hai lần gọi cùng một trang ra cùng thứ tự —
        // thiếu nó thì phân trang offset lặp hoặc bỏ sót bản ghi.
        return " ORDER BY " + cot + " " + chieu + " NULLS LAST, pt.slug ASC";
    }

    private String collationCua(String locale) {
        String ten = jdbc.queryForObject(
                "SELECT collation_name FROM locale WHERE code = ?", String.class, locale);
        if (ten == null || !TEN_COLLATION.matcher(ten).matches()) {
            throw new IllegalStateException("Tên collation không hợp lệ cho locale " + locale + ": " + ten);
        }
        return ten;
    }

    // ------------------------------------------------------------------ đọc

    private ProductSummary docTomTat(ResultSet rs) throws SQLException {
        return new ProductSummary(
                rs.getString("slug"),
                rs.getString("title"),
                ProductType.valueOf(rs.getString("product_type")),
                rs.getString("short_description"),
                rs.getString("hero_image"),
                rs.getString("hero_image_alt"),
                soNguyenHoacNull(rs, "duration_days"),
                new NamedRef(rs.getString("region_slug"), rs.getString("region_name")),
                new NamedRef(rs.getString("destination_slug"), rs.getString("destination_name")),
                tien(rs),
                rs.getBoolean("is_new"),
                soThucHoacNull(rs, "rating"),
                rs.getInt("review_count"));
    }

    private ProductDetail docChiTiet(ResultSet rs) throws SQLException {
        ProductType loai = ProductType.valueOf(rs.getString("product_type"));
        return new ProductDetail(
                rs.getString("slug"),
                rs.getString("title"),
                loai,
                rs.getString("short_description"),
                mang(rs, "long_description"),
                mang(rs, "why_choose_this"),
                rs.getString("hero_image"),
                rs.getString("hero_image_alt"),
                rs.getString("map_image"),
                soNguyenHoacNull(rs, "duration_days"),
                new NamedRef(rs.getString("region_slug"), rs.getString("region_name")),
                new NamedRef(rs.getString("destination_slug"), rs.getString("destination_name")),
                tien(rs),
                rs.getBoolean("is_new"),
                soThucHoacNull(rs, "rating"),
                rs.getInt("review_count"),
                bienThe(loai, (java.util.UUID) rs.getObject("product_id")));
    }

    /**
     * Một truy vấn phụ cho bảng con, chọn bằng {@code switch} trên kiểu tổng.
     *
     * <p>Thêm loại sản phẩm thứ bảy mà quên chỗ này thì <b>không biên dịch
     * được</b> — {@code switch} trên enum có đủ nhánh là điều kiện bắt buộc khi
     * dùng dạng biểu thức.
     */
    private ProductVariant bienThe(ProductType loai, java.util.UUID productId) {
        return switch (loai) {
            case GROUP_TOUR -> jdbc.queryForObject("""
                    SELECT min_pax, max_pax, guaranteed_threshold, tour_leader_language, fitness_level
                    FROM product_group_tour WHERE product_id = ?
                    """, (rs, i) -> new ProductVariant.GroupTour(
                            rs.getInt("min_pax"),
                            rs.getInt("max_pax"),
                            rs.getInt("guaranteed_threshold"),
                            rs.getString("tour_leader_language"),
                            rs.getInt("fitness_level")),
                    productId);

            case INDIVIDUAL_PACKAGE -> jdbc.queryForObject("""
                    SELECT min_party_size, flexible_date_window_days
                    FROM product_individual WHERE product_id = ?
                    """, (rs, i) -> new ProductVariant.IndividualPackage(
                            rs.getInt("min_party_size"),
                            rs.getInt("flexible_date_window_days")),
                    productId);

            case PRIVATE_TOUR -> jdbc.queryForObject("""
                    SELECT lead_time_days, quote_valid_days
                    FROM product_private WHERE product_id = ?
                    """, (rs, i) -> new ProductVariant.PrivateTour(
                            rs.getInt("lead_time_days"),
                            rs.getInt("quote_valid_days")),
                    productId);

            case CRUISE -> jdbc.queryForObject("""
                    SELECT ship_name, port_count
                    FROM product_cruise WHERE product_id = ?
                    """, (rs, i) -> new ProductVariant.Cruise(
                            rs.getString("ship_name"),
                            rs.getInt("port_count")),
                    productId);

            case COMBO -> jdbc.queryForObject("""
                    SELECT nights, valid_from, valid_to
                    FROM product_combo WHERE product_id = ?
                    """, (rs, i) -> new ProductVariant.Combo(
                            rs.getInt("nights"),
                            rs.getObject("valid_from", java.time.LocalDate.class),
                            rs.getObject("valid_to", java.time.LocalDate.class)),
                    productId);

            case DAY_TOUR -> jdbc.queryForObject("""
                    SELECT duration_hours, cutoff_hours
                    FROM product_day_tour WHERE product_id = ?
                    """, (rs, i) -> new ProductVariant.DayTour(
                            rs.getInt("duration_hours"),
                            rs.getInt("cutoff_hours")),
                    productId);
        };
    }

    /**
     * Giá làm tròn theo số chữ số thập phân <b>của thị trường</b>: DKK 2, VND 0.
     * Không làm tròn thì API trả {@code "18900000.00"} cho tiền đồng — đúng về
     * mặt số học nhưng sai về mặt tiền tệ.
     */
    private Money tien(ResultSet rs) throws SQLException {
        BigDecimal soTien = rs.getBigDecimal("price_from");
        if (soTien == null) {
            return null;
        }
        return new Money(soTien, rs.getString("currency")).round(rs.getInt("fraction_digits"));
    }

    private static List<String> mang(ResultSet rs, String cot) throws SQLException {
        java.sql.Array mang = rs.getArray(cot);
        if (mang == null) {
            return List.of();
        }
        return List.of((String[]) mang.getArray());
    }

    private static Integer soNguyenHoacNull(ResultSet rs, String cot) throws SQLException {
        int gia_tri = rs.getInt(cot);
        return rs.wasNull() ? null : gia_tri;
    }

    private static Double soThucHoacNull(ResultSet rs, String cot) throws SQLException {
        double gia_tri = rs.getDouble(cot);
        return rs.wasNull() ? null : gia_tri;
    }
}
