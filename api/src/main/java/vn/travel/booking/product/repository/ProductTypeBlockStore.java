package vn.travel.booking.product.repository;

import org.springframework.stereotype.Repository;
import vn.travel.booking.product.dto.ProductTypeBlocks;
import vn.travel.booking.product.entity.ComboEntity;
import vn.travel.booking.product.entity.CruiseEntity;
import vn.travel.booking.product.entity.DayTourEntity;
import vn.travel.booking.product.entity.GroupTourEntity;
import vn.travel.booking.product.entity.IndividualPackageEntity;
import vn.travel.booking.product.entity.PrivateTourEntity;

import java.util.UUID;

/**
 * Sáu bảng con 1-1 sau <b>một</b> cửa.
 *
 * <p>Sáu repository tiêm thẳng vào service nghĩa là mỗi chỗ dùng phải tự nhớ
 * bảng nào đi với loại nào — sáu lần, ở sáu chỗ khác nhau. Lớp này giữ ánh xạ
 * đó đúng một lần, và {@code switch} trên kiểu niêm phong của Java bắt được
 * ngày thêm loại sản phẩm thứ bảy mà quên bảng con của nó.
 *
 * <p>Không đặt {@code product_type} ở bất kỳ đâu: cột đó có {@code DEFAULT} khoá
 * cứng cộng một {@code CHECK} ở mỗi bảng con, và khoá ngoại kép
 * {@code (product_id, product_type)} bắt mọi sai lệch ngay lúc ghi.
 */
@Repository
public class ProductTypeBlockStore {

    private final GroupTourRepository groupTour;
    private final IndividualPackageRepository individualPackage;
    private final PrivateTourRepository privateTour;
    private final CruiseRepository cruise;
    private final ComboRepository combo;
    private final DayTourRepository dayTour;

    public ProductTypeBlockStore(GroupTourRepository groupTour,
                                 IndividualPackageRepository individualPackage,
                                 PrivateTourRepository privateTour,
                                 CruiseRepository cruise,
                                 ComboRepository combo,
                                 DayTourRepository dayTour) {
        this.groupTour = groupTour;
        this.individualPackage = individualPackage;
        this.privateTour = privateTour;
        this.cruise = cruise;
        this.combo = combo;
        this.dayTour = dayTour;
    }

    /**
     * Ghi khối riêng của loại. Tạo mới hay sửa đều đi qua đây — khoá chính là
     * {@code product_id} nên {@code save()} tự phân biệt.
     */
    public void luu(UUID productId, String productType, ProductTypeBlocks blocks) {
        switch (productType) {
            case "GROUP_TOUR" -> {
                ProductTypeBlocks.GroupTour k = blocks.groupTour();
                GroupTourEntity e = groupTour.findById(productId)
                        .orElseGet(() -> new GroupTourEntity(productId));
                e.setMinPax(k.minPax());
                e.setMaxPax(k.maxPax());
                e.setGuaranteedThreshold(k.guaranteedThreshold());
                e.setTourLeaderLanguage(k.tourLeaderLanguage());
                e.setFitnessLevel(k.fitnessLevel());
                groupTour.save(e);
            }
            case "INDIVIDUAL_PACKAGE" -> {
                ProductTypeBlocks.IndividualPackage k = blocks.individualPackage();
                IndividualPackageEntity e = individualPackage.findById(productId)
                        .orElseGet(() -> new IndividualPackageEntity(productId));
                e.setMinPartySize(k.minPartySize());
                e.setFlexibleDateWindowDays(k.flexibleDateWindowDays());
                individualPackage.save(e);
            }
            case "PRIVATE_TOUR" -> {
                ProductTypeBlocks.PrivateTour k = blocks.privateTour();
                PrivateTourEntity e = privateTour.findById(productId)
                        .orElseGet(() -> new PrivateTourEntity(productId));
                e.setLeadTimeDays(k.leadTimeDays());
                e.setQuoteValidDays(k.quoteValidDays());
                privateTour.save(e);
            }
            case "CRUISE" -> {
                ProductTypeBlocks.Cruise k = blocks.cruise();
                CruiseEntity e = cruise.findById(productId)
                        .orElseGet(() -> new CruiseEntity(productId));
                e.setShipName(k.shipName());
                e.setPortCount(k.portCount());
                cruise.save(e);
            }
            case "COMBO" -> {
                ProductTypeBlocks.Combo k = blocks.combo();
                ComboEntity e = combo.findById(productId)
                        .orElseGet(() -> new ComboEntity(productId));
                e.setNights(k.nights());
                e.setValidFrom(k.validFrom());
                e.setValidTo(k.validTo());
                combo.save(e);
            }
            case "DAY_TOUR" -> {
                ProductTypeBlocks.DayTour k = blocks.dayTour();
                DayTourEntity e = dayTour.findById(productId)
                        .orElseGet(() -> new DayTourEntity(productId));
                e.setDurationHours(k.durationHours());
                e.setCutoffHours(k.cutoffHours());
                dayTour.save(e);
            }
            default -> throw new IllegalArgumentException("loại sản phẩm lạ: " + productType);
        }
    }

    /** Đọc khối riêng của loại. Chỉ khối khớp {@code productType} khác {@code null}. */
    public ProductTypeBlocks doc(UUID productId, String productType) {
        return switch (productType) {
            case "GROUP_TOUR" -> groupTour.findById(productId)
                    .map(e -> khoi(new ProductTypeBlocks.GroupTour(
                            e.getMinPax(), e.getMaxPax(), e.getGuaranteedThreshold(),
                            e.getTourLeaderLanguage(), e.getFitnessLevel())))
                    .orElseGet(ProductTypeBlockStore::rong);
            case "INDIVIDUAL_PACKAGE" -> individualPackage.findById(productId)
                    .map(e -> new ProductTypeBlocks(null, new ProductTypeBlocks.IndividualPackage(
                            e.getMinPartySize(), e.getFlexibleDateWindowDays()),
                            null, null, null, null))
                    .orElseGet(ProductTypeBlockStore::rong);
            case "PRIVATE_TOUR" -> privateTour.findById(productId)
                    .map(e -> new ProductTypeBlocks(null, null, new ProductTypeBlocks.PrivateTour(
                            e.getLeadTimeDays(), e.getQuoteValidDays()), null, null, null))
                    .orElseGet(ProductTypeBlockStore::rong);
            case "CRUISE" -> cruise.findById(productId)
                    .map(e -> new ProductTypeBlocks(null, null, null, new ProductTypeBlocks.Cruise(
                            e.getShipName(), e.getPortCount()), null, null))
                    .orElseGet(ProductTypeBlockStore::rong);
            case "COMBO" -> combo.findById(productId)
                    .map(e -> new ProductTypeBlocks(null, null, null, null, new ProductTypeBlocks.Combo(
                            e.getNights(), e.getValidFrom(), e.getValidTo()), null))
                    .orElseGet(ProductTypeBlockStore::rong);
            case "DAY_TOUR" -> dayTour.findById(productId)
                    .map(e -> new ProductTypeBlocks(null, null, null, null, null,
                            new ProductTypeBlocks.DayTour(e.getDurationHours(), e.getCutoffHours())))
                    .orElseGet(ProductTypeBlockStore::rong);
            default -> throw new IllegalArgumentException("loại sản phẩm lạ: " + productType);
        };
    }

    private static ProductTypeBlocks khoi(ProductTypeBlocks.GroupTour k) {
        return new ProductTypeBlocks(k, null, null, null, null, null);
    }

    private static ProductTypeBlocks rong() {
        return new ProductTypeBlocks(null, null, null, null, null, null);
    }
}
