package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.PriceTierInput;
import vn.travel.booking.admin.dto.PriceTierView;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.market.repository.MarketRepository;
import vn.travel.booking.pricing.entity.PriceTierEntity;
import vn.travel.booking.pricing.repository.PriceTierWriteRepository;
import vn.travel.booking.product.entity.ProductEntity;
import vn.travel.booking.product.repository.ProductWriteRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Thang giá {@code PRIVATE_TOUR} của một thị trường (docs/22 M5, docs/14 mục 4).
 *
 * <p>Thay <b>toàn bộ</b> thang, không sửa lẻ từng bậc: bậc giá là một thang liên
 * tục, và sửa lẻ một bậc là cách tạo ra khoảng hở giữa hai bậc mà không ai thấy.
 */
@Service
public class AdminPriceTierService {

    private final PriceTierWriteRepository bacGia;
    private final ProductWriteRepository sanPham;
    private final MarketRepository market;

    public AdminPriceTierService(PriceTierWriteRepository bacGia,
                                 ProductWriteRepository sanPham,
                                 MarketRepository market) {
        this.bacGia = bacGia;
        this.sanPham = sanPham;
        this.market = market;
    }

    @Transactional(readOnly = true)
    public List<PriceTierView> danhSach(UUID productId, String maThiTruong) {
        return bacGia.findByProductIdAndMarketAndSoftDeleteFalseOrderByMinPaxAsc(productId, maThiTruong)
                .stream()
                .map(AdminPriceTierService::sangView)
                .toList();
    }

    @Transactional
    public List<PriceTierView> luu(UUID productId, String maThiTruong, List<PriceTierInput> thang) {
        ProductEntity sp = sanPham.findByIdAndSoftDeleteFalse(productId)
                .orElseThrow(() -> new NotFoundException("product id=" + productId));

        if (!"PRIVATE_TOUR".equals(sp.getProductType())) {
            throw new AdminErrors.ProductTypeBlockMismatch(
                    sp.getProductType(), "privateTour", 0);
        }

        MarketRepository.CauHinh cauHinh = market.cauHinh(maThiTruong)
                .orElseThrow(() -> new NotFoundException("market=" + maThiTruong));

        List<PriceTierInput> daSap = thang.stream()
                .sorted(Comparator.comparing(PriceTierInput::minPax))
                .toList();
        kiemLienMach(daSap);

        // Xoá MỀM bậc cũ, không xoá cứng: price_tier thuộc nhóm A của docs/11
        // mục 11.2, và một báo giá đã gửi cho khách tham chiếu tới bậc giá lúc
        // đó. Xoá cứng là làm báo giá cũ mất chỗ dựa.
        bacGia.findByProductIdAndMarketAndSoftDeleteFalseOrderByMinPaxAsc(productId, maThiTruong)
                .forEach(cu -> cu.setSoftDelete(true));
        bacGia.flush();

        List<PriceTierEntity> moi = new ArrayList<>();
        for (PriceTierInput bac : daSap) {
            moi.add(new PriceTierEntity(UUID.randomUUID(), productId, maThiTruong,
                    bac.minPax(), bac.maxPax(), bac.pricePerPerson(), cauHinh.currency()));
        }
        bacGia.saveAllAndFlush(moi);

        return danhSach(productId, maThiTruong);
    }

    /**
     * Thang phải liền mạch, và <b>đúng một</b> bậc cuối không có trần.
     *
     * <p>Một khoảng hở nghĩa là có số khách mà hệ thống không tính ra giá — và
     * không ai phát hiện cho tới khi đúng nhóm khách đó hỏi. Chồng lấn thì tệ
     * hơn: hai bậc cùng khớp, và giá phụ thuộc thứ tự dòng trong CSDL.
     *
     * <p>CSDL không diễn đạt được luật này bằng một {@code CHECK} — nó nói về
     * quan hệ giữa các dòng trong cùng một nhóm — nên nó phải sống ở đây.
     */
    private static void kiemLienMach(List<PriceTierInput> daSap) {
        for (int i = 0; i < daSap.size(); i++) {
            PriceTierInput bac = daSap.get(i);
            boolean bacCuoi = i == daSap.size() - 1;

            if (bac.maxPax() == null && !bacCuoi) {
                throw new AdminErrors.PriceTierNotContiguous(
                        "chỉ bậc cuối được bỏ trống maxPax", bac.minPax());
            }
            if (bac.maxPax() != null && bac.maxPax() < bac.minPax()) {
                throw new AdminErrors.PriceTierNotContiguous(
                        "maxPax nhỏ hơn minPax", bac.minPax());
            }
            if (!bacCuoi) {
                PriceTierInput ke = daSap.get(i + 1);
                if (ke.minPax() != bac.maxPax() + 1) {
                    throw new AdminErrors.PriceTierNotContiguous(
                            "bậc sau phải bắt đầu ngay sau bậc trước", ke.minPax());
                }
            }
        }
    }

    private static PriceTierView sangView(PriceTierEntity e) {
        return new PriceTierView(e.getId(), e.getMarket(), e.getMinPax(), e.getMaxPax(),
                new Money(e.getPricePerPerson(), e.getCurrency()));
    }
}
