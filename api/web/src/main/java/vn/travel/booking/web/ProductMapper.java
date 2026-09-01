package vn.travel.booking.web;

import vn.travel.booking.application.product.NamedRef;
import vn.travel.booking.application.product.ProductVariant;
import vn.travel.booking.application.shared.PagedResult;
import vn.travel.booking.domain.shared.Money;
import vn.travel.booking.web.generated.model.ComboDetail;
import vn.travel.booking.web.generated.model.CruiseDetail;
import vn.travel.booking.web.generated.model.DayTourDetail;
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
final class ProductMapper {

    private ProductMapper() {
    }

    static ProductPage sangTrang(PagedResult<vn.travel.booking.application.product.ProductSummary> trang) {
        return new ProductPage(
                trang.items().stream().map(ProductMapper::sangTomTat).toList(),
                trang.page(),
                trang.size(),
                trang.totalItems(),
                trang.totalPages());
    }

    static ProductSummary sangTomTat(vn.travel.booking.application.product.ProductSummary s) {
        return new ProductSummary(
                s.slug(),
                s.title(),
                loai(s.productType()),
                s.shortDescription(),
                s.heroImage(),
                s.heroImageAlt(),
                sangRef(s.region()),
                sangRef(s.destination()),
                s.isNew(),
                s.reviewCount())
                // Trường rỗng bị bỏ hẳn khỏi JSON, không trả null — docs/13 mục 4.
                // Cấu hình Jackson lo việc đó (application.yml), ở đây cứ đặt null.
                .durationDays(s.durationDays())
                .priceFrom(sangTien(s.priceFrom()))
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
    static ProductDetail sangChiTiet(vn.travel.booking.application.product.ProductDetail d) {
        return switch (d.variant()) {
            case ProductVariant.GroupTour v -> new GroupTourDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    sangRef(d.region()), sangRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.minPax(), v.maxPax(), v.guaranteedThreshold(), v.tourLeaderLanguage(),
                    v.fitnessLevel())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(sangTien(d.priceFrom())).rating(d.rating());

            case ProductVariant.IndividualPackage v -> new IndividualPackageDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    sangRef(d.region()), sangRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.minPartySize(), v.flexibleDateWindowDays())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(sangTien(d.priceFrom())).rating(d.rating());

            case ProductVariant.PrivateTour v -> new PrivateTourDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    sangRef(d.region()), sangRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.leadTimeDays(), v.quoteValidDays())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(sangTien(d.priceFrom())).rating(d.rating());

            case ProductVariant.Cruise v -> new CruiseDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    sangRef(d.region()), sangRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.shipName(), v.portCount())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(sangTien(d.priceFrom())).rating(d.rating());

            case ProductVariant.Combo v -> new ComboDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    sangRef(d.region()), sangRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.nights(), v.validFrom(), v.validTo())
                    .mapImage(d.mapImage()).durationDays(d.durationDays())
                    .priceFrom(sangTien(d.priceFrom())).rating(d.rating());

            // DAY_TOUR không có durationDays — loại này đo bằng giờ, và CSDL
            // cưỡng chế duration_days IS NULL cho nó (ck_product_duration).
            case ProductVariant.DayTour v -> new DayTourDetail(
                    d.slug(), d.title(), d.productType().name(), d.shortDescription(),
                    d.longDescription(), d.whyChooseThis(), d.heroImage(), d.heroImageAlt(),
                    sangRef(d.region()), sangRef(d.destination()), d.isNew(), d.reviewCount(),
                    v.durationHours(), v.cutoffHours())
                    .mapImage(d.mapImage())
                    .priceFrom(sangTien(d.priceFrom())).rating(d.rating());
        };
    }

    private static ProductType loai(vn.travel.booking.application.product.ProductType loai) {
        return ProductType.fromValue(loai.name());
    }

    static Ref sangRef(NamedRef r) {
        return new Ref(r.slug(), r.name());
    }

    /**
     * {@code amount} là <b>chuỗi</b>, không phải số JSON — số dấu phẩy động của
     * JavaScript làm hỏng tiền. {@code toPlainString()} chứ không
     * {@code toString()}: {@code BigDecimal} có thể in ra dạng mũ.
     */
    static vn.travel.booking.web.generated.model.Money sangTien(Money tien) {
        if (tien == null) {
            return null;
        }
        return new vn.travel.booking.web.generated.model.Money(
                tien.amount().toPlainString(), tien.currency());
    }
}
