package com.academy.paybridge.shared;

import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void alwaysHasTwoDecimalPlaces() {
        assertThat(Money.of(new BigDecimal("150.5"), Currency.NGN).amount()).isEqualByComparingTo("150.50");
        assertThat(Money.of(new BigDecimal("150.5"), Currency.NGN).amount().scale()).isEqualTo(2);
    }

    @Test
    void refusesToSilentlyRoundFractionsOfAKobo() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("10.005"), Currency.NGN))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void convertsToMinorUnits() {
        assertThat(Money.of(new BigDecimal("150.50"), Currency.NGN).toMinorUnits()).isEqualTo(15050L);
    }
}
