package com.academy.paybridge.account.web;

import com.academy.paybridge.account.domain.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Test-only endpoints. Disabled unless paybridge.dev-tools.enabled=true.
 */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "paybridge.dev-tools.enabled", havingValue = "true")
public class DevController {

    public record DepositRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount) {
    }

    private final AccountService accounts;

    public DevController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/accounts/{id}/deposit")
    public Map<String, Object> deposit(@PathVariable UUID id, @Valid @RequestBody DepositRequest request) {
        BigDecimal balance = accounts.devDeposit(id, request.amount()).amount();
        return Map.of("accountId", id, "balance", balance);
    }
}
