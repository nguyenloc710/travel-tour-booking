package vn.travel.booking.domain.pricing;

/**
 * Tám dòng của bảng phân rã, <b>theo đúng thứ tự cộng dồn</b> của docs/14 mục 2.1.
 *
 * <p>Thứ tự khai báo ở đây <b>là</b> thứ tự tính: engine duyệt enum này. Đảo hai
 * hằng số là đổi con số cuối cùng khách phải trả — đặc biệt hai dòng cuối, vì
 * giảm đặt sớm trừ <b>trước</b> phí xử lý.
 */
public enum PriceLineKind {
    BASE,
    SINGLE_SUPPLEMENT,
    CABIN_UPGRADE,
    DEPARTURE_ORIGIN,
    INSURANCE,
    PRE_TOUR_HOTEL,
    EARLY_BIRD_DISCOUNT,
    PROCESSING_FEE
}
