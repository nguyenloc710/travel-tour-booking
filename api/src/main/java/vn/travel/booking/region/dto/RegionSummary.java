package vn.travel.booking.region.dto;

/**
 * Một miền kèm số sản phẩm, đã lọc theo cặp (market, locale).
 *
 * <p>{@code productCount} đếm theo <b>sản phẩm</b>, không cộng dồn từng điểm đến:
 * một tour ghé Hà Nội, Hạ Long và Ninh Bình chỉ được tính một lần cho miền Bắc.
 * Cộng dồn ra con số to hơn thực tế và khách phát hiện ngay khi bấm vào.
 */
public record RegionSummary(String slug, String name, int productCount) {
}
