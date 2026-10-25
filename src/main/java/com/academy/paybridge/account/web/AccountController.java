package com.academy.paybridge.account.web;

import com.academy.paybridge.account.api.AccountStatus;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.account.domain.AccountService;
import com.academy.paybridge.shared.money.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    /**
     * The holder name now comes from the customer's KYC profile, not from the request.
     */
    public record OpenAccountRequest(
            @NotNull UUID customerId,
            @NotNull Currency currency) {
    }

    public record AccountResponse(
            UUID id, String accountNumber, UUID customerId, String holderName,
            Currency currency, AccountStatus status, BigDecimal balance) {
    }

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody OpenAccountRequest request) {
        return toResponse(accounts.open(request.customerId(), request.currency()));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return toResponse(accounts.getById(id));
    }

    /**
     * GET /api/accounts?customerId=...
     */
    @GetMapping
    public List<AccountResponse> byCustomer(@RequestParam UUID customerId) {
        return accounts.listByCustomer(customerId).stream().map(this::toResponse).toList();
    }

    private AccountResponse toResponse(AccountView a) {
        BigDecimal balance = accounts.balanceOf(a.id()).amount();
        return new AccountResponse(a.id(), a.accountNumber(), a.customerId(), a.holderName(),
                a.currency(), a.status(), balance);
    }
}
