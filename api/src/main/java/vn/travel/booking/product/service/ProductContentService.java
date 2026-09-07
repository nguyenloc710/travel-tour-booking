package vn.travel.booking.product.service;

import vn.travel.booking.product.dto.DepartureView;
import vn.travel.booking.product.dto.HotelStay;
import vn.travel.booking.product.dto.ItineraryDay;
import vn.travel.booking.product.dto.ProductType;
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
    private static final int NGUONG_IT_CHO = 3;

    /** docs/13 mục 9.1: hai loại này không có lịch trình theo ngày. */
    private static final Set<ProductType> KHONG_CO_LICH_TRINH =
            EnumSet.of(ProductType.COMBO, ProductType.DAY_TOUR);

    /** Du thuyền ngủ trên tàu — tab tương ứng đổi thành Tàu và cabin (docs/05 mục 2). */
    private static final Set<ProductType> KHONG_CO_KHACH_SAN =
            EnumSet.of(ProductType.CRUISE);

    private final ProductContentRepository noiDung;
    private final MarketService markets;

    public ProductContentService(ProductContentRepository noiDung, MarketService markets) {
        this.noiDung = noiDung;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public List<ItineraryDay> itinerary(String market, String locale, String slug) {
        VisibleProduct sanPham = phaiNhinThayDuoc(market, locale, slug);
        if (KHONG_CO_LICH_TRINH.contains(sanPham.productType())) {
            throw new NotFoundException(
                    "loại " + sanPham.productType() + " không có lịch trình theo ngày");
        }
        return noiDung.findItinerary(sanPham.id(), locale);
    }

    @Transactional(readOnly = true)
    public List<HotelStay> hotelStays(String market, String locale, String slug) {
        VisibleProduct sanPham = phaiNhinThayDuoc(market, locale, slug);
        if (KHONG_CO_KHACH_SAN.contains(sanPham.productType())) {
            throw new NotFoundException(
                    "loại " + sanPham.productType() + " không có chặng nghỉ khách sạn");
        }
        return noiDung.findHotelStays(sanPham.id(), locale);
    }

    @Transactional(readOnly = true)
    public List<DepartureView> departures(String market, String locale, String slug) {
        VisibleProduct sanPham = phaiNhinThayDuoc(market, locale, slug);
        return noiDung.findDepartures(sanPham.id(), market, NGUONG_IT_CHO);
    }

    private VisibleProduct phaiNhinThayDuoc(String market, String locale, String slug) {
        markets.requireActive(market);
        return noiDung.findVisibleProduct(market, locale, slug)
                .orElseThrow(() -> new NotFoundException(
                        "product slug=" + slug + " market=" + market + " locale=" + locale));
    }
}
