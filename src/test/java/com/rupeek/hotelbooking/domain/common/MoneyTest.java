package com.rupeek.hotelbooking.domain.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalisesScaleSoEqualAmountsAreEqual() {
        assertThat(Money.of("100", "INR")).isEqualTo(Money.of("100.00", "INR"));
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> Money.of("-1", "INR")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void arithmeticKeepsCurrency() {
        Money price = Money.of("2500", "INR");
        assertThat(price.times(3)).isEqualTo(Money.of("7500", "INR"));
        assertThat(price.percentage(50)).isEqualTo(Money.of("1250", "INR"));
        assertThat(price.plus(Money.of("0.50", "INR"))).isEqualTo(Money.of("2500.50", "INR"));
        assertThat(Money.of("100", "INR").dividedBy(3)).isEqualTo(Money.of("33.33", "INR"));
    }

    @Test
    void refusesToMixCurrencies() {
        assertThatThrownBy(() -> Money.of("1", "INR").plus(Money.of("1", "USD")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("currency mismatch");
    }
}
