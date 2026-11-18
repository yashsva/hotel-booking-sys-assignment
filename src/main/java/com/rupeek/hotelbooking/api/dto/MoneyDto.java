package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.common.Money;

import java.math.BigDecimal;

public record MoneyDto(BigDecimal amount, String currency) {

    public static MoneyDto from(Money money) {
        return money == null ? null : new MoneyDto(money.amount(), money.currency().getCurrencyCode());
    }
}
