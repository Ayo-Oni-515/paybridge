package com.academy.paybridge.ledger.api;

import com.academy.paybridge.shared.money.Currency;

import java.math.BigDecimal;

/**
 * Sum of every ledger account in one currency. Double entry means this must always be 0.
 */
public record TrialBalanceLine(Currency currency, BigDecimal total) {

    public boolean isBalanced() {
        return total.signum() == 0;
    }
}
