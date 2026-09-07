package vn.travel.booking.product.dto;

import vn.travel.booking.common.money.Money;

/**
 * Sản phẩm ở dạng rút gọn cho listing.
 *
 * <p>{@code durationDays} là {@code null} với {@code DAY_TOUR} — loại đó đo bằng
 * giờ. {@code priceFrom} là {@code null} khi thị trường này chưa có bảng giá;
 * frontend hiện "Liên hệ" chứ <b>không</b> hiện số 0.
 *
 * <p>{@code priceFrom} là giá <b>của thị trường này</b>, do người nhập, không
 * phải kết quả nhân tỷ giá — hệ thống này không có tỷ giá ở bất cứ đâu.
 */
public record ProductSummary(
        String slug,
        String title,
        ProductType productType,
        String shortDescription,
        String heroImage,
        String heroImageAlt,
        Integer durationDays,
        NamedRef region,
        NamedRef destination,
        Money priceFrom,
        boolean isNew,
        Double rating,
        int reviewCount) {
}
