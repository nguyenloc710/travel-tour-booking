package vn.travel.booking.quote.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.util.SecurityUtils;
import vn.travel.booking.quote.dto.AdminQuoteQuery;
import vn.travel.booking.quote.dto.AdminQuoteRow;
import vn.travel.booking.quote.dto.QuoteLineDraft;
import vn.travel.booking.quote.mapper.AdminQuoteMapper;
import vn.travel.booking.quote.service.AdminQuoteService;
import vn.travel.booking.web.generated.model.AdminQuoteDetail;
import vn.travel.booking.web.generated.model.AdminQuoteFilter;
import vn.travel.booking.web.generated.model.AdminQuoteLinesInput;
import vn.travel.booking.web.generated.model.AdminQuotePage;
import vn.travel.booking.web.generated.model.AdminQuoteStatusChange;

import java.math.BigDecimal;

import static vn.travel.booking.common.util.AdminResponses.noCache;

/**
 * Báo giá cho tour riêng — M8 của {@code docs/22}, máy trạng thái ở
 * {@code docs/14} mục 7.
 *
 * <p>Quyền: {@code CONSULTANT} và {@code ADMIN}. {@code EDITOR} và
 * {@code TRANSLATOR} là "–" — báo giá mang tên, điện thoại và yêu cầu riêng của
 * khách, và người viết nội dung không có việc gì với dữ liệu đó ({@code docs/31}).
 *
 * <p>Cả <b>bốn</b> endpoint cùng một vai trò, kể cả đường đọc: khác đơn đặt ở
 * chỗ ma trận không tách "xem báo giá" khỏi "dựng báo giá" thành hai dòng, nên ở
 * đây không có hai câu hỏi để trả lời khác nhau.
 */
@RestController
@Validated
public class AdminQuoteController {

    private final AdminQuoteService quotes;
    private final AdminQuoteMapper mapper;

    public AdminQuoteController(AdminQuoteService quotes, AdminQuoteMapper mapper) {
        this.quotes = quotes;
        this.mapper = mapper;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api/v1/admin/quotes",
            produces = {"application/json"}
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminQuotePage> danhSachBaoGia(
            @Valid @RequestParam(value = "status", required = false, defaultValue = "DRAFT") AdminQuoteFilter status,
            @Valid @RequestParam(value = "market", required = false) @Nullable String market,
            @Size(max = 120) @Valid @RequestParam(value = "q", required = false) @Nullable String q,
            @Min(0) @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(100) @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        PagedResult<AdminQuoteRow> result = quotes.list(new AdminQuoteQuery(
                status == null ? null : status.getValue(), market, q, page, size));

        return noCache().body(new AdminQuotePage(
                result.items().stream().map(mapper::toSummary).toList(),
                result.page(), result.size(), result.totalItems(), result.totalPages()));
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api/v1/admin/quotes/{reference}",
            produces = {"application/json"}
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminQuoteDetail> chiTietBaoGia(
            @Size(max = 20) @PathVariable("reference") String reference
    ) {
        return noCache().body(mapper.toDetail(quotes.detail(reference)));
    }

    /**
     * Dựng bảng giá — {@code docs/14} mục 7 quy tắc 5 và 6.
     *
     * <p>{@code total} <b>không có trong thân yêu cầu</b> và đó là chủ ý: máy
     * chủ cộng từ các dòng. Nhận tổng rồi tin là mở đường cho một báo giá mà
     * tổng không khớp bảng, và đó đúng là thứ khách mang ra tranh cãi.
     */
    @RequestMapping(
            method = RequestMethod.PUT,
            value = "/api/v1/admin/quotes/{reference}/lines",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminQuoteDetail> dungBangGiaBaoGia(
            @Size(max = 20) @PathVariable("reference") String reference,
            @Valid @RequestBody AdminQuoteLinesInput input
    ) {
        return noCache().body(mapper.toDetail(quotes.setQuoteLines(
                reference,
                input.getCurrency(),
                input.getLines().stream()
                        .map(line -> new QuoteLineDraft(
                                line.getLabelKey(),
                                line.getQuantity() == null ? null : new BigDecimal(line.getQuantity()),
                                line.getUnitAmount() == null ? null : new BigDecimal(line.getUnitAmount()),
                                new BigDecimal(line.getAmount())))
                        .toList(),
                SecurityUtils.currentStaff().id())));
    }

    /**
     * Gửi báo giá, hoặc ghi nhận khách đã trả lời — máy trạng thái
     * {@code docs/14} mục 7.
     *
     * <p>{@code AdminQuoteTargetStatus} hẹp hơn {@code QuoteStatus} (ba giá trị
     * thay vì năm), nên phép chuyển sang enum của domain luôn thành công.
     * {@code DRAFT} không nằm trong đó vì nó là điểm xuất phát; {@code EXPIRED}
     * không nằm trong đó vì nó là kết luận của đồng hồ, không phải quyết định
     * của người.
     */
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/api/v1/admin/quotes/{reference}/status",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminQuoteDetail> doiTrangThaiBaoGia(
            @Size(max = 20) @PathVariable("reference") String reference,
            @Valid @RequestBody AdminQuoteStatusChange input
    ) {
        return noCache().body(mapper.toDetail(quotes.changeStatus(
                reference,
                vn.travel.booking.quote.dto.QuoteStatus.valueOf(input.getToStatus().getValue()),
                SecurityUtils.currentStaff().id())));
    }
}
