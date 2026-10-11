package com.academy.paybridge.account.api;

import com.academy.paybridge.shared.money.Currency;

import java.util.UUID;

/**
 * Read-only snapshot of an account, safe to hand to other modules.
 */
public record AccountView(
        UUID id,
        String accountNumber,
        String holderName,
        Currency currency,
        AccountStatus status
) {
}