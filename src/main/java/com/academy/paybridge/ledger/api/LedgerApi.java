package com.academy.paybridge.ledger.api;

import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;

import java.util.List;

public interface LedgerApi {

    /**
     * Creates a ledger account if it doesn't exist yet.
     */
    void openAccount(String code, Currency currency, boolean allowNegative);

    Money balanceOf(String code);

    /**
     * Moves money as one double-entry journal: a DEBIT line on {@code fromCode}
     * and an equal CREDIT line on {@code toCode}.
     * Must be called inside the caller's transaction so it commits or rolls back with it.
     */
    void move(String journalReference, String fromCode, String toCode, Money amount, String description);

    /**
     * One line per currency. Any non-zero total means a bug.
     */
    List<TrialBalanceLine> trialBalance();
}
