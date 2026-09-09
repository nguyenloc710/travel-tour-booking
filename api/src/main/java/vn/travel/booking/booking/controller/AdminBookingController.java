package vn.travel.booking.booking.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
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
import vn.travel.booking.admin.dto.AdminBookingQuery;
import vn.travel.booking.admin.dto.AdminBookingRow;
import vn.travel.booking.admin.service.AdminBookingService;
import vn.travel.booking.booking.mapper.AdminBookingMapper;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.util.SecurityUtils;
import vn.travel.booking.web.generated.model.AdminBookingDetail;
import vn.travel.booking.web.generated.model.AdminBookingPage;
import vn.travel.booking.web.generated.model.AdminBookingScope;
import vn.travel.booking.web.generated.model.AdminBookingStatusChange;
import vn.travel.booking.web.generated.model.BookingStatus;

import java.time.LocalDate;

import static vn.travel.booking.common.util.AdminResponses.noCache;

/**
 * Vận hành đơn đặt — M6 và M7 của {@code docs/22}.
 *
 * <p>Quyền theo ma trận {@code docs/22} mục 2.1: {@code CONSULTANT} và
 * {@code ADMIN}. Biên tập viên <b>không</b> xem được đơn — họ viết nội dung, và
 * đơn có tên, email, số hộ chiếu của khách thật.
 *
 * <p>Đường đọc và đường ghi khai quyền riêng chứ không dùng chung một hằng: hai
 * dòng khác nhau trong ma trận là hai câu hỏi khác nhau, và ngày nào đó chúng sẽ
 * trả lời khác nhau.
 */
@RestController
@Validated
public class AdminBookingController {

    private final AdminBookingService bookings;
    private final AdminBookingMapper mapper;

    public AdminBookingController(AdminBookingService bookings, AdminBookingMapper mapper) {
        this.bookings = bookings;
        this.mapper = mapper;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api/v1/admin/bookings",
            produces = {"application/json"}
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminBookingPage> listBookings(
            @Valid @RequestParam(value = "scope", required = false, defaultValue = "NEEDS_ACTION") AdminBookingScope scope,
            @Valid @RequestParam(value = "status", required = false) @Nullable BookingStatus status,
            @Valid @RequestParam(value = "market", required = false) @Nullable String market,
            @Valid @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @Valid @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
            @Size(max = 120) @Valid @RequestParam(value = "q", required = false) @Nullable String q,
            @Min(0) @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Min(1) @Max(100) @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        PagedResult<AdminBookingRow> result = bookings.list(new AdminBookingQuery(
                scope == null ? null : scope.getValue(),
                status == null ? null : status.getValue(),
                market, from, to, q, page, size));

        return noCache().body(new AdminBookingPage(
                result.items().stream().map(mapper::toSummary).toList(),
                result.page(), result.size(), result.totalItems(), result.totalPages()));
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api/v1/admin/bookings/{reference}",
            produces = {"application/json"}
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminBookingDetail> getBookingDetail(
            @Size(max = 20) @PathVariable("reference") String reference
    ) {
        return noCache().body(mapper.toDetail(bookings.detail(reference)));
    }

    /**
     * Đường <b>ghi</b> của M7 — dòng "Đơn đặt: đổi trạng thái, huỷ, hoàn" của ma
     * trận {@code docs/22} mục 2.1.
     *
     * <p>{@code AdminBookingTargetStatus} hẹp hơn {@code BookingStatus} nên phép
     * chuyển qua enum của domain luôn thành công — bốn giá trị của nó là tập con
     * đúng nghĩa, và cả hai đều sinh từ cùng một spec.
     */
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/api/v1/admin/bookings/{reference}/status",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<AdminBookingDetail> changeBookingStatus(
            @Size(max = 20) @PathVariable("reference") String reference,
            @Valid @RequestBody AdminBookingStatusChange input
    ) {
        return noCache().body(mapper.toDetail(bookings.changeStatus(
                reference,
                vn.travel.booking.booking.dto.BookingStatus.valueOf(input.getToStatus().getValue()),
                SecurityUtils.currentStaff().id(),
                input.getNote())));
    }
}
