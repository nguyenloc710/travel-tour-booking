package vn.travel.booking.admin.dto;

import vn.travel.booking.product.dto.ProductTypeBlocks;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Một sản phẩm nhìn từ trang quản trị: mọi bản dịch, mọi thị trường. */
public record ProductDetailView(
        UUID id,
        String productType,
        UUID primaryDestinationId,
        Short durationDays,
        String heroImage,
        String mapImage,
        boolean isNew,
        BigDecimal rating,
        int reviewCount,
        UUID consultantId,
        List<MarketState> markets,
        List<TranslationState> translations,
        ProductTypeBlocks blocks,
        OffsetDateTime lastModifiedAt,
        UUID lastModifiedBy) {
}
