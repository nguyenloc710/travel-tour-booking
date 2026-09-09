package vn.travel.booking.pricing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.PriceTierView;
import vn.travel.booking.pricing.entity.PriceTierEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PriceTierEntityMapper {

    @Mapping(target = "pricePerPerson", expression = "java(new vn.travel.booking.common.money.Money(e.getPricePerPerson(), e.getCurrency()))")
    PriceTierView toView(PriceTierEntity e);

    List<PriceTierView> toViewList(List<PriceTierEntity> list);
}
