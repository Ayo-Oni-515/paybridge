package com.academy.paybridge.remittance.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Sent by a partner IMTO. The USD amount and naira amount come from the quote, not from here,
 * so a partner can't change the price after the quote was given.
 */
public record CreateRemittanceRequest(
        @NotBlank @Size(max = 32) String partnerCode,
        @NotBlank @Size(max = 64) String partnerReference,   // partner's own ID; doubles as idempotency key
        @NotNull UUID quoteId,
        @NotBlank @Size(max = 100) String senderName,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO country code like GB") String senderCountry,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String beneficiaryAccountNumber,
        @Valid BankPayout payoutBank    // optional: forward the naira to a bank account
) {
    public record BankPayout(
            @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String accountNumber,
            @NotBlank @Size(max = 10) String bankCode) {
    }
}
