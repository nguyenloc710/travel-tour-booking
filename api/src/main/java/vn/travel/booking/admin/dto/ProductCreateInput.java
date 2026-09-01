package vn.travel.booking.admin.dto;

import vn.travel.booking.product.dto.ProductTypeBlocks;

import java.util.UUID;

/**
 * Tạo sản phẩm: phần chung, phần riêng của loại, và bản dịch <b>ngôn ngữ
 * nguồn</b> — cả ba trong một lời gọi vì cả ba phải nằm trong một transaction.
 */
public record ProductCreateInput(
        String productType,
        UUID primaryDestinationId,
        Short durationDays,
        String heroImage,
        String mapImage,
        Boolean isNew,
        UUID consultantId,
        ProductTranslationInput source,
        ProductTypeBlocks blocks) {
}
