package vn.travel.booking.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.admin.dto.CoverageRow;
import vn.travel.booking.admin.dto.QueueItem;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hàng đợi dịch và bảng độ phủ (docs/22 M10 và M12).
 *
 * <p>Cả hai trả lời cùng một câu hỏi từ hai phía: <b>còn gì chưa dịch</b> và
 * <b>đã dịch được bao nhiêu</b>. Vì thế chúng dùng chung đúng một định nghĩa
 * {@code OUTDATED}, đặt ở một chỗ trong lớp này. Hai định nghĩa ở hai lớp là
 * cách chắc chắn để hai màn hình cãi nhau về cùng một bản ghi.
 *
 * <p><b>{@code OUTDATED} tính ra, không lưu.</b> Nó là
 * {@code nguồn.last_modified_at > đích.translated_at}. Thêm một cột cờ là tạo ra
 * thứ có thể sai — và nó sẽ sai, vào đúng lúc ai đó cập nhật bản nguồn bằng một
 * câu SQL, không qua ứng dụng.
 *
 * <p><b>{@code translated_at} chứ không {@code status}</b> là mốc "đã dịch": hai
 * cột nói hai chuyện khác nhau — dịch xong khác với sửa chính tả, và
 * {@code product_translation} tách chúng ra có chủ ý (docs/12 mục 4.6).
 *
 * <h2>Vì sao chỉ có sản phẩm và bài viết</h2>
 *
 * <p>docs/22 mục 4.1 muốn cả điểm đến, nhưng {@code destination_translation} và
 * {@code lecture_translation} <b>không có</b> cột {@code status} lẫn
 * {@code translated_at}. Với chúng thì "bản nguồn đã xuất bản chưa" và "dịch từ
 * lúc nào" đều không trả lời được.
 *
 * <p>Thêm hai cột đó là định nghĩa một vòng đời xuất bản cho điểm đến — thứ chưa
 * tài liệu nào mô tả, và nó đụng thẳng vào chính sách không-fallback. Nên: chờ
 * quyết định, không tự thêm cột.
 */
@Repository
public class TranslationWorkRepository {

    /**
     * Bậc 1 và 2 của docs/22 mục 4.1.1: sản phẩm <b>đang bán</b> gấp hơn sản phẩm
     * mới chỉ xuất bản bản nguồn. Chưa dịch một tour đang bán là mất doanh thu
     * ngay hôm nay, không phải mất về sau.
     */
    private static final String SAN_PHAM = """
            SELECT 'PRODUCT'                AS entity_type,
                   p.id                     AS id,
                   l.code                   AS locale,
                   CASE WHEN t.product_id IS NULL THEN 'MISSING' ELSE 'OUTDATED' END AS gap,
                   CASE WHEN EXISTS (SELECT 1 FROM product_market pm
                                      WHERE pm.product_id = p.id AND pm.is_published)
                        THEN 1 ELSE 2 END   AS priority,
                   src.title                AS source_title,
                   src.last_modified_at     AS source_modified,
                   t.translated_at          AS translated_at
            FROM product p
            JOIN product_translation src
              ON src.product_id = p.id
             AND src.locale = ?
             AND NOT src.soft_delete
             AND src.status = 'PUBLISHED'
            CROSS JOIN locale l
            LEFT JOIN product_translation t
              ON t.product_id = p.id
             AND t.locale = l.code
             AND NOT t.soft_delete
            WHERE NOT p.soft_delete
              AND l.is_active
              AND NOT l.is_source
              AND (t.product_id IS NULL
                   OR t.translated_at IS NULL
                   OR src.last_modified_at > t.translated_at)
            """;

    /** Bậc 4: bài viết xếp sau mọi sản phẩm, kể cả sản phẩm chưa gán thị trường. */
    private static final String BAI_VIET = """
            SELECT 'POST'                   AS entity_type,
                   po.id                    AS id,
                   l.code                   AS locale,
                   CASE WHEN t.post_id IS NULL THEN 'MISSING' ELSE 'OUTDATED' END AS gap,
                   4                        AS priority,
                   src.title                AS source_title,
                   src.last_modified_at     AS source_modified,
                   t.translated_at          AS translated_at
            FROM post po
            JOIN post_translation src
              ON src.post_id = po.id
             AND src.locale = ?
             AND NOT src.soft_delete
             AND src.status = 'PUBLISHED'
            CROSS JOIN locale l
            LEFT JOIN post_translation t
              ON t.post_id = po.id
             AND t.locale = l.code
             AND NOT t.soft_delete
            WHERE NOT po.soft_delete
              AND l.is_active
              AND NOT l.is_source
              AND (t.post_id IS NULL
                   OR t.translated_at IS NULL
                   OR src.last_modified_at > t.translated_at)
            """;

