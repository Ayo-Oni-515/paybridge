package com.academy.paybridge.transfer.api;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ExternalTransferRequest(
        @NotNull UUID sourceAccountId,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String destinationAccountNumber,
        @NotBlank @Size(max = 10) String bankCode,   // e.g. "058" for GTBank
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @Size(max = 100) String narration
) {
}
