package vn.travel.booking.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.travel.booking.admin.dto.ContentLocaleState;
import vn.travel.booking.admin.dto.DestinationDetailView;
import vn.travel.booking.admin.dto.DestinationTranslationInput;
import vn.travel.booking.admin.dto.DestinationTranslationView;
import vn.travel.booking.admin.dto.LectureCreateInput;
import vn.travel.booking.admin.dto.LectureDetailView;
import vn.travel.booking.admin.dto.LecturePatchInput;
import vn.travel.booking.admin.dto.LectureRow;
import vn.travel.booking.admin.dto.LectureTranslationInput;
import vn.travel.booking.admin.dto.LectureTranslationView;
import vn.travel.booking.admin.dto.PostCreateInput;
import vn.travel.booking.admin.dto.PostDetailView;
import vn.travel.booking.admin.dto.PostPatchInput;
import vn.travel.booking.admin.dto.PostRow;
import vn.travel.booking.admin.dto.PostTranslationInput;
import vn.travel.booking.admin.dto.PostTranslationView;
import vn.travel.booking.admin.dto.TagView;
import vn.travel.booking.common.dto.PagedResult;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Nội dung khác — điểm đến, bài viết, buổi thuyết trình (docs/22 M13).
 *
 * <p>SQL thuần cho <b>cả hai</b> chiều, khác quy ước chung của đường ghi quản
 * trị (docs/10 mục 6), và có lý do:
 *
 * <ul>
 *   <li>Ba bảng dịch đều là {@code upsert} theo khoá kép — {@code ON CONFLICT}
 *       làm việc đó trong một câu, còn JPA cần đọc-rồi-ghi và mở ra một khoảng
 *       trống giữa hai bước.
 *   <li>Cột trạng thái từng locale — <b>cột quan trọng nhất</b> của ba danh
 *       sách — là một phép gộp, không phải một trường của entity.
 *   <li>{@code post_tag} là bảng nối nhóm C, không cột kiểm toán, thay sạch mỗi
 *       lần lưu. Dựng entity cho nó là mời người sau gọi {@code save()} lần hai.
 * </ul>
 *
 * <p><b>Mọi truy vấn đọc đều có {@code AND NOT soft_delete}.</b> Xoá mềm hoạt
 * động âm thầm — quên một chỗ là rò dữ liệu đã xoá ra màn hình mà không có lỗi
 * nào nổ (api/CLAUDE.md mục 7b).
 */
@Repository
public class AdminContentRepository {

    private final JdbcTemplate jdbc;

    public AdminContentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==================================================== điểm đến

