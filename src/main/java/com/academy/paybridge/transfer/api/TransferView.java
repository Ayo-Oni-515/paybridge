package com.academy.paybridge.transfer.api;

import com.academy.paybridge.shared.money.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferView(
        UUID id,
        String reference,
        TransferType type,
        TransferStatus status,
        BigDecimal amount,
        Currency currency,
        UUID sourceAccountId,
        String destinationAccountNumber,
        String destinationBankCode,
        String destinationAccountName,
        String narration,
        String providerReference,
        String failureReason,
        Instant createdAt
) {}
