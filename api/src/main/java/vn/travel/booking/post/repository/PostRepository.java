package vn.travel.booking.post.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.post.dto.PostDetail;
import vn.travel.booking.post.repository.PostRepository;
import vn.travel.booking.post.dto.PostSummary;
import vn.travel.booking.product.dto.NamedRef;
import vn.travel.booking.common.dto.PagedResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostRepository {

    private static final String NGUON = """
            FROM post p
            JOIN post_translation pt
              ON pt.post_id = p.id
             AND pt.locale = ?
             AND pt.status = 'PUBLISHED'
             AND NOT pt.soft_delete
            WHERE NOT p.soft_delete
              AND p.published_at IS NOT NULL
            """;

    private final JdbcTemplate jdbc;

    public PostRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lọc theo thẻ dùng {@code EXISTS} chứ không {@code JOIN}: với
     * {@code JOIN post_tag} thì một bài mang hai thẻ đang lọc sẽ xuất hiện
     * <b>hai lần</b>, và {@code totalItems} đếm sai theo. {@code DISTINCT} chữa
     * được nhưng làm câu đếm và câu lấy trang lệch nhau về chi phí — {@code EXISTS}
     * nói đúng ý định: "bài này có ít nhất một trong các thẻ đó".
     */
    public PagedResult<PostSummary> findPosts(String locale, List<String> tagSlugs, int page, int size) {
        List<Object> thamSo = new ArrayList<>();
        thamSo.add(locale);
        String loc = "";

        if (tagSlugs != null && !tagSlugs.isEmpty()) {
            String dauHoi = String.join(",", java.util.Collections.nCopies(tagSlugs.size(), "?"));
            loc = """
                     AND EXISTS (SELECT 1 FROM post_tag ptg
                                   JOIN tag_translation tt
                                     ON tt.tag_id = ptg.tag_id
                                    AND tt.locale = ?
                                    AND NOT tt.soft_delete
                                  WHERE ptg.post_id = p.id
                                    AND tt.slug IN (%s))
                    """.formatted(dauHoi);
            thamSo.add(locale);
            thamSo.addAll(tagSlugs);
        }

        Long tong = jdbc.queryForObject("SELECT count(*) " + NGUON + loc, Long.class, thamSo.toArray());
        long totalItems = tong == null ? 0L : tong;

        int offset = page * size;
        if (totalItems == 0 || offset >= totalItems) {
            return new PagedResult<>(List.of(), page, size, totalItems);
        }

        List<Object> thamSoTrang = new ArrayList<>(thamSo);
        thamSoTrang.add(size);
        thamSoTrang.add(offset);

        List<Object[]> dong = jdbc.query("""
                SELECT p.id, pt.slug, pt.title, pt.excerpt, p.hero_image, p.published_at
                """ + NGUON + loc + """
                 ORDER BY p.published_at DESC, pt.slug
                 LIMIT ? OFFSET ?
                """,
                (rs, i) -> new Object[]{
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("title"),
                        rs.getString("excerpt"),
                        rs.getString("hero_image"),
                        rs.getObject("published_at", OffsetDateTime.class)},
                thamSoTrang.toArray());

        Map<UUID, List<NamedRef>> the = docThe(locale, dong.stream().map(d -> (UUID) d[0]).toList());

        List<PostSummary> items = dong.stream()
                .map(d -> new PostSummary(
                        (String) d[1], (String) d[2], (String) d[3], (String) d[4],
                        (OffsetDateTime) d[5],
                        the.getOrDefault((UUID) d[0], List.of())))
                .toList();

        return new PagedResult<>(items, page, size, totalItems);
    }
    public Optional<PostDetail> findPost(String locale, String slug) {
        List<Object[]> dong = jdbc.query("""
                SELECT p.id, pt.slug, pt.title, pt.excerpt, pt.body, p.hero_image, p.published_at
                """ + NGUON + " AND pt.slug = ?",
                (rs, i) -> new Object[]{
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("title"),
                        rs.getString("excerpt"),
                        mang(rs, "body"),
                        rs.getString("hero_image"),
                        rs.getObject("published_at", OffsetDateTime.class)},
                locale, slug);

        if (dong.isEmpty()) {
            return Optional.empty();
        }

        Object[] d = dong.get(0);
        UUID id = (UUID) d[0];

        return Optional.of(new PostDetail(
                (String) d[1], (String) d[2], (String) d[3],
                castDanhSach(d[4]),
                (String) d[5], (OffsetDateTime) d[6],
                docThe(locale, List.of(id)).getOrDefault(id, List.of())));
    }

    /**
     * Một truy vấn cho toàn bộ thẻ của cả trang, không phải một truy vấn cho mỗi
     * bài — bài toán N+1 ở một danh sách 12 bài là 13 lần đi lại cơ sở dữ liệu
     * thay vì 2.
     */
    private Map<UUID, List<NamedRef>> docThe(String locale, List<UUID> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        String dauHoi = String.join(",", java.util.Collections.nCopies(postIds.size(), "?"));

        List<Object> thamSo = new ArrayList<>();
        thamSo.add(locale);
        thamSo.addAll(postIds);

        Map<UUID, List<NamedRef>> ket_qua = new LinkedHashMap<>();
        jdbc.query("""
                SELECT ptg.post_id, tt.slug, tt.name
                FROM post_tag ptg
                JOIN tag t ON t.id = ptg.tag_id AND NOT t.soft_delete
                JOIN tag_translation tt
                  ON tt.tag_id = t.id AND tt.locale = ? AND NOT tt.soft_delete
                WHERE ptg.post_id IN (%s)
                ORDER BY t.sort_order, tt.name
                """.formatted(dauHoi),
                rs -> {
                    ket_qua.computeIfAbsent(rs.getObject("post_id", UUID.class), k -> new ArrayList<>())
                            .add(new NamedRef(rs.getString("slug"), rs.getString("name")));
                },
                thamSo.toArray());

        return ket_qua;
    }

    private static List<String> mang(ResultSet rs, String cot) throws SQLException {
        java.sql.Array mang = rs.getArray(cot);
        return mang == null ? List.of() : List.of((String[]) mang.getArray());
    }

    @SuppressWarnings("unchecked")
    private static List<String> castDanhSach(Object gia_tri) {
        return (List<String>) gia_tri;
    }
}
