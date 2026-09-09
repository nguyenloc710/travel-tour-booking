package vn.travel.booking.pricing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.web.generated.model.PriceBreakdown;
import vn.travel.booking.web.generated.model.PriceLine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bảng phân rã giá sang DTO sinh từ spec bằng MapStruct.
 */
@Mapper(componentModel = "spring")
public interface PricingMapper {

    PricingMapper INSTANCE = Mappers.getMapper(PricingMapper.class);

    @Mapping(target = "lines", source = "lines")
    @Mapping(target = "total", source = "total")
    @Mapping(target = "deposit", source = "deposit")
    @Mapping(target = "balance", source = "balance")
    PriceBreakdown toPriceBreakdown(vn.travel.booking.pricing.dto.PriceBreakdown b);

    List<PriceLine> toPriceLines(List<vn.travel.booking.pricing.dto.PriceLine> row);

    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "quantity", expression = "java(toQuantityText(d.quantity()))")
    @Mapping(target = "unitAmount", source = "unitAmount")
    @Mapping(target = "amount", source = "amount")
    PriceLine toPriceLine(vn.travel.booking.pricing.dto.PriceLine d);

    default PriceLine.KindEnum toKindEnum(vn.travel.booking.pricing.dto.PriceLineKind kind) {
        return kind == null ? null : PriceLine.KindEnum.fromValue(kind.name());
    }

    default vn.travel.booking.web.generated.model.Money toMoney(Money money) {
        return RefMapper.toMoney(money);
    }

    default String toQuantityText(BigDecimal quantity) {
        return quantity == null ? null : quantity.toPlainString();
    }
}
