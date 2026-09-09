package vn.travel.booking.common.mapper;

import vn.travel.booking.product.dto.NamedRef;
import vn.travel.booking.product.dto.ProductVariant;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.web.generated.model.ComboDetail;
import vn.travel.booking.web.generated.model.CruiseDetail;
import vn.travel.booking.web.generated.model.DayTourDetail;
import vn.travel.booking.web.generated.model.GalleryImage;
import vn.travel.booking.web.generated.model.GroupTourDetail;
import vn.travel.booking.web.generated.model.IndividualPackageDetail;
import vn.travel.booking.web.generated.model.PrivateTourDetail;
import vn.travel.booking.web.generated.model.ProductDetail;
import vn.travel.booking.web.generated.model.ProductPage;
import vn.travel.booking.web.generated.model.ProductSummary;
import vn.travel.booking.web.generated.model.ProductType;
import vn.travel.booking.web.generated.model.Ref;

/**
 * Ánh xạ từ kiểu của tầng application sang DTO <b>sinh ra từ spec</b>.
 *
 * <p>Lớp này tồn tại để tầng application không phải biết tới code sinh ra. Cái
 * giá phải trả là mỗi lần đổi spec thì sửa ở đây — nhưng đó chính là điều muốn
 * có: chỗ sửa là <b>một</b>, và quên sửa là lỗi biên dịch chứ không phải trường
 * bị mất lặng lẽ trong JSON.
 */
public final class RefMapper {

    private RefMapper() {
    }

    public static ProductPage toProductPage(PagedResult<vn.travel.booking.product.dto.ProductSummary> page) {
        return new ProductPage(
                page.items().stream().map(RefMapper::toProductSummary).toList(),
                page.page(),
                page.size(),
                page.totalItems(),
                page.totalPages());
    }

    public static ProductSummary toProductSummary(vn.travel.booking.product.dto.ProductSummary s) {
        return new ProductSummary(
                s.slug(),
                s.title(),
                type(s.productType()),
                s.shortDescription(),
                s.heroImage(),
                s.heroImageAlt(),
                toRef(s.region()),
                toRef(s.destination()),
                s.isNew(),
                s.reviewCount())
                // Trường rỗng bị bỏ hẳn khỏi JSON, không trả null — docs/13 mục 4.
                // Cấu hình Jackson lo việc đó (application.yml), ở đây cứ đặt null.
                .durationDays(s.durationDays())
                .priceFrom(toMoney(s.priceFrom()))
                .rating(s.rating());
    }

    /**
     * {@code switch} trên kiểu tổng {@code sealed}: thêm loại sản phẩm thứ bảy
     * mà quên chỗ này thì <b>không biên dịch được</b>. Đó là toàn bộ lý do dùng
     * {@code sealed} thay vì một interface thường (ADR-005).
     *
     * <p>Sáu lớp DTO sinh ra không có lớp cha chung — {@code ProductDetail} chỉ
     * là interface với đúng một phương thức — nên phần chung phải viết lặp. Đổi
     * lấy điều này: mỗi loại có đúng những trường của nó, không có trường thừa
     * mang giá trị null để frontend phải đoán.
     */
    public static ProductDetail toProductDetail(vn.travel.booking.product.dto.ProductDetail d) {
        return switch (d.variant()) {
            case ProductVariant.GroupTour v -> new GroupTourDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    toRef(d.region()), toRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.minPax(), v.maxPax(), v.guaranteedThreshold(), v.tourLeaderLanguage(),
                    v.fitnessLevel())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(toMoney(d.priceFrom())).rating(d.rating())
                    .gallery(imageSet(d.gallery())).layout(d.layout());

            case ProductVariant.IndividualPackage v -> new IndividualPackageDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    toRef(d.region()), toRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.minPartySize(), v.flexibleDateWindowDays())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(toMoney(d.priceFrom())).rating(d.rating())
                    .gallery(imageSet(d.gallery())).layout(d.layout());

            case ProductVariant.PrivateTour v -> new PrivateTourDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    toRef(d.region()), toRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.leadTimeDays(), v.quoteValidDays())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(toMoney(d.priceFrom())).rating(d.rating())
                    .gallery(imageSet(d.gallery())).layout(d.layout());

            case ProductVariant.Cruise v -> new CruiseDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    toRef(d.region()), toRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.shipName(), v.portCount())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(toMoney(d.priceFrom())).rating(d.rating())
                    .gallery(imageSet(d.gallery())).layout(d.layout());

            case ProductVariant.Combo v -> new ComboDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    toRef(d.region()), toRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.nights(), v.validFrom(), v.validTo())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(toMoney(d.priceFrom())).rating(d.rating())
                    .gallery(imageSet(d.gallery())).layout(d.layout());

            // DAY_TOUR không có durationDays — loại này đo bằng giờ, và CSDL
            // cưỡng chế duration_days IS NULL cho nó (ck_product_duration).
            case ProductVariant.DayTour v -> new DayTourDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    toRef(d.region()), toRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.durationHours(), v.cutoffHours())
                    .mapImage(d.mapImage())
                    .priceFrom(toMoney(d.priceFrom())).rating(d.rating())
                    .gallery(imageSet(d.gallery())).layout(d.layout());
        };
    }

    private static ProductType type(vn.travel.booking.product.dto.ProductType type) {
        return ProductType.fromValue(type.name());
    }

    public static Ref toRef(NamedRef r) {
        return new Ref(r.slug(), r.name());
    }

    /**
     * Bộ ảnh. Danh sách <b>rỗng</b> chứ không null: sản phẩm chưa có ảnh nào là
     * trạng thái hợp lệ, và bắt frontend phân biệt "chưa có ảnh" với "không có
     * trường" là bịa ra một trường hợp rìa không tồn tại.
     */
    private static java.util.List<GalleryImage> imageSet(
            java.util.List<vn.travel.booking.product.dto.GalleryImage> image) {
        if (image == null) {
            return java.util.List.of();
        }
        return image.stream()
                .map(a -> new GalleryImage(a.url(), a.alt(), a.width(), a.height()))
                .toList();
    }

    /**
     * {@code amount} là <b>chuỗi</b>, không phải số JSON — số dấu phẩy động của
     * JavaScript làm hỏng tiền. {@code toPlainString()} chứ không
     * {@code toString()}: {@code BigDecimal} có thể in ra dạng mũ.
     */
    public static vn.travel.booking.web.generated.model.Money toMoney(Money money) {
        if (money == null) {
            return null;
        }
        return new vn.travel.booking.web.generated.model.Money(
                money.amount().toPlainString(), money.currency());
    }
}
