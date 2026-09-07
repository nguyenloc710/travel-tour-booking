package vn.travel.booking.pricing.mapper;

import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.web.generated.model.PriceBreakdown;
import vn.travel.booking.web.generated.model.PriceLine;

import java.util.List;

/**
 * Bảng phân rã giá sang DTO sinh từ spec.
 *
 * <p>{@code quantity} đi ra dưới dạng <b>chuỗi</b>, cùng lý do với tiền: số dấu
 * phẩy động của JavaScript làm hỏng cả hai.
 */
public final class PricingMapper {

    private PricingMapper() {
    }

    public static PriceBreakdown sangBang(vn.travel.booking.pricing.dto.PriceBreakdown b) {
        return new PriceBreakdown(
                sangDong(b.lines()),
                RefMapper.sangTien(b.total()),
                RefMapper.sangTien(b.deposit()),
                RefMapper.sangTien(b.balance()));
    }

    public static List<PriceLine> sangDong(List<vn.travel.booking.pricing.dto.PriceLine> dong) {
        return dong.stream()
                .map(d -> new PriceLine(
                        PriceLine.KindEnum.fromValue(d.kind().name()),
                        d.labelKey(),
                        RefMapper.sangTien(d.amount()))
                        .quantity(d.quantity() == null ? null : d.quantity().toPlainString())
                        .unitAmount(RefMapper.sangTien(d.unitAmount())))
                .toList();
    }
}
