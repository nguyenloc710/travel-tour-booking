package vn.travel.booking.common.exception;

import java.util.Map;

/**
 * Bảy tình huống của đường <b>ghi</b> quản trị, mỗi cái một mã lỗi ở
 * docs/13 mục 5.1.
 *
 * <p>Cùng khuôn với {@link BookingErrors}: một lớp cho một mã, không một lớp
 * mang mã. Mã lỗi là thứ frontend dịch, còn kiểu ngoại lệ là thứ trình biên dịch
 * kiểm.
 *
 * <p>Khác {@code BookingErrors} ở một điểm: mấy lỗi này <b>mang tham số</b>.
 * "Sai dữ liệu" không giúp gì cho biên tập viên; "thiếu khối {@code groupTour}"
 * hay "đã bán 8 chỗ, không hạ sức chứa xuống 5 được" thì sửa được ngay. Tham số
 * là dữ liệu, không phải câu tiếng người — frontend vẫn là chỗ dịch.
 *
 * <p>Cả bảy đều là luật <b>liên trường</b> hoặc luật cần tra CSDL, tức là thứ
 * schema của OpenAPI không diễn đạt được. Luật một trường thì để
 * {@code @Valid} bắt và trả {@code VALIDATION_FAILED} như cũ.
 */
public final class AdminErrors {

    private AdminErrors() {
    }

    /** Gốc chung, chỉ để mang {@code params}. Không bắt trực tiếp lớp này. */
    public abstract static class CoThamSo extends RuntimeException {

        private final transient Map<String, Object> params;

        protected CoThamSo(String chiTiet, Map<String, Object> params) {
            super(chiTiet);
            this.params = params;
        }

        public Map<String, Object> params() {
            return params;
        }
    }

    /**
     * Khối riêng của loại không khớp {@code productType}: thiếu, sai, hoặc gửi
     * hai khối. HTTP 400.
     */
    public static class ProductTypeBlockMismatch extends CoThamSo {
        public ProductTypeBlockMismatch(String productType, String khoiCanCo, int soKhoi) {
            super("cần đúng khối " + khoiCanCo + " cho " + productType + ", nhận " + soKhoi,
                    Map.of("productType", productType, "expectedBlock", khoiCanCo, "blockCount", soKhoi));
        }
    }

    /**
     * {@code durationDays} phải rỗng với {@code DAY_TOUR} và có với mọi loại
     * khác — ràng buộc {@code ck_product_duration} cưỡng chế cả hai chiều.
     * HTTP 400.
     *
     * <p>Bắt ở tầng nghiệp vụ chứ không để CSDL ném: lỗi ràng buộc nổi lên thành
     * {@code 500} kèm {@code traceId}, và biên tập viên không đọc được gì từ đó.
     */
    public static class DurationDaysRuleViolated extends CoThamSo {
        public DurationDaysRuleViolated(String productType) {
            super("durationDays sai với " + productType,
                    Map.of("productType", productType));
        }
    }

    /** Hạng cabin chỉ có ở {@code CRUISE}. HTTP 400. */
    public static class CabinCategoryNotAllowed extends CoThamSo {
        public CabinCategoryNotAllowed(String productType) {
            super("cabinCategory không dùng được với " + productType,
                    Map.of("productType", productType));
        }
    }

    /** Mã loại khách không có trong thị trường này. HTTP 400. */
    public static class UnknownPaxType extends CoThamSo {
        public UnknownPaxType(String paxTypeCode, String market) {
            super("không có loại khách " + paxTypeCode + " ở thị trường " + market,
                    Map.of("paxTypeCode", paxTypeCode, "market", market));
        }
    }

    /**
     * Thang giá hở hoặc chồng. HTTP 400.
     *
     * <p>Bậc phải liền mạch: bậc sau bắt đầu ngay sau bậc trước, và đúng một bậc
     * cuối không có trần. Một khoảng hở nghĩa là có số khách mà hệ thống không
     * tính ra giá — và không ai phát hiện cho tới khi đúng nhóm khách đó hỏi.
     */
    public static class PriceTierNotContiguous extends CoThamSo {
        public PriceTierNotContiguous(String lyDo, Object taiMinPax) {
            super("thang giá không liền mạch: " + lyDo,
                    Map.of("reason", lyDo, "atMinPax", taiMinPax));
        }
    }

    /** Hạ sức chứa xuống dưới số chỗ đã bán. HTTP 409. */
    public static class CapacityBelowBooked extends CoThamSo {
        public CapacityBelowBooked(int capacity, int seatsBooked) {
            super("sức chứa " + capacity + " nhỏ hơn số chỗ đã bán " + seatsBooked,
                    Map.of("capacity", capacity, "seatsBooked", seatsBooked));
        }
    }

    /**
     * Xoá sản phẩm còn đơn đặt <b>chưa kết thúc</b>. HTTP 409.
     *
     * <p>Đó là dữ liệu của khách đang chờ đi, không phải nội dung biên tập. Muốn
     * ngừng bán thì tắt công tắc thị trường — thao tác đó không đụng tới đơn đã
     * đặt.
     */
    public static class ProductHasActiveBookings extends CoThamSo {
        public ProductHasActiveBookings(int soDon) {
            super("còn " + soDon + " đơn chưa kết thúc",
                    Map.of("activeBookings", soDon));
        }
    }
}
