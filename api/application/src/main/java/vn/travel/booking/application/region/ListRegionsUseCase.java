package vn.travel.booking.application.region;

import org.springframework.stereotype.Service;
import vn.travel.booking.application.market.Markets;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListRegionsUseCase {

    private final RegionQueryPort regions;
    private final Markets markets;

    public ListRegionsUseCase(RegionQueryPort regions, Markets markets) {
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
