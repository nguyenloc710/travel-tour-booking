package vn.travel.booking.quote.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.common.exception.QuoteErrors;
import vn.travel.booking.quote.dto.AdminQuoteDetailView;
import vn.travel.booking.quote.dto.AdminQuoteQuery;
import vn.travel.booking.quote.dto.AdminQuoteRow;
import vn.travel.booking.quote.dto.QuoteLineDraft;
import vn.travel.booking.quote.dto.QuoteStatus;
import vn.travel.booking.quote.repository.QuoteRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Màn hình báo giá của trang quản trị — docs/22 M8.
 *
 * <p>Ma trận quyền docs/22 mục 2.1, dòng "Báo giá: dựng, gửi": {@code CONSULTANT}
 * W, {@code ADMIN} W. {@code EDITOR} và {@code TRANSLATOR} không thấy màn hình
 * này — báo giá mang tên, điện thoại và yêu cầu riêng của khách.
 */
@Service
public class AdminQuoteService {

    private final QuoteRepository baoGia;
    private final Clock dongHo;

    public AdminQuoteService(QuoteRepository baoGia, Clock dongHo) {
        this.baoGia = baoGia;
        this.dongHo = dongHo;
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminQuoteRow> danhSach(AdminQuoteQuery query) {
        return baoGia.danhSach(query);
    }

    @Transactional(readOnly = true)
    public AdminQuoteDetailView chiTiet(String reference) {
        return baoGia.timTheoMa(reference)
                .orElseThrow(() -> new NotFoundException("không có báo giá nào mang mã " + reference));
    }

    /**
     * Dựng bảng giá — docs/14 mục 7 quy tắc 5 và 6.
     *
     * <p>Hai lần từ chối trước khi ghi:
     *
     * <ol>
     *   <li><b>Sai tiền tệ so với thị trường của báo giá.</b> Đây không phải
     *       chuyện gõ nhầm ba chữ cái: báo giá thị trường {@code VN} ghi bằng
     *       {@code DKK} là một lần quy đổi tỷ giá đi vào hệ thống bằng cửa sau,
     *       và dự án này <b>không có tỷ giá ở đâu cả</b> (CLAUDE.md điều 4).
     *   <li><b>Báo giá đã gửi.</b> Bảng giá lúc đó là thứ khách đang cầm trong
     *       tay — sửa sau lưng khách là thứ không giải thích được khi hai bên
     *       mang hai bản ra đối chiếu.
     * </ol>
     */
    @Transactional
    public AdminQuoteDetailView datBangGia(String reference, String currency,
                                           List<QuoteLineDraft> dong, UUID nhanVienId) {

        QuoteRepository.BaoGiaDeDoi hienTai = khoa(reference);

        if (!hienTai.currency().equalsIgnoreCase(currency)) {
            throw new QuoteErrors.CurrencyMismatch("báo giá thị trường " + hienTai.market()
                    + " phải ghi bằng " + hienTai.currency() + ", không phải " + currency);
        }
        if (!QuoteStatuses.suaBangGiaDuoc(hienTai.status())) {
            throw new QuoteErrors.NotAcceptable(hienTai.status(),
                    "chỉ sửa được bảng giá khi báo giá còn ở DRAFT");
        }

        baoGia.datBangGia(hienTai.id(), hienTai.currency(), dong, nhanVienId);

        return chiTiet(reference);
    }

    /**
     * Đổi trạng thái — docs/14 mục 7.
     *
     * <p>Ba luật, và cả ba nằm ở đây chứ không ở controller:
     *
     * <ol>
     *   <li><b>Không gửi báo giá rỗng.</b> Một email báo giá không có dòng nào
     *       là một lần làm khách mất thời gian, và tư vấn viên không nhận ra vì
     *       màn hình của họ vẫn hiện đủ thông tin yêu cầu.
     *   <li><b>{@code SENT} tính {@code validUntil} = hôm nay +
     *       {@code quoteValidDays}</b> (quy tắc 2). Máy chủ tính, không nhận từ
     *       client.
     *   <li><b>Quá hạn thì không {@code ACCEPTED} được</b> (quy tắc 4) — khách
     *       phải yêu cầu lại, không tự gia hạn. Kiểm bằng đồng hồ được tiêm, nên
     *       test cố định được ngày.
     * </ol>
     */
    @Transactional
    public AdminQuoteDetailView doiTrangThai(String reference, QuoteStatus sang, UUID nhanVienId) {
        QuoteRepository.BaoGiaDeDoi hienTai = khoa(reference);

        QuoteStatuses.phaiDiDuoc(hienTai.status(), sang);

        OffsetDateTime sentAt = null;
        LocalDate validUntil = null;

        if (sang == QuoteStatus.SENT) {
            if (hienTai.soDong() == 0) {
                throw new QuoteErrors.NotAcceptable(hienTai.status(),
                        "chưa có dòng giá nào để gửi");
            }
            sentAt = OffsetDateTime.now(dongHo);
            validUntil = LocalDate.now(dongHo).plusDays(hienTai.quoteValidDays());
        }

        if (sang == QuoteStatus.ACCEPTED
                && hienTai.validUntil() != null
                && hienTai.validUntil().isBefore(LocalDate.now(dongHo))) {
            throw new QuoteErrors.QuoteExpired(
                    "báo giá " + reference + " hết hạn ngày " + hienTai.validUntil());
        }

        baoGia.datTrangThai(hienTai.id(), sang, nhanVienId, sentAt, validUntil);

        return chiTiet(reference);
    }

    private QuoteRepository.BaoGiaDeDoi khoa(String reference) {
        return baoGia.khoaBaoGia(reference)
                .orElseThrow(() -> new NotFoundException("không có báo giá nào mang mã " + reference));
    }
}
