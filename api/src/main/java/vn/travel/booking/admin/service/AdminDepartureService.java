package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.CopyResult;
import vn.travel.booking.admin.dto.DepartureCreateInput;
import vn.travel.booking.admin.dto.DeparturePatchInput;
import vn.travel.booking.admin.dto.DeparturePriceInput;
import vn.travel.booking.admin.dto.DeparturePriceView;
import vn.travel.booking.admin.dto.DepartureView;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.common.exception.SinglePriceMissingException;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.departure.entity.DepartureEntity;
import vn.travel.booking.departure.entity.DeparturePriceEntity;
import vn.travel.booking.departure.mapper.DepartureEntityMapper;
import vn.travel.booking.departure.repository.DeparturePriceRepository;
import vn.travel.booking.departure.repository.DepartureWriteRepository;
import vn.travel.booking.market.repository.MarketRepository;
import vn.travel.booking.market.repository.PaxTypeRepository;
import vn.travel.booking.product.entity.ProductEntity;
import vn.travel.booking.product.repository.ProductWriteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ngày khởi hành và bảng giá của nó (docs/22 M4 và M5).
 *
 * <p>Một sản phẩm bán ở cả hai thị trường có <b>hai bộ</b> {@code departure}
 * (ADR-006), nên lớp này làm việc với cặp {@code (sản phẩm, thị trường)} chứ
 * không với sản phẩm.
 */
@Service
public class AdminDepartureService {

    private final DepartureWriteRepository departureRepository;
    private final DeparturePriceRepository departurePriceRepository;
    private final ProductWriteRepository product;
    private final MarketRepository market;
    private final PaxTypeRepository paxTypeRepository;
    private final DepartureEntityMapper departureEntityMapper;

    public AdminDepartureService(DepartureWriteRepository departureRepository,
                                 DeparturePriceRepository departurePriceRepository,
                                 ProductWriteRepository product,
                                 MarketRepository market,
                                 PaxTypeRepository paxTypeRepository,
                                 DepartureEntityMapper departureEntityMapper) {
        this.departureRepository = departureRepository;
        this.departurePriceRepository = departurePriceRepository;
        this.product = product;
        this.market = market;
        this.paxTypeRepository = paxTypeRepository;
        this.departureEntityMapper = departureEntityMapper;
    }

    // ------------------------------------------------------------ đọc

    /**
     * Khác đường đọc công khai ở hai điểm: không giới hạn một thị trường, và
     * <b>có cả ngày đã đóng bán</b>. Nhân viên cần thấy cái đã đóng để mở lại
     * được.
     */
    @Transactional(readOnly = true)
    public List<DepartureView> list(UUID productId, String marketCode) {
        requireProduct(productId);

        List<DepartureEntity> departures = marketCode == null
                ? departureRepository.findByProductIdAndSoftDeleteFalseOrderByMarketAscDepartDateAsc(productId)
                : departureRepository.findByProductIdAndMarketAndSoftDeleteFalseOrderByDepartDateAsc(
                        productId, marketCode);

        return departures.stream().map(this::toView).toList();
    }

    // ------------------------------------------------------------ tạo và sửa

    @Transactional
    public DepartureView create(UUID productId, DepartureCreateInput input) {
        ProductEntity prod = requireProduct(productId);
        requireMarket(input.market());
        validateCabin(prod.getProductType(), input.cabinCategory());

        DepartureEntity e = new DepartureEntity(UUID.randomUUID(), productId, input.market());
        e.setSchedule(input.departDate(), input.days());
        e.setCapacity(input.capacity());
        e.setCabinCategory(input.cabinCategory());
        e.setDepartureOriginId(input.departureOriginId());
        if (input.baseStatus() != null) {
            e.setBaseStatus(input.baseStatus());
        }

        return toView(departureRepository.saveAndFlush(e));
    }

    @Transactional
    public DepartureView update(UUID departureId, DeparturePatchInput input) {
        DepartureEntity e = requireDeparture(departureId);
        ProductEntity prod = requireProduct(e.getProductId());

        if (input.cabinCategory() != null) {
            validateCabin(prod.getProductType(), input.cabinCategory());
            e.setCabinCategory(input.cabinCategory());
        }
        if (input.departDate() != null || input.days() != null) {
            // Đặt cùng lúc: return_date suy từ cả hai, và ck_dep_dates kiểm đúng
            // công thức đó. Đổi lẻ một trong hai rồi quên cái kia là ràng buộc
            // CSDL từ chối — muộn hơn và khó hiểu hơn.
            e.setSchedule(input.departDate() != null ? input.departDate() : e.getDepartDate(),
                    input.days() != null ? input.days() : e.getDays());
        }
        if (input.capacity() != null) {
            if (input.capacity() < e.getSeatsBooked()) {
                throw new AdminErrors.CapacityBelowBooked(input.capacity(), e.getSeatsBooked());
            }
            e.setCapacity(input.capacity());
        }
        if (input.baseStatus() != null) {
            e.setBaseStatus(input.baseStatus());
        }
        if (input.departureOriginId() != null) {
            e.setDepartureOriginId(input.departureOriginId());
        }

        return toView(departureRepository.saveAndFlush(e));
    }

