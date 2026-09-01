package vn.travel.booking.admin.dto;

import vn.travel.booking.common.money.Money;

public record DeparturePriceView(String paxTypeCode, String occupancy, Money amount) {
}
