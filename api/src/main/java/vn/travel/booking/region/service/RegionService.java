package vn.travel.booking.region.service;

import vn.travel.booking.region.dto.RegionSummary;
import vn.travel.booking.region.repository.RegionRepository;
import org.springframework.stereotype.Service;
import vn.travel.booking.market.service.MarketService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegionService {

    private final RegionRepository regions;
    private final MarketService markets;

    public RegionService(RegionRepository regions, MarketService markets) {
        this.regions = regions;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public List<RegionSummary> execute(String market, String locale) {
        // Thị trường chưa bật thì 404 ngay, không trả một danh sách rỗng —
        // rỗng và "không tồn tại" là hai câu trả lời khác nhau (docs/13 mục 3).
        markets.requireActive(market);
        return regions.findRegions(market, locale);
    }
}
