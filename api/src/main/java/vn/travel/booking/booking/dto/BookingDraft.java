package vn.travel.booking.booking.dto;

import vn.travel.booking.pricing.dto.PriceBreakdown;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Đơn sắp được tạo, cùng bảng giá đã tính.
 *
 * <p>{@code productTitle} và {@code breakdown} là <b>bản chụp</b>: đơn giữ bản
 * sao của những gì đã bán, nên đổi bảng giá hay sửa tên tour sau này không làm
 * đổi đơn cũ (docs/23 mục 4.1).
 */
public record BookingDraft(
        String market,
        String locale,
        UUID productId,
        UUID departureId,
        UUID seatHoldId,
        String productTitle,
        Map<String, Integer> pax,
        List<PassengerDraft> passengers,
        String contactEmail,
        String contactPhone,
        PriceBreakdown breakdown) {

    public int totalPaxCount() {
        return pax.values().stream().mapToInt(Integer::intValue).sum();
    }
}
