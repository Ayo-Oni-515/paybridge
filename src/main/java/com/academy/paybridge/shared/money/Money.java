package com.academy.paybridge.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount in a specific currency, always with exactly 2 decimal places (kobo / cents).
 * Never use double for money: 0.1 + 0.2 != 0.3 in floating point.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        // Throws ArithmeticException for 10.005 - we never silently round money.
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    /**
     * Payment APIs usually want minor units: NGN 150.50 -> 15050 kobo.
     */
    public long toMinorUnits() {
        return amount.movePointRight(2).longValueExact();
    }

    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }
}
