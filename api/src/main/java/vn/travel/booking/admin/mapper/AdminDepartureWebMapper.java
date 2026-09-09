package vn.travel.booking.admin.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.DeparturePriceView;
import vn.travel.booking.admin.dto.DepartureView;
import vn.travel.booking.admin.dto.PriceTierView;
import vn.travel.booking.web.generated.model.AdminDeparture;
import vn.travel.booking.web.generated.model.AdminDeparturePrice;
import vn.travel.booking.web.generated.model.AdminPriceTier;
import vn.travel.booking.web.generated.model.Money;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminDepartureWebMapper {

    @Mapping(target = "market", source = "market")
    @Mapping(target = "days", source = "days")
    @Mapping(target = "capacity", source = "capacity")
    @Mapping(target = "seatsBooked", source = "seatsBooked")
    @Mapping(target = "prices", source = "prices")
    AdminDeparture toDeparture(DepartureView d);

    List<AdminDeparture> toDepartureList(List<DepartureView> departures);

    AdminDeparturePrice toDeparturePrice(DeparturePriceView g);

    List<AdminDeparturePrice> toDeparturePriceList(List<DeparturePriceView> prices);

    @Mapping(target = "market", source = "market")
    @Mapping(target = "minPax", source = "minPax")
    @Mapping(target = "maxPax", source = "maxPax")
    AdminPriceTier toPriceTier(PriceTierView t);

    List<AdminPriceTier> toPriceTierList(List<PriceTierView> tiers);

    default AdminDeparture.MarketEnum toDepartureMarket(String market) {
        return market == null ? null : AdminDeparture.MarketEnum.fromValue(market);
    }

    default AdminPriceTier.MarketEnum toPriceTierMarket(String market) {
        return market == null ? null : AdminPriceTier.MarketEnum.fromValue(market);
    }

    default Integer toInteger(Short value) {
        return value == null ? null : value.intValue();
    }

    default Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    default Money toMoney(vn.travel.booking.common.money.Money m) {
        return m == null ? null : new Money(m.amount().toPlainString(), m.currency());
    }
}
