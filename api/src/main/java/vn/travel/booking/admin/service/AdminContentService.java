package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.DestinationDetailView;
import vn.travel.booking.admin.dto.DestinationTranslationInput;
import vn.travel.booking.admin.dto.LectureCreateInput;
import vn.travel.booking.admin.dto.LectureDetailView;
import vn.travel.booking.admin.dto.LecturePatchInput;
import vn.travel.booking.admin.dto.LectureRow;
import vn.travel.booking.admin.dto.LectureTranslationInput;
import vn.travel.booking.admin.dto.PostCreateInput;
import vn.travel.booking.admin.dto.PostDetailView;
import vn.travel.booking.admin.dto.PostPatchInput;
import vn.travel.booking.admin.dto.PostRow;
import vn.travel.booking.admin.dto.PostTranslationInput;
import vn.travel.booking.admin.dto.TagView;
import vn.travel.booking.admin.repository.AdminContentRepository;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.ForbiddenException;
import vn.travel.booking.common.exception.NotFoundException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Nội dung khác — điểm đến, bài viết, buổi thuyết trình (docs/22 M13).
 *
 * <p>Ba loại, một luật chung và vài luật riêng.
 *
 * <p><b>Luật chung: ai sửa được locale nào.</b> Giống hệt bản dịch sản phẩm —
 * {@code EDITOR} sửa bản nguồn, {@code TRANSLATOR} sửa bản dịch, {@code ADMIN}
 * sửa cả hai (docs/22 mục 2.1 điều 2). Luật này <b>không</b> diễn đạt được bằng
 * {@code @PreAuthorize} vì nó phụ thuộc cả vai trò lẫn locale đang sửa, nên nó
 * nằm ở đây; controller vẫn giữ phép kiểm "ai được vào cửa".
 *
 * <p>Ma trận cho `EDITOR` và `TRANSLATOR` quyền `W` với nội dung khác, khác hẳn
 * với sản phẩm nơi `TRANSLATOR` chỉ đọc bản nguồn — nhưng luật locale thì giống
 * nhau, và đó là điều quan trọng: một người dịch sửa bản `da` là làm hai bản
 * lệch nhau mà không ai biết bản nào đúng.
 */
@Service
public class AdminContentService {

    private final AdminContentRepository noiDung;

    public AdminContentService(AdminContentRepository noiDung) {
        this.noiDung = noiDung;
    }

    // ==================================================== điểm đến

    @Transactional(readOnly = true)
    public DestinationDetailView diemDen(UUID id, String sourceLocale) {
        return noiDung.findDestination(id, sourceLocale)
                .orElseThrow(() -> new NotFoundException("destination id=" + id));
    }

    @Transactional
    public DestinationDetailView suaDiemDen(UUID id, UUID regionId, Integer sortOrder,
                                            String sourceLocale, UUID nhanVienId) {
        phaiCoDiemDen(id);
        noiDung.patchDestination(id, regionId, sortOrder, nhanVienId);
        return diemDen(id, sourceLocale);
    }

    /**
     * Xoá mềm một điểm đến.
     *
     * <p>Từ chối khi còn sản phẩm trỏ tới. Cứ cho xoá thì <b>không có gì nổ</b>:
     * sản phẩm chỉ lặng lẽ biến mất khỏi listing, vì truy vấn của nó
     * {@code INNER JOIN destination_translation}. Đúng chính sách không-fallback
     * đang làm việc của nó, nên không có lỗi nào ghi ra và không ai nối được hai
     * sự việc với nhau.
     */
    @Transactional
    public void xoaDiemDen(UUID id, UUID nhanVienId) {
        phaiCoDiemDen(id);

        int soSanPham = noiDung.countProductsOfDestination(id);
        if (soSanPham > 0) {
            throw new AdminErrors.DestinationInUse(soSanPham);
        }
        noiDung.softDeleteDestination(id, nhanVienId);
    }

