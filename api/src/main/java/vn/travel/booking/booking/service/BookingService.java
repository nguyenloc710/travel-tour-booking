package vn.travel.booking.booking.service;

import vn.travel.booking.booking.dto.BookingCommand;
import vn.travel.booking.booking.dto.BookingDraft;
import vn.travel.booking.booking.dto.BookingView;
import vn.travel.booking.booking.dto.DeparturePricing;
import vn.travel.booking.booking.dto.PricingQuery;
import vn.travel.booking.booking.dto.SeatHoldView;
import vn.travel.booking.booking.repository.BookingPricingRepository;
import vn.travel.booking.booking.repository.BookingRepository;
import vn.travel.booking.booking.repository.SeatHoldRepository;
import vn.travel.booking.common.exception.BookingErrors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.market.service.MarketService;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.common.exception.SinglePriceMissingException;
import vn.travel.booking.pricing.dto.PaxLine;
import vn.travel.booking.pricing.dto.PriceBreakdown;
import vn.travel.booking.pricing.service.PricingEngine;
import vn.travel.booking.pricing.dto.PricingInput;
import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tính giá, giữ chỗ, đặt tour.
 *
 * <p>Lớp này <b>không tính tiền</b>: nó gom dữ liệu rồi gọi engine ở
 * {@code domain}. Ranh giới đó là cố ý — công thức giá phải test được bằng JUnit
 * thuần trong vài mili giây, không dựng context, không cơ sở dữ liệu.
 */
@Service
public class BookingService {

    /** Hạn giữ chỗ — docs/14 mục 2.4. Là cấu hình, nên nằm ở tầng này chứ không ở lõi. */
    private static final Duration HAN_GIU_CHO = Duration.ofMinutes(20);

    /** Loại không có tồn kho chung thì không giữ chỗ — docs/14 mục 6. */
    private static final Set<String> KHONG_CO_TON_KHO = Set.of("INDIVIDUAL_PACKAGE", "PRIVATE_TOUR");

    /** Không đặt trực tiếp được: CTA là "Yêu cầu báo giá" — docs/23 mục 1. */
    private static final Set<String> KHONG_DAT_TRUC_TIEP = Set.of("PRIVATE_TOUR");

    private final BookingPricingRepository giaPort;
    private final SeatHoldRepository giuChoPort;
    private final BookingRepository donPort;
    private final MarketService markets;

    public BookingService(BookingPricingRepository giaPort, SeatHoldRepository giuChoPort,
                           BookingRepository donPort, MarketService markets) {
        this.giaPort = giaPort;
        this.giuChoPort = giuChoPort;
        this.donPort = donPort;
        this.markets = markets;
    }

    // ------------------------------------------------------------ tính giá

    /**
     * <b>Không lưu gì.</b> Màn hình đặt tour gọi mỗi lần khách đổi lựa chọn, nên
     * nó phải gọi lại được vô số lần mà không tạo ra gì (docs/13 mục 9.2).
     */
    @Transactional(readOnly = true)
    public PriceBreakdown xemTruocGia(String market, PricingQuery truyVan) {
        markets.requireActive(market);
        return tinh(nap(market, truyVan), truyVan);
    }

    // ------------------------------------------------------------ giữ chỗ

    @Transactional
    public SeatHoldView giuCho(String market, UUID departureId, int seats, String sessionRef) {
        markets.requireActive(market);

        DeparturePricing d = giaPort.load(market, departureId, null)
                .orElseThrow(() -> new NotFoundException("departure id=" + departureId));

        if (KHONG_CO_TON_KHO.contains(d.productType())) {
            throw new BookingErrors.ProductNotBookable(
                    "loại " + d.productType() + " không có tồn kho chung nên không giữ chỗ");
        }

        return giuChoPort.hold(departureId, seats, sessionRef, HAN_GIU_CHO);
    }

    @Transactional
    public void boGiuCho(String market, UUID seatHoldId) {
        markets.requireActive(market);
        giuChoPort.release(seatHoldId);
    }

    // ------------------------------------------------------------ đặt tour

