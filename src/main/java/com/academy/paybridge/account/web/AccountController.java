package com.academy.paybridge.account.web;

import com.academy.paybridge.account.api.AccountStatus;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.account.domain.AccountService;
import com.academy.paybridge.shared.money.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    public record OpenAccountRequest(
            @NotBlank @Size(max = 100) String holderName,
            @NotNull Currency currency) {
    }

    public record AccountResponse(
            UUID id, String accountNumber, String holderName,
            Currency currency, AccountStatus status, BigDecimal balance) {
    }

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody OpenAccountRequest request) {
        AccountView account = accounts.open(request.holderName(), request.currency());
        return toResponse(account);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return toResponse(accounts.getById(id));
    }

    private AccountResponse toResponse(AccountView a) {
        BigDecimal balance = accounts.balanceOf(a.id()).amount();
        return new AccountResponse(a.id(), a.accountNumber(), a.holderName(), a.currency(), a.status(), balance);
    }
}