    // ------------------------------------------------------------ nhân bản

    /**
     * Nhân bản <b>lịch</b> sang thị trường kia.
     *
     * <p><b>Không nhân bản giá.</b> Đây là điểm quan trọng nhất của phương thức
     * này: giá của mỗi thị trường là số người nhập, không phải kết quả tính từ
     * thị trường kia — tour bán cho khách Đan gồm vé bay quốc tế, bán cho khách
     * Việt thì không. Hai sản phẩm khác nhau, hai bảng giá khác nhau
     * (CLAUDE.md điều 4, docs/22 mục 10). Ngày mới vì thế <b>chưa có giá</b>, và
     * sản phẩm chưa hiện giá ở thị trường đích cho tới khi có người nhập.
     *
     * <p>Hạng cabin thì nhân bản: nó là thuộc tính của con tàu, không phải của
     * thị trường.
     *
     * <p>Ngày đã tồn tại ở thị trường đích thì <b>bỏ qua</b>, không ghi đè. Nhờ
     * vậy gọi lại lần hai không tạo bản sao thứ hai và không xoá giá vừa nhập.
     */
    @Transactional
    public CopyResult duplicate(UUID productId, String fromMarket, String toMarket, LocalDate fromDate) {
        requireProduct(productId);
        requireMarket(fromMarket);
        requireMarket(toMarket);

        if (fromMarket.equals(toMarket)) {
            throw new IllegalArgumentException("nhân bản sang chính thị trường nguồn");
        }

        int created = 0;
        int skipped = 0;

        for (DepartureEntity source
                : departureRepository.findByProductIdAndMarketAndSoftDeleteFalseOrderByDepartDateAsc(productId, fromMarket)) {

            if (fromDate != null && source.getDepartDate().isBefore(fromDate)) {
                continue;
            }
            if (departureRepository.existsByProductIdAndMarketAndDepartDateAndSoftDeleteFalse(
                    productId, toMarket, source.getDepartDate())) {
                skipped++;
                continue;
            }

            DepartureEntity newDeparture = new DepartureEntity(UUID.randomUUID(), productId, toMarket);
            newDeparture.setSchedule(source.getDepartDate(), source.getDays());
            newDeparture.setCapacity(source.getCapacity());
            newDeparture.setCabinCategory(source.getCabinCategory());
            // departure_origin_id KHÔNG chép: điểm khởi hành là dữ liệu riêng
            // của từng thị trường (bảng departure_origin có cột market), nên id
            // của thị trường nguồn không có nghĩa gì ở thị trường đích.
            departureRepository.save(newDeparture);
            created++;
        }

        departureRepository.flush();
        return new CopyResult(created, skipped);
    }

    // ------------------------------------------------------------ bảng giá

    /**
     * Thay <b>toàn bộ</b> bảng giá của một ngày khởi hành.
     *
     * <p>Thay toàn bộ chứ không sửa từng ô: bảng giá thiếu một ô là một tổ hợp
     * khách đặt được mà hệ thống không tính ra giá, và không ai phát hiện cho
     * tới khi đúng tổ hợp đó xuất hiện.
     *
     * <p>Tiền tệ lấy từ cấu hình thị trường của chính ngày khởi hành này, không
     * nhận từ client — cho client gửi tiền tệ là mở đường nhập giá DKK vào thị
     * trường {@code VN}, và không ràng buộc CSDL nào bắt được chuyện đó.
     *
     * <p>Ghi vào đây làm trigger {@code tg_departure_price_price_from} tính lại
     * {@code product_market.price_from} (migration {@code V5}).
     *
     * <p>Bảng giá của sản phẩm có lưu trú phải có dòng phòng đơn — xem
     * {@link #validateSingleRoomPrice}. Đây là chỗ chặn <b>sớm nhất</b> mà luật đó chặn
     * được, vì đây là màn hình mà người ta thực sự gõ giá vào.
     */
    @Transactional
    public List<DeparturePriceView> savePrices(UUID departureId, List<DeparturePriceInput> price) {
        DepartureEntity d = requireDeparture(departureId);
        MarketRepository.MarketConfig config = requireMarket(d.getMarket());
        Map<String, UUID> paxTypeMap = paxTypeRepository.byCode(d.getMarket());

        List<DeparturePriceEntity> newPrices = new ArrayList<>();
        for (DeparturePriceInput row : price) {
            UUID paxTypeId = paxTypeMap.get(row.paxTypeCode());
            if (paxTypeId == null) {
                throw new AdminErrors.UnknownPaxType(row.paxTypeCode(), d.getMarket());
            }
            newPrices.add(new DeparturePriceEntity(departureId, paxTypeId, row.occupancy(),
                    row.amount(), config.currency()));
        }

        // SAU vòng lặp trên, không phải trước: một bảng giá có thể vi phạm cả hai
        // luật cùng lúc, và "mã loại khách này không tồn tại" là thứ phải nói
        // trước — nói về giá phòng đơn của một loại khách không có thật thì biên
        // tập viên đi sửa nhầm chỗ.
        validateSingleRoomPrice(requireProduct(d.getProductId()).getProductType(), departureId, price);

        // Xoá rồi ghi lại, và flush() ở giữa: không flush thì Hibernate có thể
        // xếp câu INSERT trước câu DELETE và đụng khoá chính của chính dòng đang
        // thay.
        departurePriceRepository.deleteByDepartureId(departureId);
        departurePriceRepository.flush();
        departurePriceRepository.saveAllAndFlush(newPrices);

        return readPrice(departureId, d.getMarket());
    }

