package vn.travel.booking.quote.dto;

import vn.travel.booking.common.money.Money;

import java.math.BigDecimal;

/** Một dòng bảng giá đọc lên từ {@code quote_line}. */
public record QuoteLineRow(
        int seq,
        String labelKey,
        BigDecimal quantity,
        Money unitAmount,
        Money amount) {
}
