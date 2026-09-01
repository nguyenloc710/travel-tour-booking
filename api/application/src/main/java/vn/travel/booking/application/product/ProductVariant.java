package vn.travel.booking.application.product;

import java.time.LocalDate;

/**
 * Phần riêng của từng loại sản phẩm — ADR-005, mỗi loại một bảng con.
 *
 * <p>{@code sealed} là có chủ ý: thêm loại thứ bảy mà quên xử lý ở một chỗ nào
 * đó thì {@code switch} không còn đủ nhánh và <b>trình biên dịch báo đỏ</b>.
 * Với một hệ thống mà mỗi loại có ràng buộc nghiệp vụ khác nhau, quên một nhánh
 * là bán sai sản phẩm chứ không phải hiển thị xấu.
 */
public sealed interface ProductVariant {

    /** Đoàn ghép có trưởng đoàn. */
    record GroupTour(
            int minPax,
            int maxPax,
            int guaranteedThreshold,
            /* Ngôn ngữ của TRƯỞNG ĐOÀN, không phải locale khách đang đọc. */
            String tourLeaderLanguage,
            int fitnessLevel) implements ProductVariant {
    }

    /** Gói cá nhân, khách tự chọn ngày trong cửa sổ linh hoạt. */
    record IndividualPackage(
            int minPartySize,
            int flexibleDateWindowDays) implements ProductVariant {
    }

    /** Tour riêng — không đặt trực tiếp được, chỉ yêu cầu báo giá. */
    record PrivateTour(
            int leadTimeDays,
            int quoteValidDays) implements ProductVariant {
    }

    /** Du thuyền. */
    record Cruise(
            String shipName,
            int portCount) implements ProductVariant {
    }

    /** Bó cố định do nhân viên soạn, chỉ bán trong khoảng ngày hiệu lực. */
    record Combo(
            int nights,
            LocalDate validFrom,
            LocalDate validTo) implements ProductVariant {
    }

    /** Tour trong ngày — đo bằng giờ, nên không có số ngày. */
    record DayTour(
            int durationHours,
            int cutoffHours) implements ProductVariant {
    }
}
