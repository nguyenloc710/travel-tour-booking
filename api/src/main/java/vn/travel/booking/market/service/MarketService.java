package vn.travel.booking.market.service;

import vn.travel.booking.market.repository.MarketRepository;
import org.springframework.stereotype.Service;
import vn.travel.booking.common.exception.NotFoundException;

/**
 * Cổng chặn thị trường, dùng chung cho mọi use case công khai.
 *
 * <p>Thị trường không tồn tại hoặc {@code is_active = false} thì <b>404</b>,
 * không phải 400 — docs/13 mục 3. Lý do: `/api/v1/xx/products` với thị trường
 * chưa mở là một tài nguyên không tồn tại, chứ không phải một yêu cầu sai cú
 * pháp. Trả 400 còn tiết lộ rằng mã đó "đúng dạng nhưng chưa bật".
 */
@Service
public class MarketService {

    private final MarketRepository markets;

    public MarketService(MarketRepository markets) {
        this.markets = markets;
    }

    public void requireActive(String market) {
        if (!markets.isActive(market)) {
            throw new NotFoundException("thị trường không tồn tại hoặc chưa bật: " + market);
        }
    }
}
