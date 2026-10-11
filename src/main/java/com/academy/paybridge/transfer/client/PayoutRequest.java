package com.academy.paybridge.transfer.client;

import com.academy.paybridge.shared.money.Money;

public record PayoutRequest(
        String reference,        // our unique reference; the provider echoes it back in webhooks
        String accountName,
        String accountNumber,
        String bankCode,
        Money amount,
        String narration
) {}
