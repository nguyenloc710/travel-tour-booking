package vn.travel.booking.booking.mapper;

import org.mapstruct.Mapper;
import vn.travel.booking.admin.dto.AdminBookingDetailView;
import vn.travel.booking.admin.dto.AdminBookingRow;
import vn.travel.booking.admin.dto.BookingEventRow;
import vn.travel.booking.admin.dto.BookingPassengerRow;
import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.pricing.mapper.PricingMapper;
import vn.travel.booking.web.generated.model.AdminBookingDetail;
import vn.travel.booking.web.generated.model.AdminBookingEvent;
import vn.travel.booking.web.generated.model.AdminBookingPassenger;
import vn.travel.booking.web.generated.model.AdminBookingSummary;
import vn.travel.booking.web.generated.model.BookingStatus;

/**
 * Đơn đặt sang DTO của hợp đồng — M6 và M7 của {@code docs/22}.
 *
 * <p>Trước đây là năm phương thức {@code private static toView(...)} nạp chồng
 * nhau trong {@code AdminController}. Nạp chồng theo kiểu tham số khiến người
 * đọc phải tự dò xem lời gọi nào rơi vào bản nào, và thêm một trường vào bản ghi
 * nguồn thì không có gì nhắc rằng phép ánh xạ cần sửa theo.
 *
 * <p>MapStruct sinh mã lúc biên dịch và <b>cảnh báo cho mọi trường đích không
 * được ánh xạ</b>. Đó là toàn bộ lý do đổi: biến việc quên thành thứ nhìn thấy
 * được.
 */
@Mapper(componentModel = "spring")
public interface AdminBookingMapper {

    AdminBookingSummary toSummary(AdminBookingRow row);

    AdminBookingDetail toDetail(AdminBookingDetailView view);

    AdminBookingPassenger toPassenger(BookingPassengerRow row);

    AdminBookingEvent toEvent(BookingEventRow row);

    /**
     * Trạng thái của domain sang enum của hợp đồng.
     *
     * <p>Đi qua {@code name()} chứ không qua thứ tự khai báo: hai enum trùng tên
     * hằng nhưng thứ tự có thể lệch, và khớp theo thứ tự là loại lỗi im lặng
     * đúng cho tới ngày ai đó chèn một hằng vào giữa.
     */
    default BookingStatus toStatus(vn.travel.booking.booking.dto.BookingStatus status) {
        return status == null ? null : BookingStatus.fromValue(status.name());
    }

    /**
     * Hai enum thị trường riêng cho hai DTO. Chúng cùng tập giá trị nhưng là hai
     * kiểu khác nhau trong mã sinh ra, nên phải có hai phương thức — MapStruct
     * chọn theo kiểu đích.
     */
    default AdminBookingSummary.MarketEnum toSummaryMarket(String market) {
        return market == null ? null : AdminBookingSummary.MarketEnum.fromValue(market);
    }

    default AdminBookingDetail.MarketEnum toDetailMarket(String market) {
        return market == null ? null : AdminBookingDetail.MarketEnum.fromValue(market);
    }

    default AdminBookingEvent.ActorTypeEnum toActorType(String actorType) {
        return actorType == null ? null : AdminBookingEvent.ActorTypeEnum.fromValue(actorType);
    }

    /**
     * Tiền và bảng phân rã giá dùng lại hai lớp ánh xạ đã có, không viết lại ở
     * đây: {@code amount} là <b>chuỗi</b> trong JSON, và quy tắc đó chỉ đúng khi
     * nó nằm đúng một chỗ ({@code CLAUDE.md} quy tắc 6).
     */
    default vn.travel.booking.web.generated.model.Money toMoney(Money money) {
        return RefMapper.toMoney(money);
    }

    default vn.travel.booking.web.generated.model.PriceBreakdown toBreakdown(
            vn.travel.booking.pricing.dto.PriceBreakdown breakdown) {
        return PricingMapper.INSTANCE.toPriceBreakdown(breakdown);
    }
}