    // ------------------------------------------------------------ tiện ích

    private ProductEntity requireProduct(UUID productId) {
        return product.findByIdAndSoftDeleteFalse(productId)
                .orElseThrow(() -> new NotFoundException("product id=" + productId));
    }

    private DepartureEntity requireDeparture(UUID departureId) {
        return departureRepository.findByIdAndSoftDeleteFalse(departureId)
                .orElseThrow(() -> new NotFoundException("departure id=" + departureId));
    }

    /** Thị trường đã tắt trả 404: với bề mặt quản trị, "không có" và "đã tắt" dẫn tới cùng một việc. */
    private MarketRepository.MarketConfig requireMarket(String marketCode) {
        return market.config(marketCode)
                .orElseThrow(() -> new NotFoundException("market=" + marketCode));
    }

    /**
     * Sản phẩm có lưu trú qua đêm phải có giá phòng đơn, và giá ấy phải CAO HƠN
     * giá phòng đôi — quy tắc kiểm 23 của docs/12 mục 9.
     *
     * <p>Chặn ở đây vì đây là chỗ sớm nhất luật này chặn được. Công tắc mở bán
     * cũng kiểm, nhưng quy trình thật của docs/22 mục 5 bật bán ở bước 6 rồi mới
     * nhập ngày khởi hành ở bước 7 — nên lúc bấm công tắc thì thường chưa có ngày
     * nào để kiểm. Màn hình bảng giá thì ngược lại: nó là chỗ người ta gõ đúng
     * những con số này vào.
     *
     * <p>Danh sách <b>rỗng</b> không bị chặn ở đây — hợp đồng đã chặn nó bằng
     * {@code minItems: 1}, nên nhánh này chỉ tới được khi gọi thẳng service.
     *
     * <p>{@code DAY_TOUR} không kiểm — tour trong ngày không có đêm nào để ở phòng.
     */
    private static void validateSingleRoomPrice(String productType, UUID departureId,
                                        List<DeparturePriceInput> price) {
        if ("DAY_TOUR".equals(productType) || price.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> doublePrices = new LinkedHashMap<>();
        Map<String, BigDecimal> singlePrices = new LinkedHashMap<>();
        for (DeparturePriceInput row : price) {
            ("SINGLE".equals(row.occupancy()) ? singlePrices : doublePrices)
                    .put(row.paxTypeCode(), row.amount());
        }

        if (singlePrices.isEmpty()) {
            throw SinglePriceMissingException.forDeparture(departureId.toString());
        }

        // Dòng phòng đơn có mặt nhưng không cao hơn phòng đôi để lại đúng hậu quả
        // như khi vắng mặt: phụ thu bằng 0, và khách đi một mình trả giá chia đôi
        // phòng. Quy tắc 23b của bộ kiểm bắt cùng thứ này trên toàn CSDL.
        for (Map.Entry<String, BigDecimal> e : singlePrices.entrySet()) {
            BigDecimal doublePrice = doublePrices.get(e.getKey());
            if (doublePrice != null && e.getValue().compareTo(doublePrice) <= 0) {
                throw new SinglePriceMissingException(
                        "giá phòng đơn " + e.getValue() + " không cao hơn giá phòng đôi "
                                + doublePrice + " (loại khách " + e.getKey() + ")",
                        Map.of("departureId", departureId.toString(),
                                "paxTypeCode", e.getKey(),
                                "singleAmount", e.getValue().toPlainString(),
                                "doubleAmount", doublePrice.toPlainString()));
            }
        }
    }

    /** Hạng cabin chỉ có ở {@code CRUISE} — ràng buộc nghiệp vụ, không phải cột rỗng cho vui. */
    private static void validateCabin(String productType, String cabinCategory) {
        if (cabinCategory != null && !"CRUISE".equals(productType)) {
            throw new AdminErrors.CabinCategoryNotAllowed(productType);
        }
    }

    private DepartureView toView(DepartureEntity e) {
        return departureEntityMapper.toView(e, readPrice(e.getId(), e.getMarket()));
    }

    private List<DeparturePriceView> readPrice(UUID departureId, String marketCode) {
        Map<UUID, String> byId = paxTypeRepository.byId(marketCode);
        return departurePriceRepository.findByDepartureId(departureId).stream()
                .map(g -> departureEntityMapper.toPriceView(
                        g, byId.getOrDefault(g.getPaxTypeId(), g.getPaxTypeId().toString())))
                .toList();
    }
}
