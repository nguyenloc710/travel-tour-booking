package vn.travel.booking.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.MarketState;
import vn.travel.booking.admin.dto.ProductTranslationView;
import vn.travel.booking.admin.dto.TranslationState;
import vn.travel.booking.product.entity.ProductMarketEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductEntityMapper {

    @Mapping(target = "outdated", expression = "java(Boolean.TRUE.equals(v.outdated()))")
    TranslationState toState(ProductTranslationView v);

    List<TranslationState> toStateList(List<ProductTranslationView> list);

    @Mapping(target = "market", source = "market")
    @Mapping(target = "published", source = "published")
    MarketState toMarketState(ProductMarketEntity pm);

    List<MarketState> toMarketStateList(List<ProductMarketEntity> list);
}
