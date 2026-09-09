package vn.travel.booking.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.product.dto.DepartureView;
import vn.travel.booking.product.dto.NamedRef;
import vn.travel.booking.web.generated.model.Departure;
import vn.travel.booking.web.generated.model.DepartureStatus;
import vn.travel.booking.web.generated.model.HotelStay;
import vn.travel.booking.web.generated.model.ItineraryDay;
import vn.travel.booking.web.generated.model.ProductDetail;
import vn.travel.booking.web.generated.model.ProductPage;
import vn.travel.booking.web.generated.model.ProductSort;
import vn.travel.booking.web.generated.model.Ref;

import java.util.List;

/**
 * Ánh xạ DTO của sản phẩm sang model spec của API công khai bằng MapStruct.
 */
@Mapper(componentModel = "spring")
public interface ProductWebMapper {

    @Mapping(target = "priceFrom", source = "priceFrom")
    @Mapping(target = "status", source = "status")
    Departure toDeparture(DepartureView d);

    List<Departure> toDepartureList(List<DepartureView> list);

    @Mapping(target = "destination", source = "destination")
    ItineraryDay toItineraryDay(vn.travel.booking.product.dto.ItineraryDay d);

    List<ItineraryDay> toItineraryDayList(List<vn.travel.booking.product.dto.ItineraryDay> list);

    @Mapping(target = "destination", source = "destination")
    HotelStay toHotelStay(vn.travel.booking.product.dto.HotelStay h);

    List<HotelStay> toHotelStayList(List<vn.travel.booking.product.dto.HotelStay> list);

    Ref toRef(NamedRef ref);

    default vn.travel.booking.web.generated.model.Money toMoney(Money money) {
        return RefMapper.toMoney(money);
    }

    default DepartureStatus toDepartureStatus(vn.travel.booking.departure.dto.DepartureStatus status) {
        return status == null ? null : DepartureStatus.fromValue(status.name());
    }

    default vn.travel.booking.product.dto.ProductSort toDomainSort(ProductSort sort) {
        ProductSort effective = sort == null ? ProductSort.TITLE_ASC : sort;
        return vn.travel.booking.product.dto.ProductSort.valueOf(effective.name());
    }

    default ProductPage toPage(PagedResult<vn.travel.booking.product.dto.ProductSummary> page) {
        return RefMapper.toProductPage(page);
    }

    default ProductDetail toDetail(vn.travel.booking.product.dto.ProductDetail d) {
        return RefMapper.toProductDetail(d);
    }
}
