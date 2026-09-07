package vn.travel.booking.admin.dto;

import java.math.BigDecimal;

/** {@code maxPax} rỗng = bậc cuối, không có trần. Đúng một bậc được phép như vậy. */
public record PriceTierInput(Short minPax, Short maxPax, BigDecimal pricePerPerson) {
}
