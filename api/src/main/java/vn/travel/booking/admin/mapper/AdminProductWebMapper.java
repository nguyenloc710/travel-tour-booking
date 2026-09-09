package vn.travel.booking.admin.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.AdminProductRow;
import vn.travel.booking.admin.dto.MarketState;
import vn.travel.booking.admin.dto.ProductDetailView;
import vn.travel.booking.admin.dto.ProductTranslationInput;
import vn.travel.booking.admin.dto.ProductTranslationView;
import vn.travel.booking.admin.dto.TranslationState;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.product.dto.ProductTypeBlocks;
import vn.travel.booking.web.generated.model.AdminProductDetail;
import vn.travel.booking.web.generated.model.AdminProductMarketState;
import vn.travel.booking.web.generated.model.AdminProductPage;
import vn.travel.booking.web.generated.model.AdminProductSummary;
import vn.travel.booking.web.generated.model.AdminProductTranslation;
import vn.travel.booking.web.generated.model.AdminProductTranslationInput;
import vn.travel.booking.web.generated.model.AdminTranslationState;
import vn.travel.booking.web.generated.model.ComboFields;
import vn.travel.booking.web.generated.model.CruiseFields;
import vn.travel.booking.web.generated.model.DayTourFields;
import vn.travel.booking.web.generated.model.GroupTourFields;
import vn.travel.booking.web.generated.model.IndividualPackageFields;
import vn.travel.booking.web.generated.model.PrivateTourFields;
import vn.travel.booking.web.generated.model.ProductType;
import vn.travel.booking.web.generated.model.TranslationStatus;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminProductWebMapper {

    @Mapping(target = "productType", source = "productType")
    @Mapping(target = "sourceStatus", source = "sourceStatus")
    @Mapping(target = "markets", source = "markets")
    @Mapping(target = "translations", source = "translations")
    AdminProductSummary toSummary(AdminProductRow r);

    List<AdminProductSummary> toSummaryList(List<AdminProductRow> list);

    @Mapping(target = "market", source = "market")
    AdminProductMarketState toMarketState(MarketState m);

    List<AdminProductMarketState> toMarketStateList(List<MarketState> list);

    @Mapping(target = "status", source = "status")
    AdminTranslationState toTranslationState(TranslationState t);

    List<AdminTranslationState> toTranslationStateList(List<TranslationState> list);

    @Mapping(target = "status", source = "status")
    AdminProductTranslation toTranslation(ProductTranslationView v);

    List<AdminProductTranslation> toTranslationList(List<ProductTranslationView> list);

    @Mapping(target = "status", source = "status.value")
    ProductTranslationInput toTranslationInput(AdminProductTranslationInput i);

    default AdminProductPage toPage(PagedResult<AdminProductRow> result) {
        if (result == null) {
            return null;
        }
        return new AdminProductPage(
                toSummaryList(result.items()),
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages());
    }

    default AdminProductDetail toDetail(ProductDetailView v) {
        if (v == null) {
            return null;
        }
        AdminProductDetail detail = new AdminProductDetail(
                v.id(), ProductType.fromValue(v.productType()), v.primaryDestinationId(),
                v.heroImage(), v.isNew(), v.reviewCount(),
                toMarketStateList(v.markets()),
                toTranslationStateList(v.translations()))
                .durationDays(toInteger(v.durationDays()))
                .mapImage(v.mapImage())
                .layout(v.layout())
                .rating(v.rating() == null ? null : v.rating().doubleValue())
                .consultantId(v.consultantId())
                .lastModifiedAt(v.lastModifiedAt())
                .lastModifiedBy(v.lastModifiedBy());

        ProductTypeBlocks blocks = v.blocks();
        if (blocks != null) {
            if (blocks.groupTour() != null) {
                detail.setGroupTour(new GroupTourFields(toInteger(blocks.groupTour().minPax()),
                        toInteger(blocks.groupTour().maxPax()), toInteger(blocks.groupTour().guaranteedThreshold()),
                        GroupTourFields.TourLeaderLanguageEnum.fromValue(blocks.groupTour().tourLeaderLanguage()),
                        toInteger(blocks.groupTour().fitnessLevel())));
            }
            if (blocks.individualPackage() != null) {
                detail.setIndividualPackage(new IndividualPackageFields(
                        toInteger(blocks.individualPackage().minPartySize()),
                        toInteger(blocks.individualPackage().flexibleDateWindowDays())));
            }
            if (blocks.privateTour() != null) {
                detail.setPrivateTour(new PrivateTourFields(toInteger(blocks.privateTour().leadTimeDays()),
                        toInteger(blocks.privateTour().quoteValidDays())));
            }
            if (blocks.cruise() != null) {
                detail.setCruise(new CruiseFields(blocks.cruise().shipName(), toInteger(blocks.cruise().portCount())));
            }
            if (blocks.combo() != null) {
                detail.setCombo(new ComboFields(toInteger(blocks.combo().nights()),
                        blocks.combo().validFrom(), blocks.combo().validTo()));
            }
            if (blocks.dayTour() != null) {
                detail.setDayTour(new DayTourFields(toInteger(blocks.dayTour().durationHours()),
                        toInteger(blocks.dayTour().cutoffHours())));
            }
        }
        return detail;
    }

    default ProductTypeBlocks block(GroupTourFields g, IndividualPackageFields i,
                                    PrivateTourFields p, CruiseFields c,
                                    ComboFields cb, DayTourFields d) {
        return new ProductTypeBlocks(
                g == null ? null : new ProductTypeBlocks.GroupTour(
                        toShort(g.getMinPax()), toShort(g.getMaxPax()), toShort(g.getGuaranteedThreshold()),
                        g.getTourLeaderLanguage().getValue(), toShort(g.getFitnessLevel())),
                i == null ? null : new ProductTypeBlocks.IndividualPackage(
                        toShort(i.getMinPartySize()), toShort(i.getFlexibleDateWindowDays())),
                p == null ? null : new ProductTypeBlocks.PrivateTour(
                        toShort(p.getLeadTimeDays()), toShort(p.getQuoteValidDays())),
                c == null ? null : new ProductTypeBlocks.Cruise(
                        c.getShipName(), toShort(c.getPortCount())),
                cb == null ? null : new ProductTypeBlocks.Combo(
                        toShort(cb.getNights()), cb.getValidFrom(), cb.getValidTo()),
                d == null ? null : new ProductTypeBlocks.DayTour(
                        toShort(d.getDurationHours()), toShort(d.getCutoffHours())));
    }

    default Short toShort(Integer count) {
        return count == null ? null : count.shortValue();
    }

    default Integer toInteger(Short count) {
        return count == null ? null : count.intValue();
    }

    default ProductType toProductType(String type) {
        return type == null ? null : ProductType.fromValue(type);
    }

    default TranslationStatus toTranslationStatus(String status) {
        return status == null ? null : TranslationStatus.fromValue(status);
    }

    default AdminProductMarketState.MarketEnum toMarketEnum(String market) {
        return market == null ? null : AdminProductMarketState.MarketEnum.fromValue(market);
    }
}
