package vn.travel.booking.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.admin.dto.AdminProductQuery;
import vn.travel.booking.admin.dto.AdminProductRow;
import vn.travel.booking.admin.dto.DestinationOption;
import vn.travel.booking.admin.dto.MarketState;
import vn.travel.booking.admin.dto.TranslationState;
import vn.travel.booking.common.dto.PagedResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Danh sách sản phẩm nhìn từ trang quản trị (docs/22 M2).
 *
 * <p><b>Bốn điều kiện của đường đọc công khai cố tình KHÔNG có ở đây</b>, và
 * biết vì sao thì mới đọc đúng lớp này:
 *
 * <ul>
 *   <li>Không lọc {@code market} làm phạm vi. Nhân viên nhập giá cho cả hai thị
 *       trường trong cùng một màn hình.
 *   <li>Không lọc {@code locale}. Trả <b>mọi</b> bản dịch — màn hình dịch song
 *       song cần bản nguồn và bản đích cạnh nhau.
 *   <li>Không đòi {@code status = 'PUBLISHED'}. Nội dung chưa xuất bản chính là
 *       thứ nhân viên vào đây để làm.
 *   <li>Không đòi {@code pm.is_published}. Sản phẩm chưa gán thị trường vẫn phải
 *       thấy, nếu không thì không ai gán được cho nó.
 * </ul>
 *
 * <p>Điều kiện duy nhất giữ lại là {@code NOT soft_delete}: xoá mềm áp cho cả
 * hai bề mặt, và ADR-003 đòi nó nhìn thấy được trong câu truy vấn.
 */
@Repository
public class AdminCatalogRepository {

    /**
     * Bản dịch nguồn {@code INNER JOIN} chứ không {@code LEFT JOIN}: ràng buộc
     * {@code ct_product_source_translation} bảo đảm nó luôn tồn tại, nên
     * {@code LEFT JOIN} chỉ thêm một nhánh {@code null} không bao giờ chạy tới —
     * và một cái bẫy cho người đọc sau.
     */
    private static final String NGUON = """
            FROM product p
            JOIN product_translation src
              ON src.product_id = p.id
             AND src.locale = ?
             AND NOT src.soft_delete
            WHERE NOT p.soft_delete
            """;

    private final JdbcTemplate jdbc;

    public AdminCatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PagedResult<AdminProductRow> findProducts(String localeNguon, AdminProductQuery query) {
        List<Object> thamSo = new ArrayList<>();
        thamSo.add(localeNguon);
        String loc = dungMenhDeLoc(query, thamSo);

        Long tong = jdbc.queryForObject(
                "SELECT count(*) " + NGUON + loc, Long.class, thamSo.toArray());
        long totalItems = tong == null ? 0L : tong;

        List<Object> thamSoTrang = new ArrayList<>(thamSo);
        thamSoTrang.add(query.size());
        thamSoTrang.add((long) query.page() * query.size());

        List<AdminProductRow> khung = jdbc.query(
                "SELECT p.id, p.product_type, src.title, src.status, src.last_modified_at\n"
                        + NGUON + loc
                        + "ORDER BY src.last_modified_at DESC, p.id\n"
                        + "LIMIT ? OFFSET ?",
                (rs, i) -> new AdminProductRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("product_type"),
                        rs.getString("title"),
                        rs.getString("status"),
                        List.of(), List.of(),
                        rs.getObject("last_modified_at", OffsetDateTime.class)),
                thamSoTrang.toArray());

