package vn.travel.booking.admin.dto;

/**
 * Một sản phẩm đứng ở đâu trong một thị trường.
 *
 * @param published công tắc "bắt đầu bán được" (docs/01 mục 4.4). Dịch xong mà
 *                  chưa bật thì sản phẩm vẫn không tồn tại với khách
 */
public record MarketState(String market, boolean published) {
}
