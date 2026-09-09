package vn.travel.booking.product.dto;

import java.time.LocalDate;

/**
 * Sáu khối riêng của sáu loại sản phẩm. <b>Đúng một khối được phép khác
 * {@code null}</b>, và nó phải khớp {@code productType}.
 *
 * <p>Gom sáu khối vào một bản ghi thay vì sáu tham số riêng để chỗ kiểm tra
 * "đúng một khối" có thứ để kiểm: đếm được số khối khác {@code null} thì mới
 * phân biệt được "thiếu khối" với "gửi hai khối", và hai lỗi đó cần hai thông
 * báo khác nhau.
 *
 * <p>Kiểu {@code Short} chứ không {@code Integer} vì cột là {@code SMALLINT}.
 * Hibernate chạy {@code ddl-auto: validate} nên lệch kiểu là ứng dụng không khởi
 * động được — sai sớm, đúng chỗ.
 */
public record ProductTypeBlocks(
        GroupTour groupTour,
        IndividualPackage individualPackage,
        PrivateTour privateTour,
        Cruise cruise,
        Combo combo,
        DayTour dayTour) {

    public record GroupTour(Short minPax, Short maxPax, Short guaranteedThreshold,
                            String tourLeaderLanguage, Short fitnessLevel) {
    }

    public record IndividualPackage(Short minPartySize, Short flexibleDateWindowDays) {
    }

    public record PrivateTour(Short leadTimeDays, Short quoteValidDays) {
    }

    public record Cruise(String shipName, Short portCount) {
    }

    public record Combo(Short nights, LocalDate validFrom, LocalDate validTo) {
    }

    public record DayTour(Short durationHours, Short cutoffHours) {
    }

    /** Rỗng hoàn toàn — dùng cho {@code PATCH} không đụng tới phần riêng của loại. */
    public boolean trong() {
        return blockCount() == 0;
    }

    public int blockCount() {
        int n = 0;
        if (groupTour != null) {
            n++;
        }
        if (individualPackage != null) {
            n++;
        }
        if (privateTour != null) {
            n++;
        }
        if (cruise != null) {
            n++;
        }
        if (combo != null) {
            n++;
        }
        if (dayTour != null) {
            n++;
        }
        return n;
    }

    /** Tên khối khớp với một {@code productType} — dùng cho thông báo lỗi. */
    public static String blockName(String productType) {
        return switch (productType) {
            case "GROUP_TOUR" -> "groupTour";
            case "INDIVIDUAL_PACKAGE" -> "individualPackage";
            case "PRIVATE_TOUR" -> "privateTour";
            case "CRUISE" -> "cruise";
            case "COMBO" -> "combo";
            case "DAY_TOUR" -> "dayTour";
            default -> throw new IllegalArgumentException("loại sản phẩm lạ: " + productType);
        };
    }

    public Object blocksFor(String productType) {
        return switch (productType) {
            case "GROUP_TOUR" -> groupTour;
            case "INDIVIDUAL_PACKAGE" -> individualPackage;
            case "PRIVATE_TOUR" -> privateTour;
            case "CRUISE" -> cruise;
            case "COMBO" -> combo;
            case "DAY_TOUR" -> dayTour;
            default -> throw new IllegalArgumentException("loại sản phẩm lạ: " + productType);
        };
    }
}
