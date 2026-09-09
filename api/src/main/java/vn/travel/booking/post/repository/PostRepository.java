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
        List<Object> params = new ArrayList<>();
        params.add(locale);
        String filter = "";

        if (tagSlugs != null && !tagSlugs.isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(tagSlugs.size(), "?"));
            filter = """
                     AND EXISTS (SELECT 1 FROM post_tag ptg
                                   JOIN tag_translation tt
                                     ON tt.tag_id = ptg.tag_id
                                    AND tt.locale = ?
                                    AND NOT tt.soft_delete
                                  WHERE ptg.post_id = p.id
                                    AND tt.slug IN (%s))
                    """.formatted(placeholders);
            params.add(locale);
            params.addAll(tagSlugs);
        }

        Long total = jdbc.queryForObject("SELECT count(*) " + NGUON + filter, Long.class, params.toArray());
        long totalItems = total == null ? 0L : total;

        int offset = page * size;
        if (totalItems == 0 || offset >= totalItems) {
            return new PagedResult<>(List.of(), page, size, totalItems);
        }

        List<Object> pagingParams = new ArrayList<>(params);
        pagingParams.add(size);
        pagingParams.add(offset);

        List<Object[]> row = jdbc.query("""
                SELECT p.id, pt.slug, pt.title, pt.excerpt, p.hero_image, p.published_at
                """ + NGUON + filter + """
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
                pagingParams.toArray());

        Map<UUID, List<NamedRef>> tag = readTag(locale, row.stream().map(r -> (UUID) r[0]).toList());

        List<PostSummary> items = row.stream()
                .map(r -> new PostSummary(
                        (String) r[1], (String) r[2], (String) r[3], (String) r[4],
                        (OffsetDateTime) r[5],
                        tag.getOrDefault((UUID) r[0], List.of())))
                .toList();

        return new PagedResult<>(items, page, size, totalItems);
    }
    public Optional<PostDetail> findPost(String locale, String slug) {
        List<Object[]> row = jdbc.query("""
                SELECT p.id, pt.slug, pt.title, pt.excerpt, pt.body, p.hero_image, p.published_at
                """ + NGUON + " AND pt.slug = ?",
                (rs, i) -> new Object[]{
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("title"),
                        rs.getString("excerpt"),
                        array(rs, "body"),
                        rs.getString("hero_image"),
                        rs.getObject("published_at", OffsetDateTime.class)},
                locale, slug);

        if (row.isEmpty()) {
            return Optional.empty();
        }

        Object[] r = row.get(0);
        UUID id = (UUID) r[0];

        return Optional.of(new PostDetail(
                (String) r[1], (String) r[2], (String) r[3],
                castList(r[4]),
                (String) r[5], (OffsetDateTime) r[6],
                readTag(locale, List.of(id)).getOrDefault(id, List.of())));
    }

    /**
     * Một truy vấn cho toàn bộ thẻ của cả trang, không phải một truy vấn cho mỗi
     * bài — bài toán N+1 ở một danh sách 12 bài là 13 lần đi lại cơ sở dữ liệu
     * thay vì 2.
     */
    private Map<UUID, List<NamedRef>> readTag(String locale, List<UUID> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(postIds.size(), "?"));

        List<Object> params = new ArrayList<>();
        params.add(locale);
        params.addAll(postIds);

        Map<UUID, List<NamedRef>> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT ptg.post_id, tt.slug, tt.name
                FROM post_tag ptg
                JOIN tag t ON t.id = ptg.tag_id AND NOT t.soft_delete
                JOIN tag_translation tt
                  ON tt.tag_id = t.id AND tt.locale = ? AND NOT tt.soft_delete
                WHERE ptg.post_id IN (%s)
                ORDER BY t.sort_order, tt.name
                """.formatted(placeholders),
                rs -> {
                    result.computeIfAbsent(rs.getObject("post_id", UUID.class), k -> new ArrayList<>())
                            .add(new NamedRef(rs.getString("slug"), rs.getString("name")));
                },
                params.toArray());

        return result;
    }

    private static List<String> array(ResultSet rs, String column) throws SQLException {
        java.sql.Array array = rs.getArray(column);
        return array == null ? List.of() : List.of((String[]) array.getArray());
    }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object value) {
        return (List<String>) value;
    }
}