    private final JdbcTemplate jdbc;

    public TranslationWorkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------ hàng đợi

    public List<QueueItem> queue(String localeNguon, String entityType, int limit) {
        List<String> block = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (entityType == null || "PRODUCT".equals(entityType)) {
            block.add(SAN_PHAM);
            params.add(localeNguon);
        }
        if (entityType == null || "POST".equals(entityType)) {
            block.add(BAI_VIET);
            params.add(localeNguon);
        }
        if (block.isEmpty()) {
            return List.of();
        }
        params.add(limit);

        // Sắp theo priority TRƯỚC, ngày sau. Sắp theo ngày là để một dòng chữ
        // trong bài blog chen lên trước một tour đang bán (docs/22 mục 4.1.1).
        String sql = "SELECT * FROM (\n" + String.join("\nUNION ALL\n", block) + "\n) x\n"
                + "ORDER BY x.priority, x.source_modified DESC\n"
                + "LIMIT ?";

        return jdbc.query(sql,
                (rs, i) -> new QueueItem(
                        rs.getString("entity_type"),
                        rs.getObject("id", UUID.class),
                        rs.getString("locale"),
                        rs.getString("gap"),
                        rs.getInt("priority"),
                        rs.getString("source_title"),
                        rs.getObject("source_modified", OffsetDateTime.class),
                        rs.getObject("translated_at", OffsetDateTime.class)),
                params.toArray());
    }

    // ------------------------------------------------------------ độ phủ

    /**
     * Trả <b>số đếm</b>, không trả phần trăm: chia và làm tròn là việc của
     * frontend (docs/13 mục 5). Mẫu số chỉ tính bản ghi có bản nguồn
     * {@code PUBLISHED} — bản nguồn còn là nháp thì chưa đến lượt dịch, đưa nó
     * vào mẫu số là làm độ phủ tụt xuống vì một việc chưa ai được phép làm.
     */
    public List<CoverageRow> coverage(String localeNguon, String locale) {
        List<CoverageRow> ket_qua = new ArrayList<>();
        ket_qua.addAll(countOne("PRODUCT", "product", "product_translation", "product_id",
                localeNguon, locale));
        ket_qua.addAll(countOne("POST", "post", "post_translation", "post_id",
                localeNguon, locale));
        return ket_qua;
    }

    /**
     * Tên bảng và tên cột nối thẳng vào SQL, và điều đó an toàn ở đây vì cả bốn
     * đều là <b>hằng số trong mã nguồn</b>, không có đường nào cho dữ liệu người
     * dùng chạm tới. Giá trị do người dùng nhập vẫn đi bằng tham số {@code ?}.
     */
    private List<CoverageRow> countOne(String entityType, String table, String bangDich,
                                     String cotKhoa, String localeNguon, String locale) {
        List<Object> params = new ArrayList<>();
        params.add(localeNguon);

        String localeFilter = "";
        if (locale != null) {
            localeFilter = " AND l.code = ?";
            params.add(locale);
        }

        String sql = "SELECT l.code AS locale,\n"
                + "       count(*) AS total,\n"
                + "       count(*) FILTER (WHERE t.translated_at IS NOT NULL) AS translated,\n"
                + "       count(*) FILTER (WHERE t.translated_at IS NOT NULL\n"
                + "                          AND t.translated_at >= src.last_modified_at) AS up_to_date\n"
                + "FROM " + table + " e\n"
                + "JOIN " + bangDich + " src\n"
                + "  ON src." + cotKhoa + " = e.id AND src.locale = ?\n"
                + " AND NOT src.soft_delete AND src.status = 'PUBLISHED'\n"
                + "CROSS JOIN locale l\n"
                + "LEFT JOIN " + bangDich + " t\n"
                + "  ON t." + cotKhoa + " = e.id AND t.locale = l.code AND NOT t.soft_delete\n"
                + "WHERE NOT e.soft_delete AND l.is_active AND NOT l.is_source" + localeFilter + "\n"
                + "GROUP BY l.code\n"
                + "ORDER BY l.code";

        return jdbc.query(sql,
                (rs, i) -> new CoverageRow(entityType, rs.getString("locale"),
                        rs.getInt("total"), rs.getInt("translated"), rs.getInt("up_to_date")),
                params.toArray());
    }
}
