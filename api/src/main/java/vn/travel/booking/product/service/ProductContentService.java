package vn.travel.booking.product.service;

import vn.travel.booking.product.dto.DepartureView;
import vn.travel.booking.product.dto.HotelStay;
import vn.travel.booking.product.dto.ItineraryDay;
import vn.travel.booking.product.dto.ProductType;
import vn.travel.booking.product.dto.ProductStopView;
import vn.travel.booking.product.dto.VisibleProduct;
import vn.travel.booking.product.repository.ProductContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.market.service.MarketService;
import vn.travel.booking.common.exception.NotFoundException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Ba endpoint con của một sản phẩm: lịch trình, chặng nghỉ khách sạn, ngày khởi hành.
 *
 * <p>Gom vào một lớp vì cả ba đi qua đúng một cổng chung — sản phẩm phải nhìn
 * thấy được ở cặp {@code (market, locale)} này trước đã — và tách ba lớp chỉ để
 * mỗi lớp có một phương thức là chia mà không được gì.
 */
@Service
public class ProductContentService {

    /**
     * Ngưỡng "còn ít chỗ" — docs/14 mục 2.4, hiện là 3 ở cả hai thị trường.
     *
     * <p>Để ở đây chứ không nhét vào lõi nghiệp vụ: nó là cấu hình. Khi hai thị
     * trường cần hai giá trị khác nhau thì nó chuyển thành một cột của bảng
     * {@code market}, và chữ ký của {@code DepartureStatuses.resolve} không đổi.
     */
    private static final int FEW_SEATS_THRESHOLD = 3;

    /** docs/13 mục 9.1: hai loại này không có lịch trình theo ngày. */
    private static final Set<ProductType> NO_ITINERARY_TYPES =
            EnumSet.of(ProductType.COMBO, ProductType.DAY_TOUR);

    /** Du thuyền ngủ trên tàu — tab tương ứng đổi thành Tàu và cabin (docs/05 mục 2). */
    private static final Set<ProductType> NO_HOTEL_TYPES =
            EnumSet.of(ProductType.CRUISE);

    private final ProductContentRepository productContentRepository;
    private final MarketService markets;

    public ProductContentService(ProductContentRepository productContentRepository, MarketService markets) {
        this.productContentRepository = productContentRepository;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public List<ItineraryDay> itinerary(String market, String locale, String slug) {
        VisibleProduct product = requireVisible(market, locale, slug);
        if (NO_ITINERARY_TYPES.contains(product.productType())) {
            throw new NotFoundException(
                    "loại " + product.productType() + " không có lịch trình theo ngày");
        }
        return productContentRepository.findItinerary(product.id(), locale);
    }

    /**
     * Các chặng dừng của lộ trình — khối "bản đồ lộ trình" của docs/05 mục 6.1.
     *
     * <p>Cùng cổng 404 với {@code /itinerary}, và cùng lý do: nó dựng từ chính
     * lịch trình đó, nên loại nào không có lịch trình thì cũng không có chặng.
     */
    @Transactional(readOnly = true)
    public List<ProductStopView> stops(String market, String locale, String slug) {
        VisibleProduct product = requireVisible(market, locale, slug);
        if (NO_ITINERARY_TYPES.contains(product.productType())) {
            throw new NotFoundException(
                    "loại " + product.productType() + " không có lộ trình theo chặng");
        }
        return productContentRepository.findStops(product.id(), locale);
    }

    @Transactional(readOnly = true)
    public List<HotelStay> hotelStays(String market, String locale, String slug) {
        VisibleProduct product = requireVisible(market, locale, slug);
        if (NO_HOTEL_TYPES.contains(product.productType())) {
            throw new NotFoundException(
                    "loại " + product.productType() + " không có chặng nghỉ khách sạn");
        }
        return productContentRepository.findHotelStays(product.id(), locale);
    }

    @Transactional(readOnly = true)
    public List<DepartureView> departures(String market, String locale, String slug) {
        VisibleProduct product = requireVisible(market, locale, slug);
        return productContentRepository.findDepartures(product.id(), market, FEW_SEATS_THRESHOLD);
    }

    private VisibleProduct requireVisible(String market, String locale, String slug) {
        markets.requireActive(market);
        return productContentRepository.findVisibleProduct(market, locale, slug)
                .orElseThrow(() -> new NotFoundException(
                        "product slug=" + slug + " market=" + market + " locale=" + locale));
    }
}
