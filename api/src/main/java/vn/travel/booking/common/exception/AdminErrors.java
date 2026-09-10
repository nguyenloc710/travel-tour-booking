package vn.travel.booking.common.exception;

import vn.travel.booking.web.generated.model.FieldRule;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chín tình huống của đường <b>ghi</b> quản trị, mỗi cái một mã lỗi ở
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
 * <p>Cả chín đều là luật <b>liên trường</b> hoặc luật cần tra CSDL, tức là thứ
 * schema của OpenAPI không diễn đạt được. Luật một trường thì để
 * {@code @Valid} bắt và trả {@code VALIDATION_FAILED} như cũ.
 */
public final class AdminErrors {

    private AdminErrors() {
    }

    /** Gốc chung, chỉ để mang {@code params}. Không bắt trực tiếp lớp này. */
    public abstract static class WithParams extends RuntimeException {

        private final transient Map<String, Object> params;

        protected WithParams(String detail, Map<String, Object> params) {
            super(detail);
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
    public static class ProductTypeBlockMismatch extends WithParams {
        public ProductTypeBlockMismatch(String productType, String requiredBlocks, int blockCount) {
            super("cần đúng khối " + requiredBlocks + " cho " + productType + ", nhận " + blockCount,
                    Map.of("productType", productType, "expectedBlock", requiredBlocks, "blockCount", blockCount));
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
    public static class DurationDaysRuleViolated extends WithParams {
        public DurationDaysRuleViolated(String productType) {
            super("durationDays sai với " + productType,
                    Map.of("productType", productType));
        }
    }

    /** Hạng cabin chỉ có ở {@code CRUISE}. HTTP 400. */
    public static class CabinCategoryNotAllowed extends WithParams {
        public CabinCategoryNotAllowed(String productType) {
            super("cabinCategory không dùng được với " + productType,
                    Map.of("productType", productType));
        }
    }

    /** Mã loại khách không có trong thị trường này. HTTP 400. */
    public static class UnknownPaxType extends WithParams {
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
    public static class PriceTierNotContiguous extends WithParams {
        public PriceTierNotContiguous(String reason, Object atMinPax) {
            super("thang giá không liền mạch: " + reason,
                    Map.of("reason", reason, "atMinPax", atMinPax));
        }
    }

    /** Hạ sức chứa xuống dưới số chỗ đã bán. HTTP 409. */
    public static class CapacityBelowBooked extends WithParams {
        public CapacityBelowBooked(int capacity, int seatsBooked) {
            super("sức chứa " + capacity + " nhỏ hơn số chỗ đã bán " + seatsBooked,
                    Map.of("capacity", capacity, "seatsBooked", seatsBooked));
        }
    }

    /**
     * Xoá điểm đến còn sản phẩm trỏ tới. HTTP 409.
     *
     * <p>Không có gì nổ nếu cứ cho xoá — sản phẩm chỉ <b>lặng lẽ biến mất</b>
     * khỏi listing, vì truy vấn của nó {@code INNER JOIN destination_translation}.
     * Đó là kiểu hỏng tệ nhất: đúng chính sách không-fallback đang làm việc của
     * nó, nên không có lỗi nào ghi ra, và không ai nối được hai sự việc với nhau.
     */
    public static class DestinationInUse extends WithParams {
        public DestinationInUse(int productCount) {
            super("còn " + productCount + " sản phẩm trỏ tới điểm đến này",
                    Map.of("productCount", productCount));
        }
    }

    /**
     * Tắt hoặc gỡ vai trò của {@code ADMIN} đang bật cuối cùng. HTTP 409.
     *
     * <p>Không còn {@code ADMIN} nào đang bật thì không ai vào lại được màn hình
     * người dùng để sửa, và lối ra duy nhất là {@code UPDATE} tay trên cơ sở dữ
     * liệu lúc nửa đêm.
     */
    public static class LastAdmin extends WithParams {
        public LastAdmin(String detail) {
            super(detail, Map.of());
        }
    }

    /**
     * Xoá sản phẩm còn đơn đặt <b>chưa kết thúc</b>. HTTP 409.
     *
     * <p>Đó là dữ liệu của khách đang chờ đi, không phải nội dung biên tập. Muốn
     * ngừng bán thì tắt công tắc thị trường — thao tác đó không đụng tới đơn đã
     * đặt.
     */
    public static class ProductHasActiveBookings extends WithParams {
        public ProductHasActiveBookings(int bookingCount) {
            super("còn " + bookingCount + " đơn chưa kết thúc",
                    Map.of("activeBookings", bookingCount));
        }
    }

    /**
     * Luật <b>liên trường</b> mà schema của hợp đồng không diễn đạt được, nhưng
     * vẫn chỉ đích danh được một ô nhập: {@code guaranteedThreshold} so với
     * {@code minPax}, {@code validTo} so với {@code validFrom}.
     *
     * <p><b>Không có mã lỗi riêng cho từng luật.</b> Chúng đi ra dưới dạng
     * {@code VALIDATION_FAILED} kèm {@code fields}, giống hệt luật một trường —
     * vì với người nhập liệu chúng là cùng một chuyện: một ô cần sửa và một câu
     * nói vì sao. Mã riêng chỉ dành cho luật <b>không</b> chỉ được vào ô nào,
     * ví dụ {@code PRODUCT_TYPE_BLOCK_MISMATCH}.
     *
     * <p>Bắt ở tầng nghiệp vụ chứ không để CSDL ném: ràng buộc {@code CHECK}
     * nổi lên thành {@code 500} kèm {@code traceId}, và biên tập viên không đọc
     * được gì từ đó.
     *
     * <p>Giới hạn đi kèm là <b>giá trị thật lúc đó</b>, không phải hằng số:
     * {@code {"max": 10}} lấy từ chính {@code minPax} vừa nhập. Frontend dựng
     * câu từ đó nên không phải biết luật này tồn tại.
     */
    public static class FieldRulesViolated extends RuntimeException {

        /** Một ô sai, một luật, và giới hạn đọc được từ dữ liệu vừa nhập. */
        public record Issue(String path, FieldRule rule, Map<String, Object> params) {
        }

        private final transient List<Issue> issues;

        public FieldRulesViolated(List<Issue> issues) {
            super(issues.stream().map(Issue::path).collect(Collectors.joining(", ")));
            this.issues = List.copyOf(issues);
        }

        public List<Issue> issues() {
            return issues;
        }

        /**
         * Gom nhiều luật rồi ném <b>một lần</b>.
         *
         * <p>Ném ngay ở luật đầu tiên là bắt người nhập sửa từng cái một và gửi
         * lại từng lần — cùng lý do mà {@code @Valid} trả cả danh sách.
         */
        public static void throwIfAny(List<Issue> issues) {
            if (!issues.isEmpty()) {
                throw new FieldRulesViolated(issues);
            }
        }
    }
}
