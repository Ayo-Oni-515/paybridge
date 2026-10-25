package com.academy.paybridge.settlement.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The partner's daily report plus the wire we received in our dollar (domiciliary) account.
 * In real life the report is a file (CSV/SFTP) and the wire comes from the bank statement.
 */
public record SettlementRequest(
        @NotBlank @Size(max = 32) String partnerCode,
        @NotNull LocalDate settlementDate,
        @NotBlank @Size(max = 64) String bankReference,     // the wire's reference on our bank statement
        @NotNull @DecimalMin("0.00") @Digits(integer = 15, fraction = 2) BigDecimal wireAmount,
        @NotNull @Size(max = 10_000) List<@Valid Line> lines
) {
    public record Line(
            @NotBlank @Size(max = 64) String partnerReference,
            @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal usdAmount) {
    }
}
