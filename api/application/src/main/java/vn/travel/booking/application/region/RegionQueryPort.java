package vn.travel.booking.application.region;

import java.util.List;

/**
 * Cổng ra ngoài do tầng application khai báo, infrastructure hiện thực.
 *
 * <p>{@code market} và {@code locale} là <b>hai tham số riêng</b> và luôn phải
 * riêng: market quyết định khách mua gì, locale quyết định khách đọc bằng tiếng
 * gì. Không bao giờ suy cái này từ cái kia.
 */
public interface RegionQueryPort {

    List<RegionSummary> findRegions(String market, String locale);
}
