package vn.travel.booking.pricing.service;

import vn.travel.booking.common.exception.PartySizeOutOfRangeException;
import vn.travel.booking.pricing.dto.PriceTier;
import vn.travel.booking.common.money.Money;

import java.util.List;

/**
 * Tra bảng giá theo bậc — docs/14 mục 2.3.
 */
public final class PriceTiers {

    private PriceTiers() {
    }

    /**
     * Bậc chọn theo <b>tổng số khách, kể cả trẻ em</b>. Giảm theo loại khách áp
     * <b>sau</b> khi đã chọn bậc: trẻ em vẫn tính vào số khách để chọn bậc, nhưng
     * trả theo tỷ lệ của loại mình.
     *
     * <p>Bỏ trẻ em ra khỏi số khách khi chọn bậc là làm một nhóm bốn người thành
     * nhóm hai người, và trả đơn giá của bậc nhỏ — đắt hơn hẳn, và không ai hiểu
     * vì sao.
     */
    public static Money donGiaMoiNguoi(List<PriceTier> bac, int tongSoKhach) {
        return bac.stream()
                .filter(b -> b.chua(tongSoKhach))
                .findFirst()
                .map(PriceTier::pricePerPerson)
                .orElseThrow(() -> new PartySizeOutOfRangeException(tongSoKhach));
    }
}