    public boolean destinationExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM destination WHERE id = ? AND NOT soft_delete)",
                Boolean.class, id));
    }

    public Optional<DestinationDetailView> findDestination(UUID id, String sourceLocale) {
        List<DestinationDetailView> tim = jdbc.query("""
                SELECT d.id, d.code, d.region_id, d.sort_order,
                       d.last_modified_at, s.display_name AS nguoi_sua,
                       rt.name AS region_name,
                       (SELECT count(*) FROM product p
                         WHERE p.primary_destination_id = d.id AND NOT p.soft_delete) AS so_san_pham
                FROM destination d
                LEFT JOIN staff_user s ON s.id = d.last_modified_by
                LEFT JOIN region_translation rt
                  ON rt.region_id = d.region_id AND rt.locale = ? AND NOT rt.soft_delete
                WHERE d.id = ? AND NOT d.soft_delete
                """,
                (rs, i) -> new DestinationDetailView(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getObject("region_id", UUID.class),
                        rs.getString("region_name"),
                        rs.getInt("sort_order"),
                        rs.getInt("so_san_pham"),
                        destinationTranslations(id, sourceLocale),
                        rs.getObject("last_modified_at", OffsetDateTime.class),
                        rs.getString("nguoi_sua")),
                sourceLocale, id);

        return tim.stream().findFirst();
    }

    private List<DestinationTranslationView> destinationTranslations(UUID id, String sourceLocale) {
        return jdbc.query("""
                SELECT dt.locale, dt.slug, dt.name, dt.summary,
                       dt.last_modified_at, s.display_name AS nguoi_sua
                FROM destination_translation dt
                LEFT JOIN staff_user s ON s.id = dt.last_modified_by
                WHERE dt.destination_id = ? AND NOT dt.soft_delete
                ORDER BY dt.locale
                """,
                (rs, i) -> new DestinationTranslationView(
                        rs.getString("locale"),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getString("summary"),
                        sourceLocale.equals(rs.getString("locale")),
                        rs.getObject("last_modified_at", OffsetDateTime.class),
                        rs.getString("nguoi_sua")),
                id);
    }

    /**
     * {@code COALESCE}: trường vắng thì giữ nguyên giá trị cũ.
     *
     * <p>Đó đúng là điều {@code PATCH} nói, và viết bằng SQL thì không phải dựng
     * câu động theo số trường có mặt — thứ luôn sinh ra một nhánh không ai test.
     */
    public void patchDestination(UUID id, UUID regionId, Integer sortOrder, UUID staffUserId) {
        jdbc.update("""
                UPDATE destination
                SET region_id = COALESCE(?, region_id),
                    sort_order = COALESCE(?, sort_order),
                    last_modified_by = ?
                WHERE id = ?
                """, regionId, sortOrder, staffUserId, id);
    }

    public int countProductsOfDestination(UUID id) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM product
                WHERE primary_destination_id = ? AND NOT soft_delete
                """, Integer.class, id);
        return count == null ? 0 : count;
    }

    /**
     * Xoá mềm điểm đến <b>và cả chùm bản dịch của nó</b>.
     *
     * <p>Xoá mềm không lan xuống dưới: {@code ON DELETE CASCADE} chỉ chạy khi
     * xoá cứng (api/CLAUDE.md mục 7b). Bỏ câu thứ hai thì bản dịch vẫn "sống",
     * và slug của nó vẫn chiếm chỗ trong index duy nhất bộ phận — người sau tạo
     * lại điểm đến cùng slug bị từ chối, mà thông báo lỗi không nói gì về nguyên
     * nhân thật.
     */
    public void softDeleteDestination(UUID id, UUID staffUserId) {
        jdbc.update("UPDATE destination SET soft_delete = TRUE, last_modified_by = ? WHERE id = ?",
                staffUserId, id);
        jdbc.update("""
                UPDATE destination_translation SET soft_delete = TRUE, last_modified_by = ?
                WHERE destination_id = ?
                """, staffUserId, id);
    }

    public void saveDestinationTranslation(UUID id, String locale,
                                           DestinationTranslationInput input, UUID staffUserId) {
        jdbc.update("""
                INSERT INTO destination_translation
                       (destination_id, locale, slug, name, summary, created_by, last_modified_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (destination_id, locale) DO UPDATE
                SET slug = EXCLUDED.slug,
                    name = EXCLUDED.name,
                    summary = EXCLUDED.summary,
                    soft_delete = FALSE,
                    last_modified_by = EXCLUDED.last_modified_by
                """,
                id, locale, input.slug(), input.name(), input.summary(), staffUserId, staffUserId);
    }

    // ==================================================== thẻ

    public List<TagView> tags(String sourceLocale) {
        return jdbc.query("""
                SELECT t.id, t.code, tt.name
                FROM tag t
                JOIN tag_translation tt
                  ON tt.tag_id = t.id AND tt.locale = ? AND NOT tt.soft_delete
                WHERE NOT t.soft_delete
                ORDER BY t.sort_order, tt.name
                """,
                (rs, i) -> new TagView(rs.getObject("id", UUID.class),
                        rs.getString("code"), rs.getString("name")),
                sourceLocale);
    }

    // ==================================================== bài viết

    public boolean postExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM post WHERE id = ? AND NOT soft_delete)",
                Boolean.class, id));
    }

    public PagedResult<PostRow> findPosts(String q, int page, int size, String sourceLocale) {
        String loc = "";
        List<Object> params = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            // Khớp tiêu đề ở BẤT KỲ ngôn ngữ nào: nhân viên nhớ tên bài bằng
            // thứ tiếng họ đang làm việc, không phải bằng ngôn ngữ nguồn.
            loc = """
                   AND EXISTS (SELECT 1 FROM post_translation pt
                                WHERE pt.post_id = p.id AND NOT pt.soft_delete
                                  AND pt.title ILIKE '%' || ? || '%')
                  """;
            params.add(q.trim());
        }

        Long tong = jdbc.queryForObject(
                "SELECT count(*) FROM post p WHERE NOT p.soft_delete\n" + loc,
                Long.class, params.toArray());
        long totalItems = tong == null ? 0L : tong;

        List<Object> thamSoTrang = new ArrayList<>(params);
        thamSoTrang.add(size);
        thamSoTrang.add((long) page * size);

        List<PostRow> row = jdbc.query(
                """
                SELECT p.id, p.hero_image, p.published_at,
                       p.last_modified_at, s.display_name AS nguoi_sua
                FROM post p
                LEFT JOIN staff_user s ON s.id = p.last_modified_by
                WHERE NOT p.soft_delete
                """ + loc
                        // Bài mới nhất trên cùng; NULLS FIRST để bài chưa đặt
                        // ngày xuất bản — tức bài đang soạn dở — không rơi
                        // xuống cuối, đúng chỗ ít ai cuộn tới nhất.
                        + "ORDER BY p.published_at DESC NULLS FIRST, p.id\n"
                        + "LIMIT ? OFFSET ?",
                (rs, i) -> {
                    UUID id = rs.getObject("id", UUID.class);
                    return new PostRow(
                            id,
                            postTitle(id, sourceLocale),
                            rs.getString("hero_image"),
                            rs.getObject("published_at", OffsetDateTime.class),
                            postLocales(id),
                            tagsOfPost(id, sourceLocale),
                            rs.getObject("last_modified_at", OffsetDateTime.class),
                            rs.getString("nguoi_sua"));
                },
                thamSoTrang.toArray());

        return new PagedResult<>(row, page, size, totalItems);
    }

    /**
     * Tiêu đề hiện trong danh sách: bản nguồn nếu có, không thì bản nào cũng được.
     *
     * <p>Không trả chuỗi rỗng khi thiếu bản nguồn: một dòng không tiêu đề là một
     * dòng không ai bấm vào, mà bài viết thì vẫn tồn tại và vẫn cần sửa được.
     */
    private String postTitle(UUID id, String sourceLocale) {
        return jdbc.query("""
                SELECT title FROM post_translation
                WHERE post_id = ? AND NOT soft_delete
                ORDER BY (locale = ?) DESC, locale
                LIMIT 1
                """, (rs, i) -> rs.getString(1), id, sourceLocale)
                .stream().findFirst().orElse("(chưa có tiêu đề)");
    }

    private Map<String, ContentLocaleState> postLocales(UUID id) {
        return localeStatus("""
                SELECT l.code, pt.status
                FROM locale l
                LEFT JOIN post_translation pt
                  ON pt.post_id = ? AND pt.locale = l.code AND NOT pt.soft_delete
                ORDER BY l.code
                """, id);
    }

    public Optional<PostDetailView> findPost(UUID id, String sourceLocale) {
        List<PostDetailView> tim = jdbc.query("""
                SELECT p.id, p.hero_image, p.published_at,
                       p.last_modified_at, s.display_name AS nguoi_sua
                FROM post p
                LEFT JOIN staff_user s ON s.id = p.last_modified_by
                WHERE p.id = ? AND NOT p.soft_delete
                """,
                (rs, i) -> new PostDetailView(
                        rs.getObject("id", UUID.class),
                        rs.getString("hero_image"),
                        rs.getObject("published_at", OffsetDateTime.class),
                        tagsOfPost(id, sourceLocale),
                        postTranslations(id, sourceLocale),
                        rs.getObject("last_modified_at", OffsetDateTime.class),
                        rs.getString("nguoi_sua")),
                id);

        return tim.stream().findFirst();
    }

    private List<PostTranslationView> postTranslations(UUID id, String sourceLocale) {
        return jdbc.query("""
                SELECT pt.locale, pt.slug, pt.title, pt.excerpt, pt.body, pt.status,
                       pt.last_modified_at, s.display_name AS nguoi_sua
                FROM post_translation pt
                LEFT JOIN staff_user s ON s.id = pt.last_modified_by
                WHERE pt.post_id = ? AND NOT pt.soft_delete
                ORDER BY pt.locale
                """,
                (rs, i) -> new PostTranslationView(
                        rs.getString("locale"),
                        rs.getString("slug"),
                        rs.getString("title"),
                        rs.getString("excerpt"),
                        mangChu(rs, "body"),
                        rs.getString("status"),
                        sourceLocale.equals(rs.getString("locale")),
                        rs.getObject("last_modified_at", OffsetDateTime.class),
                        rs.getString("nguoi_sua")),
                id);
    }

    private List<TagView> tagsOfPost(UUID id, String sourceLocale) {
        return jdbc.query("""
                SELECT t.id, t.code, tt.name
                FROM post_tag pt
                JOIN tag t ON t.id = pt.tag_id AND NOT t.soft_delete
                JOIN tag_translation tt
                  ON tt.tag_id = t.id AND tt.locale = ? AND NOT tt.soft_delete
                WHERE pt.post_id = ?
                ORDER BY t.sort_order
                """,
                (rs, i) -> new TagView(rs.getObject("id", UUID.class),
                        rs.getString("code"), rs.getString("name")),
                sourceLocale, id);
    }

    public UUID createPost(PostCreateInput input, String sourceLocale, UUID staffUserId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO post (id, hero_image, published_at, created_by, last_modified_by)
                VALUES (?, ?, ?, ?, ?)
                """, id, input.heroImage(), input.publishedAt(), staffUserId, staffUserId);

        savePostTranslation(id, sourceLocale, input.translation(), staffUserId);
        datTheChoBaiViet(id, input.tagIds() == null ? List.of() : input.tagIds());
        return id;
    }

    public void patchPost(UUID id, PostPatchInput input, UUID staffUserId) {
        jdbc.update("""
                UPDATE post
                SET hero_image = COALESCE(?, hero_image),
                    published_at = COALESCE(?, published_at),
                    last_modified_by = ?
                WHERE id = ?
                """, input.heroImage(), input.publishedAt(), staffUserId, id);
    }

    /**
     * Thay <b>sạch</b> tập thẻ.
     *
     * <p>Endpoint riêng chứ không nằm trong {@code PATCH}: bộ sinh mã dựng trường
     * mảng vắng mặt thành danh sách <b>rỗng</b>, nên trong một {@code PATCH} thì
     * "không nhắc tới thẻ" và "gỡ hết thẻ" tới đây giống hệt nhau — và đổi mỗi
     * cái ảnh bìa sẽ lặng lẽ gỡ sạch thẻ của bài.
     */
    public void datTheChoBaiViet(UUID id, List<UUID> tagIds) {
        jdbc.update("DELETE FROM post_tag WHERE post_id = ?", id);
        for (UUID tagId : tagIds) {
            jdbc.update("INSERT INTO post_tag (post_id, tag_id) VALUES (?, ?)", id, tagId);
        }
    }

    public void softDeletePost(UUID id, UUID staffUserId) {
        jdbc.update("UPDATE post SET soft_delete = TRUE, last_modified_by = ? WHERE id = ?",
                staffUserId, id);
        jdbc.update("""
                UPDATE post_translation SET soft_delete = TRUE, last_modified_by = ?
                WHERE post_id = ?
                """, staffUserId, id);
    }

    public void savePostTranslation(UUID id, String locale, PostTranslationInput input,
                                    UUID staffUserId) {
        jdbc.update("""
                INSERT INTO post_translation
                       (post_id, locale, slug, title, excerpt, body, status,
                        created_by, last_modified_by, translated_at, translated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?)
                ON CONFLICT (post_id, locale) DO UPDATE
                SET slug = EXCLUDED.slug,
                    title = EXCLUDED.title,
                    excerpt = EXCLUDED.excerpt,
                    body = EXCLUDED.body,
                    status = EXCLUDED.status,
                    soft_delete = FALSE,
                    translated_at = now(),
                    translated_by = EXCLUDED.translated_by,
                    last_modified_by = EXCLUDED.last_modified_by
                """,
                id, locale, input.slug(), input.title(), input.excerpt(),
                input.body().toArray(new String[0]), input.status(),
                staffUserId, staffUserId, staffUserId);
    }

    // ==================================================== buổi thuyết trình

    public boolean lectureExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM lecture WHERE id = ? AND NOT soft_delete)",
                Boolean.class, id));
    }

    public PagedResult<LectureRow> findLectures(int page, int size, String sourceLocale) {
        Long tong = jdbc.queryForObject(
                "SELECT count(*) FROM lecture WHERE NOT soft_delete", Long.class);
        long totalItems = tong == null ? 0L : tong;

        List<LectureRow> row = jdbc.query("""
                SELECT l.id, l.market, l.event_date, l.start_time, l.city, l.venue,
                       l.seats, l.seats_taken, l.last_modified_at, s.display_name AS nguoi_sua
                FROM lecture l
                LEFT JOIN staff_user s ON s.id = l.last_modified_by
                WHERE NOT l.soft_delete
                ORDER BY l.event_date DESC, l.id
                LIMIT ? OFFSET ?
                """,
                (rs, i) -> {
                    UUID id = rs.getObject("id", UUID.class);
                    return new LectureRow(
                            id,
                            rs.getString("market"),
                            ngay(rs.getDate("event_date")),
                            gio(rs.getTime("start_time")),
                            rs.getString("city"),
                            rs.getString("venue"),
                            rs.getInt("seats"),
                            rs.getInt("seats_taken"),
                            lectureTitle(id, sourceLocale),
                            lectureLocales(id),
                            rs.getObject("last_modified_at", OffsetDateTime.class),
                            rs.getString("nguoi_sua"));
                },
                size, (long) page * size);

        return new PagedResult<>(row, page, size, totalItems);
    }

    private String lectureTitle(UUID id, String sourceLocale) {
        return jdbc.query("""
                SELECT title FROM lecture_translation
                WHERE lecture_id = ? AND NOT soft_delete
                ORDER BY (locale = ?) DESC, locale
                LIMIT 1
                """, (rs, i) -> rs.getString(1), id, sourceLocale)
                .stream().findFirst().orElse("(chưa có tiêu đề)");
    }

    /**
     * Bản dịch buổi thuyết trình <b>không có cột {@code status}</b>, nên chỉ
     * phân biệt được có hay không.
     *
     * <p>Có thì coi là {@code PUBLISHED}: bề mặt khách đọc thẳng bảng dịch này
     * và không lọc theo trạng thái nào, nên "có bản dịch" chính là "khách đọc
     * được". Đó cũng là lý do hàng đợi dịch chưa nhận buổi thuyết trình
     * (docs/12 mục 10).
     */
    private Map<String, ContentLocaleState> lectureLocales(UUID id) {
        return localeStatus("""
                SELECT l.code, CASE WHEN lt.lecture_id IS NULL THEN NULL ELSE 'PUBLISHED' END
                FROM locale l
                LEFT JOIN lecture_translation lt
                  ON lt.lecture_id = ? AND lt.locale = l.code AND NOT lt.soft_delete
                ORDER BY l.code
                """, id);
    }

    public Optional<LectureDetailView> findLecture(UUID id, String sourceLocale) {
        List<LectureDetailView> tim = jdbc.query("""
                SELECT l.id, l.market, l.event_date, l.start_time, l.city, l.venue,
                       l.seats, l.seats_taken, l.last_modified_at, s.display_name AS nguoi_sua
                FROM lecture l
                LEFT JOIN staff_user s ON s.id = l.last_modified_by
                WHERE l.id = ? AND NOT l.soft_delete
                """,
                (rs, i) -> new LectureDetailView(
                        rs.getObject("id", UUID.class),
                        rs.getString("market"),
                        ngay(rs.getDate("event_date")),
                        gio(rs.getTime("start_time")),
                        rs.getString("city"),
                        rs.getString("venue"),
                        rs.getInt("seats"),
                        rs.getInt("seats_taken"),
                        lectureTranslations(id, sourceLocale),
                        rs.getObject("last_modified_at", OffsetDateTime.class),
                        rs.getString("nguoi_sua")),
                id);

        return tim.stream().findFirst();
    }

    private List<LectureTranslationView> lectureTranslations(UUID id, String sourceLocale) {
        return jdbc.query("""
                SELECT lt.locale, lt.title, lt.description,
                       lt.last_modified_at, s.display_name AS nguoi_sua
                FROM lecture_translation lt
                LEFT JOIN staff_user s ON s.id = lt.last_modified_by
                WHERE lt.lecture_id = ? AND NOT lt.soft_delete
                ORDER BY lt.locale
                """,
                (rs, i) -> new LectureTranslationView(
                        rs.getString("locale"),
                        rs.getString("title"),
                        rs.getString("description"),
                        sourceLocale.equals(rs.getString("locale")),
                        rs.getObject("last_modified_at", OffsetDateTime.class),
                        rs.getString("nguoi_sua")),
                id);
    }

    public int seatsTaken(UUID id) {
        Integer count = jdbc.queryForObject(
                "SELECT seats_taken FROM lecture WHERE id = ?", Integer.class, id);
        return count == null ? 0 : count;
    }

    public UUID createLecture(LectureCreateInput input, String sourceLocale, UUID staffUserId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO lecture (id, market, event_date, start_time, city, venue,
                                     seats, seats_taken, created_by, last_modified_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """,
                id, input.market(), Date.valueOf(input.eventDate()),
                input.startTime() == null ? null : Time.valueOf(input.startTime()),
                input.city(), input.venue(), input.seats(), staffUserId, staffUserId);

        saveLectureTranslation(id, sourceLocale, input.translation(), staffUserId);
        return id;
    }

    public void patchLecture(UUID id, LecturePatchInput input, UUID staffUserId) {
        jdbc.update("""
                UPDATE lecture
                SET event_date = COALESCE(?, event_date),
                    start_time = COALESCE(?, start_time),
                    city = COALESCE(?, city),
                    venue = COALESCE(?, venue),
                    seats = COALESCE(?, seats),
                    last_modified_by = ?
                WHERE id = ?
                """,
                input.eventDate() == null ? null : Date.valueOf(input.eventDate()),
                input.startTime() == null ? null : Time.valueOf(input.startTime()),
                input.city(), input.venue(), input.seats(), staffUserId, id);
    }

    public void softDeleteLecture(UUID id, UUID staffUserId) {
        jdbc.update("UPDATE lecture SET soft_delete = TRUE, last_modified_by = ? WHERE id = ?",
                staffUserId, id);
        jdbc.update("""
                UPDATE lecture_translation SET soft_delete = TRUE, last_modified_by = ?
                WHERE lecture_id = ?
                """, staffUserId, id);
    }

    public void saveLectureTranslation(UUID id, String locale, LectureTranslationInput input,
                                       UUID staffUserId) {
        jdbc.update("""
                INSERT INTO lecture_translation
                       (lecture_id, locale, title, description, created_by, last_modified_by)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (lecture_id, locale) DO UPDATE
                SET title = EXCLUDED.title,
                    description = EXCLUDED.description,
                    soft_delete = FALSE,
                    last_modified_by = EXCLUDED.last_modified_by
                """, id, locale, input.title(), input.description(), staffUserId, staffUserId);
    }

    // ==================================================== dùng chung

    /**
     * Trạng thái từng locale, <b>đủ mọi locale đang bật</b>.
     *
     * <p>{@code LEFT JOIN locale} chứ không gộp từ bảng dịch: gộp thì locale
     * chưa có dòng nào sẽ vắng mặt khỏi kết quả, và màn hình mất đúng thông tin
     * quan trọng nhất — bài này <b>chưa</b> có bản tiếng Việt.
     */
    private Map<String, ContentLocaleState> localeStatus(String sql, UUID id) {
        Map<String, ContentLocaleState> ket_qua = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            String status = rs.getString(2);
            ket_qua.put(rs.getString(1), status == null
                    ? ContentLocaleState.MISSING
                    : ContentLocaleState.valueOf(status));
        }, id);
        return ket_qua;
    }

    private static List<String> mangChu(ResultSet rs, String column) throws SQLException {
        java.sql.Array array = rs.getArray(column);
        if (array == null) {
            return List.of();
        }
        return List.of((String[]) array.getArray());
    }

    private static LocalDate ngay(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    private static LocalTime gio(Time t) {
        return t == null ? null : t.toLocalTime();
    }
}
