package vn.travel.booking.product.dto;

import vn.travel.booking.common.money.Money;

import java.util.List;

/**
 * Sản phẩm ở dạng đầy đủ cho trang chi tiết: phần chung cộng <b>đúng một</b>
 * phần riêng theo loại.
 *
 * <p>{@code longDescription} là danh sách đoạn văn, không phải một khối HTML.
 * Nội dung do biên tập viên nhập, và HTML tự do trong CSDL là một lỗ chèn mã
 * chờ sẵn (docs/12).
 *
 * <p>{@code gallery} có thể <b>rỗng</b> — sản phẩm chưa có ảnh nào là trạng thái
 * hợp lệ, không phải lỗi. {@code layout} có thể <b>null</b>: nghĩa là chưa ai
 * chọn template, và frontend dùng mặc định của loại.
 */
public record ProductDetail(
        String slug,
        String title,
        ProductType productType,
        String shortDescription,
        List<String> longDescription,
        List<String> whyChooseThis,
        String heroImage,
        String heroImageAlt,
        String mapImage,
        List<GalleryImage> gallery,
        String layout,
        Integer durationDays,
        NamedRef region,
        NamedRef destination,
        Money priceFrom,
        boolean isNew,
        Double rating,
        int reviewCount,
        ProductVariant variant) {
}
