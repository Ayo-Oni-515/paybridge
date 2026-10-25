package com.academy.paybridge.fx.api;

import com.academy.paybridge.shared.money.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * "$100 = NGN 149,720.00 if you commit before expiresAt". The market (mid) rate is not exposed.
 */
public record QuoteView(
        UUID id,
        BigDecimal sourceAmount,
        Currency sourceCurrency,
        BigDecimal targetAmount,
        Currency targetCurrency,
        BigDecimal customerRate,
        Instant expiresAt,
        QuoteStatus status
) {
}
