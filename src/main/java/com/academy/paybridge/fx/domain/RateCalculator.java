package com.academy.paybridge.fx.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure maths, no Spring: easy to unit test. */
public final class RateCalculator {

    private RateCalculator() {}

    /**
     * The customer gets fewer naira per dollar than the market rate. That gap is the spread.
     * 1520.00 with a 1.5% spread -> 1497.2000
     */
    public static BigDecimal customerRate(BigDecimal midRate, BigDecimal spreadPercent) {
        BigDecimal factor = BigDecimal.ONE.subtract(spreadPercent.movePointLeft(2));
        return midRate.multiply(factor).setScale(4, RoundingMode.DOWN);
    }

    /**
     * Rounds DOWN to 2 decimal places: we never pay out a fraction of a kobo we haven't got.
     * 100.00 x 1497.2000 -> 149720.00
     */
    public static BigDecimal convert(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(2, RoundingMode.DOWN);
    }
}
