package com.academy.paybridge.fx;

import com.academy.paybridge.fx.domain.RateCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RateCalculatorTest {

    @Test
    void customerRateIsMidRateMinusSpread() {
        BigDecimal rate = RateCalculator.customerRate(new BigDecimal("1520.00"), new BigDecimal("1.5"));
        assertThat(rate).isEqualByComparingTo("1497.20");
    }

    @Test
    void zeroSpreadGivesTheMarketRate() {
        assertThat(RateCalculator.customerRate(new BigDecimal("1520.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("1520.00");
    }

    @Test
    void conversionRoundsDownToTheKobo() {
        // 33.33 x 1497.2 = 49901.676 -> 49901.67, never 49901.68
        assertThat(RateCalculator.convert(new BigDecimal("33.33"), new BigDecimal("1497.2000")))
                .isEqualByComparingTo("49901.67");
    }
}