        return new PagedResult<>(dienThemChiTiet(khung, localeNguon),
                query.page(), query.size(), totalItems);
    }

    /**
     * Điểm đến để chọn khi tạo sản phẩm.
     *
     * <p>Tên lấy ở <b>ngôn ngữ nguồn</b> — trang quản trị dùng một ngôn ngữ
     * (docs/22 mục 8), và ngôn ngữ đó không nhất thiết là ngôn ngữ giao diện:
     * nội dung thì luôn viết bằng ngôn ngữ nguồn trước (ADR-004).
     *
     * <p>Không lọc theo thị trường và không đếm sản phẩm. Đây là danh sách để
     * chọn, không phải trang danh mục.
     */
    public List<DestinationOption> findDestinations(String localeNguon) {
        return jdbc.query("""
                SELECT d.id, d.code, dt.name, rt.name AS region_name
                FROM destination d
                JOIN destination_translation dt
                  ON dt.destination_id = d.id AND dt.locale = ? AND NOT dt.soft_delete
                JOIN region r
                  ON r.id = d.region_id AND NOT r.soft_delete
                JOIN region_translation rt
                  ON rt.region_id = r.id AND rt.locale = ? AND NOT rt.soft_delete
                WHERE NOT d.soft_delete
                ORDER BY r.sort_order, d.sort_order, dt.name
                """,
                (rs, i) -> new DestinationOption(
                        rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getString("region_name")),
                localeNguon, localeNguon);
    }

    // ------------------------------------------------------------ lọc

    private static String dungMenhDeLoc(AdminProductQuery query, List<Object> thamSo) {
        StringBuilder sb = new StringBuilder();

        if (query.productType() != null) {
            sb.append(" AND p.product_type = ?");
            thamSo.add(query.productType());
        }
        if (query.market() != null) {
            // EXISTS chứ không JOIN: JOIN product_market nhân đôi số dòng khi một
            // sản phẩm bán ở cả hai thị trường (ADR-006), và tổng đếm ra sai.
            sb.append(" AND EXISTS (SELECT 1 FROM product_market pm"
                    + " WHERE pm.product_id = p.id AND pm.market = ?)");
            thamSo.add(query.market());
        }
        if (query.q() != null && !query.q().isBlank()) {
            // Tìm trong tiêu đề của MỌI locale: nhân viên gõ "Hội An" hay "Halong"
            // đều phải ra, và họ không nhớ tour này nhập bằng tiếng nào.
            // f_unaccent là bản IMMUTABLE tự khai (docs/12 mục 1) — unaccent()
            // trần chỉ STABLE nên index GIN không dùng được với nó.
            sb.append(" AND EXISTS (SELECT 1 FROM product_translation q2"
                    + " WHERE q2.product_id = p.id AND NOT q2.soft_delete"
                    + " AND f_unaccent(q2.title) ILIKE '%' || f_unaccent(?) || '%')");
            thamSo.add(query.q().trim());
        }
        if ("MISSING".equals(query.gap())) {
            sb.append(" AND EXISTS (SELECT 1 FROM locale l"
                    + " WHERE l.is_active AND NOT l.is_source"
                    + " AND NOT EXISTS (SELECT 1 FROM product_translation t"
                    + " WHERE t.product_id = p.id AND t.locale = l.code AND NOT t.soft_delete))");
        }
        if ("OUTDATED".equals(query.gap())) {
            sb.append(" AND EXISTS (SELECT 1 FROM locale l"
                    + " JOIN product_translation t"
                    + " ON t.locale = l.code AND t.product_id = p.id AND NOT t.soft_delete"
                    + " WHERE l.is_active AND NOT l.is_source"
                    + " AND (t.translated_at IS NULL OR src.last_modified_at > t.translated_at))");
        }
        return sb.isEmpty() ? "" : sb.append('\n').toString();
    }

    // ------------------------------------------------------------ chi tiết

    /**
     * Hai truy vấn phụ cho <b>cả trang</b>, không phải hai truy vấn cho mỗi dòng.
     * Một trang 20 sản phẩm × 2 locale × 2 thị trường vẫn là ba lượt đi CSDL.
     */
    private List<AdminProductRow> dienThemChiTiet(List<AdminProductRow> khung, String localeNguon) {
        if (khung.isEmpty()) {
            return khung;
        }
        List<UUID> ids = khung.stream().map(AdminProductRow::id).toList();
        String o = ids.stream().map(x -> "?").collect(Collectors.joining(","));

        Object[] thamSoDich = new Object[ids.size() + 1];
        thamSoDich[0] = localeNguon;
        for (int i = 0; i < ids.size(); i++) {
            thamSoDich[i + 1] = ids.get(i);
        }

        Map<UUID, List<TranslationState>> banDich = new LinkedHashMap<>();
        jdbc.query(
                "SELECT t.product_id, t.locale, t.status, t.translated_at,\n"
                        + "       l.is_source, s.last_modified_at AS source_modified\n"
                        + "FROM product_translation t\n"
                        + "JOIN locale l ON l.code = t.locale\n"
                        + "JOIN product_translation s\n"
                        + "  ON s.product_id = t.product_id AND s.locale = ? AND NOT s.soft_delete\n"
                        + "WHERE NOT t.soft_delete AND t.product_id IN (" + o + ")\n"
                        + "ORDER BY l.is_source DESC, t.locale",
                rs -> {
                    boolean laNguon = rs.getBoolean("is_source");
                    OffsetDateTime dichLuc = rs.getObject("translated_at", OffsetDateTime.class);
                    OffsetDateTime nguonSuaLuc = rs.getObject("source_modified", OffsetDateTime.class);
                    banDich.computeIfAbsent(rs.getObject("product_id", UUID.class), k -> new ArrayList<>())
                            .add(new TranslationState(
                                    rs.getString("locale"), rs.getString("status"), laNguon,
                                    !laNguon && (dichLuc == null || nguonSuaLuc.isAfter(dichLuc))));
                },
                thamSoDich);

        Map<UUID, List<MarketState>> thiTruong = new LinkedHashMap<>();
        jdbc.query("SELECT product_id, market, is_published FROM product_market"
                        + " WHERE product_id IN (" + o + ") ORDER BY market",
                rs -> {
                    // Thân lambda phải là KHỐI: `add()` trả về boolean, và khi đó
                    // trình biên dịch không phân biệt được RowCallbackHandler với
                    // ResultSetExtractor — lỗi "reference to query is ambiguous".
                    thiTruong.computeIfAbsent(rs.getObject("product_id", UUID.class), k -> new ArrayList<>())
                            .add(new MarketState(rs.getString("market"), rs.getBoolean("is_published")));
                },
                ids.toArray());

        return khung.stream()
                .map(r -> new AdminProductRow(
                        r.id(), r.productType(), r.sourceTitle(), r.sourceStatus(),
                        thiTruong.getOrDefault(r.id(), List.of()),
                        banDich.getOrDefault(r.id(), List.of()),
                        r.lastModifiedAt()))
                .toList();
    }
}