    @Transactional
    public BookingView datTour(String market, String locale, BookingCommand lenh) {
        markets.requireActive(market);

        DeparturePricing d = nap(market, new PricingQuery(
                lenh.departureId(), lenh.pax(), lenh.singleTravellers(), lenh.departureOriginId()));

        if (KHONG_DAT_TRUC_TIEP.contains(d.productType())) {
            throw new BookingErrors.ProductNotBookable(
                    "loại " + d.productType() + " chỉ nhận yêu cầu báo giá");
        }

        boolean canGiuCho = !KHONG_CO_TON_KHO.contains(d.productType());
        if (canGiuCho && lenh.seatHoldId() == null) {
            // Không có giữ chỗ nghĩa là chỗ chưa bao giờ được khoá, và hai khách
            // cùng bấm đặt sẽ cùng thành công. Chặn ở đây thay vì hy vọng frontend nhớ.
            throw new BookingErrors.SeatHoldExpired("loại có tồn kho phải kèm seatHoldId");
        }

        PriceBreakdown bang = tinh(d, new PricingQuery(
                lenh.departureId(), lenh.pax(), lenh.singleTravellers(), lenh.departureOriginId()));

        return donPort.create(new BookingDraft(
                market, locale, d.productId(), d.departureId(), lenh.seatHoldId(),
                d.productTitle(), lenh.pax(), lenh.passengers(),
                lenh.contactEmail(), lenh.contactPhone(), bang));
    }

    @Transactional(readOnly = true)
    public BookingView traDon(String market, String reference, String email) {
        markets.requireActive(market);
        return donPort.findByReferenceAndEmail(market, reference, email)
                .orElseThrow(() -> new NotFoundException("booking reference=" + reference));
    }

    // ------------------------------------------------------------ nội bộ

    private DeparturePricing nap(String market, PricingQuery truyVan) {
        return giaPort.load(market, truyVan.departureId(), truyVan.departureOriginId())
                .orElseThrow(() -> new NotFoundException("departure id=" + truyVan.departureId()));
    }

    /**
     * Dựng đầu vào cho engine.
     *
     * <p>Bốn dòng của docs/14 mục 2.1 <b>chưa có nguồn dữ liệu</b> trong lược đồ:
     * bảo hiểm, đêm khách sạn trước bay, giảm đặt sớm và nâng hạng cabin. Chúng
     * không bị bỏ quên — engine đã có chỗ cho cả bốn, chỉ chưa có bảng để đọc.
     * Ghi ở docs/12 mục 10.
     */
    private PriceBreakdown tinh(DeparturePricing d, PricingQuery truyVan) {
        List<PaxLine> dong = new ArrayList<>();

        for (Map.Entry<String, Integer> e : truyVan.pax().entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) {
                continue;
            }
            Money donGia = d.doubleOccupancy().get(e.getKey());
            if (donGia == null) {
                throw new NotFoundException("chưa có giá cho loại khách " + e.getKey());
            }
            dong.add(PaxLine.of(e.getKey(), e.getValue(), donGia));
        }
        if (dong.isEmpty()) {
            throw new NotFoundException("đơn không có khách nào");
        }

        PricingInput dauVao = PricingInput.cua(dong, d.fractionDigits(), d.depositRate())
                .phiXuLy(d.processingFee());

        if (truyVan.singleTravellers() > 0) {
            dauVao.phongDon(truyVan.singleTravellers(), phuThuPhongDon(d));
        }
        if (d.originSurcharge() != null) {
            dauVao.phuThuDiemKhoiHanh(d.originSurcharge());
        }

        return PricingEngine.tinh(dauVao);
    }

    /**
     * Phụ thu phòng đơn = giá phòng đơn − giá phòng đôi, lấy theo loại khách
     * người lớn đầu tiên có đủ cả hai mức giá.
     *
     * <p><b>Không có giá phòng đơn thì NÉM, không trả 0.</b> Trước đây chỗ này
     * kết thúc bằng {@code orElse(0)}, và cái 0 ấy là một lỗi tiền im lặng: khách
     * đi một mình đặt được nguyên chuyến ở giá chia đôi phòng, bảng giá không có
     * dòng phụ thu nào để ai đó thấy là thiếu, và chênh lệch chỉ lộ ra khi kế
     * toán đối soát với khách sạn — sau khi khách đã đi.
     *
     * <p>Máy chủ không đoán giữa "chuyến này không bán phòng đơn" và "quên nhập
     * giá": hai thứ để lại đúng cùng một dấu vết, và đoán theo hướng dễ chịu là
     * đoán về phía mất tiền. Đường bật bán đã chặn từ đầu (quy tắc kiểm 23), nên
     * nhánh này đáng lẽ không bao giờ chạy — nó là lưới thứ hai.
     */
    private static Money phuThuPhongDon(DeparturePricing d) {
        return d.singleOccupancy().entrySet().stream()
                .filter(e -> d.doubleOccupancy().containsKey(e.getKey()))
                .map(e -> new Money(
                        e.getValue().amount().subtract(d.doubleOccupancy().get(e.getKey()).amount())
                                .max(BigDecimal.ZERO),
                        e.getValue().currency()))
                .findFirst()
                .orElseThrow(() ->
                        SinglePriceMissingException.cuaNgayKhoiHanh(d.departureId().toString()));
    }
}