    @Transactional
    public DestinationDetailView luuBanDichDiemDen(UUID id, String locale, String sourceLocale,
                                                   Set<String> roles,
                                                   DestinationTranslationInput input,
                                                   UUID nhanVienId) {
        phaiCoDiemDen(id);
        phaiSuaDuocLocale(locale, sourceLocale, roles);

        noiDung.saveDestinationTranslation(id, locale, input, nhanVienId);
        return diemDen(id, sourceLocale);
    }

    // ==================================================== thẻ

    @Transactional(readOnly = true)
    public List<TagView> the(String sourceLocale) {
        return noiDung.tags(sourceLocale);
    }

    // ==================================================== bài viết

    @Transactional(readOnly = true)
    public PagedResult<PostRow> danhSachBaiViet(String q, int page, int size, String sourceLocale) {
        return noiDung.findPosts(q, page, size, sourceLocale);
    }

    @Transactional(readOnly = true)
    public PostDetailView baiViet(UUID id, String sourceLocale) {
        return noiDung.findPost(id, sourceLocale)
                .orElseThrow(() -> new NotFoundException("post id=" + id));
    }

    /**
     * Tạo bài viết, kèm luôn bản ngôn ngữ nguồn.
     *
     * <p>Người tạo phải sửa được <b>bản nguồn</b>, không phải bản dịch: bài viết
     * mới luôn bắt đầu bằng bản {@code da} (ADR-004). Người dịch tạo bài mới
     * nghĩa là có một bài chỉ tồn tại ở tiếng Việt, và không có gì để dịch ngược.
     */
    @Transactional
    public PostDetailView taoBaiViet(PostCreateInput input, String sourceLocale,
                                     Set<String> roles, UUID nhanVienId) {
        phaiSuaDuocLocale(sourceLocale, sourceLocale, roles);

        UUID id = noiDung.createPost(input, sourceLocale, nhanVienId);
        return baiViet(id, sourceLocale);
    }

    @Transactional
    public PostDetailView suaBaiViet(UUID id, PostPatchInput input, String sourceLocale,
                                     UUID nhanVienId) {
        phaiCoBaiViet(id);
        noiDung.patchPost(id, input, nhanVienId);
        return baiViet(id, sourceLocale);
    }

    @Transactional
    public PostDetailView datTheChoBaiViet(UUID id, List<UUID> tagIds, String sourceLocale,
                                           UUID nhanVienId) {
        phaiCoBaiViet(id);
        noiDung.datTheChoBaiViet(id, tagIds);
        // Gán thẻ vẫn là một lần sửa bài viết, nên nó phải đứng tên ai đó: bảng
        // nối `post_tag` không có cột kiểm toán (nhóm C), nên dấu vết duy nhất
        // là `last_modified_by` của chính bài viết.
        noiDung.patchPost(id, new PostPatchInput(null, null), nhanVienId);
        return baiViet(id, sourceLocale);
    }

    @Transactional
    public void xoaBaiViet(UUID id, UUID nhanVienId) {
        phaiCoBaiViet(id);
        noiDung.softDeletePost(id, nhanVienId);
    }

    @Transactional
    public PostDetailView luuBanDichBaiViet(UUID id, String locale, String sourceLocale,
                                            Set<String> roles, PostTranslationInput input,
                                            UUID nhanVienId) {
        phaiCoBaiViet(id);
        phaiSuaDuocLocale(locale, sourceLocale, roles);

        noiDung.savePostTranslation(id, locale, input, nhanVienId);
        return baiViet(id, sourceLocale);
    }

    // ==================================================== buổi thuyết trình

    @Transactional(readOnly = true)
    public PagedResult<LectureRow> danhSachSuKien(int page, int size, String sourceLocale) {
        return noiDung.findLectures(page, size, sourceLocale);
    }

