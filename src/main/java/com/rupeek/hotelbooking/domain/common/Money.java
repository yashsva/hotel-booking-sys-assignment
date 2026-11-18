package com.rupeek.hotelbooking.domain.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import static com.rupeek.hotelbooking.domain.common.Preconditions.require;
import static com.rupeek.hotelbooking.domain.common.Preconditions.requireNonNull;

/** Immutable, non-negative monetary amount in a single currency. */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    private static final int SCALE = 2;

    public Money {
        requireNonNull(amount, "amount");
        requireNonNull(currency, "currency");
        require(amount.signum() >= 0, "amount must not be negative");
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        ensureSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(long multiplier) {
        return new Money(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    public Money percentage(int percent) {
        require(percent >= 0, "percent must not be negative");
        return new Money(amount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP), currency);
    }

    public Money dividedBy(long divisor) {
        require(divisor > 0, "divisor must be positive");
        return new Money(amount.divide(BigDecimal.valueOf(divisor), SCALE, RoundingMode.HALF_UP), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public int compareTo(Money other) {
        ensureSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void ensureSameCurrency(Money other) {
        require(currency.equals(other.currency), "currency mismatch: " + currency + " vs " + other.currency);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}
