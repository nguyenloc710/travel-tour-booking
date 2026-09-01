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
import vn.travel.booking.common.money.Money;
import vn.travel.booking.departure.entity.DepartureEntity;
import vn.travel.booking.departure.entity.DeparturePriceEntity;
import vn.travel.booking.departure.repository.DeparturePriceRepository;
import vn.travel.booking.departure.repository.DepartureWriteRepository;
import vn.travel.booking.market.repository.MarketRepository;
import vn.travel.booking.market.repository.PaxTypeRepository;
import vn.travel.booking.product.entity.ProductEntity;
import vn.travel.booking.product.repository.ProductWriteRepository;

import java.time.LocalDate;
import java.util.ArrayList;
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

    private final DepartureWriteRepository ngayKhoiHanh;
    private final DeparturePriceRepository bangGia;
    private final ProductWriteRepository sanPham;
    private final MarketRepository market;
    private final PaxTypeRepository loaiKhach;

    public AdminDepartureService(DepartureWriteRepository ngayKhoiHanh,
                                 DeparturePriceRepository bangGia,
                                 ProductWriteRepository sanPham,
                                 MarketRepository market,
                                 PaxTypeRepository loaiKhach) {
        this.ngayKhoiHanh = ngayKhoiHanh;
        this.bangGia = bangGia;
        this.sanPham = sanPham;
        this.market = market;
        this.loaiKhach = loaiKhach;
    }

    // ------------------------------------------------------------ đọc

    /**
     * Khác đường đọc công khai ở hai điểm: không giới hạn một thị trường, và
     * <b>có cả ngày đã đóng bán</b>. Nhân viên cần thấy cái đã đóng để mở lại
     * được.
     */
    @Transactional(readOnly = true)
    public List<DepartureView> danhSach(UUID productId, String maThiTruong) {
        phaiCoSanPham(productId);

        List<DepartureEntity> ds = maThiTruong == null
                ? ngayKhoiHanh.findByProductIdAndSoftDeleteFalseOrderByMarketAscDepartDateAsc(productId)
                : ngayKhoiHanh.findByProductIdAndMarketAndSoftDeleteFalseOrderByDepartDateAsc(
                        productId, maThiTruong);

        return ds.stream().map(this::sangView).toList();
    }

    // ------------------------------------------------------------ tạo và sửa

    @Transactional
    public DepartureView tao(UUID productId, DepartureCreateInput input) {
        ProductEntity sp = phaiCoSanPham(productId);
        phaiCoThiTruong(input.market());
        kiemCabin(sp.getProductType(), input.cabinCategory());

        DepartureEntity e = new DepartureEntity(UUID.randomUUID(), productId, input.market());
        e.datLich(input.departDate(), input.days());
        e.setCapacity(input.capacity());
        e.setCabinCategory(input.cabinCategory());
        e.setDepartureOriginId(input.departureOriginId());
        if (input.baseStatus() != null) {
            e.setBaseStatus(input.baseStatus());
        }

        return sangView(ngayKhoiHanh.saveAndFlush(e));
    }

    @Transactional
    public DepartureView sua(UUID departureId, DeparturePatchInput input) {
        DepartureEntity e = phaiCoNgay(departureId);
        ProductEntity sp = phaiCoSanPham(e.getProductId());

        if (input.cabinCategory() != null) {
            kiemCabin(sp.getProductType(), input.cabinCategory());
            e.setCabinCategory(input.cabinCategory());
        }
        if (input.departDate() != null || input.days() != null) {
            // Đặt cùng lúc: return_date suy từ cả hai, và ck_dep_dates kiểm đúng
            // công thức đó. Đổi lẻ một trong hai rồi quên cái kia là ràng buộc
            // CSDL từ chối — muộn hơn và khó hiểu hơn.
            e.datLich(input.departDate() != null ? input.departDate() : e.getDepartDate(),
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

        return sangView(ngayKhoiHanh.saveAndFlush(e));
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
    public CopyResult nhanBan(UUID productId, String tu, String sang, LocalDate tuNgay) {
        phaiCoSanPham(productId);
        phaiCoThiTruong(tu);
        phaiCoThiTruong(sang);

        if (tu.equals(sang)) {
            throw new IllegalArgumentException("nhân bản sang chính thị trường nguồn");
        }

        int taoMoi = 0;
        int boQua = 0;

        for (DepartureEntity nguon
                : ngayKhoiHanh.findByProductIdAndMarketAndSoftDeleteFalseOrderByDepartDateAsc(productId, tu)) {

            if (tuNgay != null && nguon.getDepartDate().isBefore(tuNgay)) {
                continue;
            }
            if (ngayKhoiHanh.existsByProductIdAndMarketAndDepartDateAndSoftDeleteFalse(
                    productId, sang, nguon.getDepartDate())) {
                boQua++;
                continue;
            }

            DepartureEntity moi = new DepartureEntity(UUID.randomUUID(), productId, sang);
            moi.datLich(nguon.getDepartDate(), nguon.getDays());
            moi.setCapacity(nguon.getCapacity());
            moi.setCabinCategory(nguon.getCabinCategory());
            // departure_origin_id KHÔNG chép: điểm khởi hành là dữ liệu riêng
            // của từng thị trường (bảng departure_origin có cột market), nên id
            // của thị trường nguồn không có nghĩa gì ở thị trường đích.
            ngayKhoiHanh.save(moi);
            taoMoi++;
        }

        ngayKhoiHanh.flush();
        return new CopyResult(taoMoi, boQua);
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
     */
    @Transactional
    public List<DeparturePriceView> luuGia(UUID departureId, List<DeparturePriceInput> gia) {
        DepartureEntity d = phaiCoNgay(departureId);
        MarketRepository.CauHinh cauHinh = phaiCoThiTruong(d.getMarket());
        Map<String, UUID> ma = loaiKhach.theoMa(d.getMarket());

        List<DeparturePriceEntity> moi = new ArrayList<>();
        for (DeparturePriceInput dong : gia) {
            UUID paxTypeId = ma.get(dong.paxTypeCode());
            if (paxTypeId == null) {
                throw new AdminErrors.UnknownPaxType(dong.paxTypeCode(), d.getMarket());
            }
            moi.add(new DeparturePriceEntity(departureId, paxTypeId, dong.occupancy(),
                    dong.amount(), cauHinh.currency()));
        }

        // Xoá rồi ghi lại, và flush() ở giữa: không flush thì Hibernate có thể
        // xếp câu INSERT trước câu DELETE và đụng khoá chính của chính dòng đang
        // thay.
        bangGia.deleteByDepartureId(departureId);
        bangGia.flush();
        bangGia.saveAllAndFlush(moi);

        return docGia(departureId, d.getMarket());
    }

    // ------------------------------------------------------------ tiện ích

    private ProductEntity phaiCoSanPham(UUID productId) {
        return sanPham.findByIdAndSoftDeleteFalse(productId)
                .orElseThrow(() -> new NotFoundException("product id=" + productId));
    }

    private DepartureEntity phaiCoNgay(UUID departureId) {
        return ngayKhoiHanh.findByIdAndSoftDeleteFalse(departureId)
                .orElseThrow(() -> new NotFoundException("departure id=" + departureId));
    }

    /** Thị trường đã tắt trả 404: với bề mặt quản trị, "không có" và "đã tắt" dẫn tới cùng một việc. */
    private MarketRepository.CauHinh phaiCoThiTruong(String maThiTruong) {
        return market.cauHinh(maThiTruong)
                .orElseThrow(() -> new NotFoundException("market=" + maThiTruong));
    }

    /** Hạng cabin chỉ có ở {@code CRUISE} — ràng buộc nghiệp vụ, không phải cột rỗng cho vui. */
    private static void kiemCabin(String productType, String cabinCategory) {
        if (cabinCategory != null && !"CRUISE".equals(productType)) {
            throw new AdminErrors.CabinCategoryNotAllowed(productType);
        }
    }

    private DepartureView sangView(DepartureEntity e) {
        return new DepartureView(e.getId(), e.getMarket(), e.getDepartDate(), e.getReturnDate(),
                e.getDays(), e.getCabinCategory(), e.getBaseStatus(), e.getCapacity(),
                e.getSeatsBooked(), e.getDepartureOriginId(),
                docGia(e.getId(), e.getMarket()));
    }

    private List<DeparturePriceView> docGia(UUID departureId, String maThiTruong) {
        Map<UUID, String> theoId = loaiKhach.theoId(maThiTruong);
        return bangGia.findByDepartureId(departureId).stream()
                .map(g -> new DeparturePriceView(
                        theoId.getOrDefault(g.getPaxTypeId(), g.getPaxTypeId().toString()),
                        g.getOccupancy(),
                        new Money(g.getAmount(), g.getCurrency())))
                .toList();
    }
}
