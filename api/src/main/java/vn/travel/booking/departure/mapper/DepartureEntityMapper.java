package vn.travel.booking.departure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.DeparturePriceView;
import vn.travel.booking.admin.dto.DepartureView;
import vn.travel.booking.departure.entity.DepartureEntity;
import vn.travel.booking.departure.entity.DeparturePriceEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DepartureEntityMapper {

    @Mapping(target = "id", source = "e.id")
    @Mapping(target = "market", source = "e.market")
    @Mapping(target = "departDate", source = "e.departDate")
    @Mapping(target = "returnDate", source = "e.returnDate")
    @Mapping(target = "days", source = "e.days")
    @Mapping(target = "cabinCategory", source = "e.cabinCategory")
    @Mapping(target = "baseStatus", source = "e.baseStatus")
    @Mapping(target = "capacity", source = "e.capacity")
    @Mapping(target = "seatsBooked", source = "e.seatsBooked")
    @Mapping(target = "departureOriginId", source = "e.departureOriginId")
    @Mapping(target = "prices", source = "prices")
    DepartureView toView(DepartureEntity e, List<DeparturePriceView> prices);

    @Mapping(target = "paxTypeCode", source = "paxTypeCode")
    @Mapping(target = "occupancy", source = "entity.occupancy")
    @Mapping(target = "amount", expression = "java(new vn.travel.booking.common.money.Money(entity.getAmount(), entity.getCurrency()))")
    DeparturePriceView toPriceView(DeparturePriceEntity entity, String paxTypeCode);
}
