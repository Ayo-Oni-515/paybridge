package com.academy.paybridge.settlement.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SettlementView(
        UUID id,
        String partnerCode,
        LocalDate settlementDate,
        String bankReference,
        BigDecimal wireAmount,
        BigDecimal reportedTotal,
        int matchedCount,
        BigDecimal matchedTotal,
        SettlementStatus status,
        BigDecimal receivableAfter,   // negative = partner still owes us this much
        List<BreakView> breaks,
        Instant createdAt
) {}
