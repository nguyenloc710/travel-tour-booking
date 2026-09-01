package vn.travel.booking.admin.dto;

import vn.travel.booking.common.money.Money;

import java.util.UUID;

public record PriceTierView(UUID id, String market, Short minPax, Short maxPax, Money pricePerPerson) {
}
