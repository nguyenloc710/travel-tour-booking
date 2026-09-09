package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.PriceTierInput;
import vn.travel.booking.admin.dto.PriceTierView;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.market.repository.MarketRepository;
import vn.travel.booking.pricing.entity.PriceTierEntity;
import vn.travel.booking.pricing.mapper.PriceTierEntityMapper;
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

    private final PriceTierWriteRepository priceTierRepository;
    private final ProductWriteRepository product;
    private final MarketRepository market;
    private final PriceTierEntityMapper priceTierEntityMapper;

    public AdminPriceTierService(PriceTierWriteRepository priceTierRepository,
                                 ProductWriteRepository product,
                                 MarketRepository market,
                                 PriceTierEntityMapper priceTierEntityMapper) {
        this.priceTierRepository = priceTierRepository;
        this.product = product;
        this.market = market;
        this.priceTierEntityMapper = priceTierEntityMapper;
    }

    @Transactional(readOnly = true)
    public List<PriceTierView> list(UUID productId, String marketCode) {
        return priceTierEntityMapper.toViewList(
                priceTierRepository.findByProductIdAndMarketAndSoftDeleteFalseOrderByMinPaxAsc(productId, marketCode));
    }

    @Transactional
    public List<PriceTierView> save(UUID productId, String marketCode, List<PriceTierInput> tiers) {
        ProductEntity prod = product.findByIdAndSoftDeleteFalse(productId)
                .orElseThrow(() -> new NotFoundException("product id=" + productId));

        if (!"PRIVATE_TOUR".equals(prod.getProductType())) {
            throw new AdminErrors.ProductTypeBlockMismatch(
                    prod.getProductType(), "privateTour", 0);
        }

        MarketRepository.MarketConfig config = market.config(marketCode)
                .orElseThrow(() -> new NotFoundException("market=" + marketCode));

        List<PriceTierInput> sortedTiers = tiers.stream()
                .sorted(Comparator.comparing(PriceTierInput::minPax))
                .toList();
        validateContiguous(sortedTiers);

        // Xoá MỀM bậc cũ, không xoá cứng: price_tier thuộc nhóm A của docs/11
        // mục 11.2, và một báo giá đã gửi cho khách tham chiếu tới bậc giá lúc
        // đó. Xoá cứng là làm báo giá cũ mất chỗ dựa.
        priceTierRepository.findByProductIdAndMarketAndSoftDeleteFalseOrderByMinPaxAsc(productId, marketCode)
                .forEach(oldTier -> oldTier.setSoftDelete(true));
        priceTierRepository.flush();

        List<PriceTierEntity> newEntities = new ArrayList<>();
        for (PriceTierInput tier : sortedTiers) {
            newEntities.add(new PriceTierEntity(UUID.randomUUID(), productId, marketCode,
                    tier.minPax(), tier.maxPax(), tier.pricePerPerson(), config.currency()));
        }
        priceTierRepository.saveAllAndFlush(newEntities);

        return list(productId, marketCode);
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
    private static void validateContiguous(List<PriceTierInput> sortedTiers) {
        for (int i = 0; i < sortedTiers.size(); i++) {
            PriceTierInput tier = sortedTiers.get(i);
            boolean isLastTier = i == sortedTiers.size() - 1;

            if (tier.maxPax() == null && !isLastTier) {
                throw new AdminErrors.PriceTierNotContiguous(
                        "chỉ bậc cuối được bỏ trống maxPax", tier.minPax());
            }
            if (tier.maxPax() != null && tier.maxPax() < tier.minPax()) {
                throw new AdminErrors.PriceTierNotContiguous(
                        "maxPax nhỏ hơn minPax", tier.minPax());
            }
            if (!isLastTier) {
                PriceTierInput nextTier = sortedTiers.get(i + 1);
                if (nextTier.minPax() != tier.maxPax() + 1) {
                    throw new AdminErrors.PriceTierNotContiguous(
                            "bậc sau phải bắt đầu ngay sau bậc trước", nextTier.minPax());
                }
            }
        }
    }
}
