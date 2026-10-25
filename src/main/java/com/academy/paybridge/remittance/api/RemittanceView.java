package com.academy.paybridge.remittance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RemittanceView(
        UUID id,
        String partnerCode,
        String partnerReference,
        String senderName,
        String senderCountry,
        UUID quoteId,
        BigDecimal usdAmount,
        BigDecimal ngnAmount,
        BigDecimal customerRate,
        UUID beneficiaryAccountId,
        String beneficiaryAccountNumber,
        String beneficiaryName,
        RemittanceStatus status,
        String payoutAccountNumber,
        String payoutBankCode,
        UUID payoutTransferId,
        String failureReason,
        UUID settlementId,    // null until the partner has paid us for it
        Instant createdAt
) {}
