package vn.travel.booking.web;

import vn.travel.booking.web.generated.model.PriceBreakdown;
import vn.travel.booking.web.generated.model.PriceLine;

import java.util.List;

/**
 * Bảng phân rã giá sang DTO sinh từ spec.
 *
 * <p>{@code quantity} đi ra dưới dạng <b>chuỗi</b>, cùng lý do với tiền: số dấu
 * phẩy động của JavaScript làm hỏng cả hai.
 */
final class PricingMapper {

    private PricingMapper() {
    }

    static PriceBreakdown sangBang(vn.travel.booking.domain.pricing.PriceBreakdown b) {
        return new PriceBreakdown(
                sangDong(b.lines()),
                ProductMapper.sangTien(b.total()),
                ProductMapper.sangTien(b.deposit()),
                ProductMapper.sangTien(b.balance()));
    }

    static List<PriceLine> sangDong(List<vn.travel.booking.domain.pricing.PriceLine> dong) {
        return dong.stream()
                .map(d -> new PriceLine(
                        PriceLine.KindEnum.fromValue(d.kind().name()),
                        d.labelKey(),
                        ProductMapper.sangTien(d.amount()))
                        .quantity(d.quantity() == null ? null : d.quantity().toPlainString())
                        .unitAmount(ProductMapper.sangTien(d.unitAmount())))
                .toList();
    }
}