    @Transactional(readOnly = true)
    public LectureDetailView suKien(UUID id, String sourceLocale) {
        return noiDung.findLecture(id, sourceLocale)
                .orElseThrow(() -> new NotFoundException("lecture id=" + id));
    }

    @Transactional
    public LectureDetailView taoSuKien(LectureCreateInput input, String sourceLocale,
                                       Set<String> roles, UUID nhanVienId) {
        phaiSuaDuocLocale(sourceLocale, sourceLocale, roles);

        UUID id = noiDung.createLecture(input, sourceLocale, nhanVienId);
        return suKien(id, sourceLocale);
    }

    /**
     * Sửa buổi thuyết trình.
     *
     * <p>Hạ {@code seats} xuống dưới số người đã đăng ký là lấy lại chỗ đã hứa —
     * cùng câu chuyện với sức chứa ngày khởi hành, nên cùng mã lỗi
     * {@code CAPACITY_BELOW_BOOKED}. Một mã lỗi cho một tình huống nghiệp vụ,
     * không phải một mã cho một bảng.
     */
    @Transactional
    public LectureDetailView suaSuKien(UUID id, LecturePatchInput input, String sourceLocale,
                                       UUID nhanVienId) {
        phaiCoSuKien(id);

        if (input.seats() != null) {
            int daDangKy = noiDung.seatsTaken(id);
            if (input.seats() < daDangKy) {
                throw new AdminErrors.CapacityBelowBooked(input.seats(), daDangKy);
            }
        }

        noiDung.patchLecture(id, input, nhanVienId);
        return suKien(id, sourceLocale);
    }

    @Transactional
    public void xoaSuKien(UUID id, UUID nhanVienId) {
        phaiCoSuKien(id);
        noiDung.softDeleteLecture(id, nhanVienId);
    }

    @Transactional
    public LectureDetailView luuBanDichSuKien(UUID id, String locale, String sourceLocale,
                                              Set<String> roles, LectureTranslationInput input,
                                              UUID nhanVienId) {
        phaiCoSuKien(id);
        phaiSuaDuocLocale(locale, sourceLocale, roles);

        noiDung.saveLectureTranslation(id, locale, input, nhanVienId);
        return suKien(id, sourceLocale);
    }

    // ==================================================== nội bộ

    /**
     * Luật quyền phụ thuộc locale — docs/22 mục 2.1 điều 2.
     *
     * <p>Trùng nguyên văn với {@code AdminProductTranslationService}, và ở lại
     * hai chỗ có chủ ý: gộp thành một lớp dùng chung nghĩa là đổi luật cho nội
     * dung sẽ âm thầm đổi luật cho sản phẩm. Hai bề mặt, hai dòng khác nhau
     * trong ma trận, hai chỗ kiểm — ngày nào đó chúng sẽ trả lời khác nhau.
     */
    private static void phaiSuaDuocLocale(String locale, String sourceLocale, Set<String> roles) {
        boolean laNguon = sourceLocale.equals(locale);
        boolean duocPhep = roles.contains("ADMIN")
                || (laNguon ? roles.contains("EDITOR") : roles.contains("TRANSLATOR"));

        if (!duocPhep) {
            throw new ForbiddenException(laNguon
                    ? "chỉ EDITOR hoặc ADMIN sửa được bản ngôn ngữ nguồn"
                    : "chỉ TRANSLATOR hoặc ADMIN sửa được bản dịch");
        }
    }

    private void phaiCoDiemDen(UUID id) {
        if (!noiDung.destinationExists(id)) {
            throw new NotFoundException("destination id=" + id);
        }
    }

    private void phaiCoBaiViet(UUID id) {
        if (!noiDung.postExists(id)) {
            throw new NotFoundException("post id=" + id);
        }
    }

    private void phaiCoSuKien(UUID id) {
        if (!noiDung.lectureExists(id)) {
            throw new NotFoundException("lecture id=" + id);
        }
    }
}
