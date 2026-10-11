package com.academy.paybridge.account.domain;

import com.academy.paybridge.account.api.AccountApi;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.account.repository.AccountRepository;
import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import com.academy.paybridge.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Service
public class AccountService implements AccountApi {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final LedgerApi ledger;

    public AccountService(AccountRepository accounts, LedgerApi ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    /** Opens the account and its matching ledger account in one transaction. */
    @Transactional
    public AccountView open(String holderName, Currency currency) {
        Account account = accounts.save(new Account(newAccountNumber(), holderName.trim(), currency));
        ledger.openAccount(ledgerCodeOf(account.getId()), currency, false);
        return account.toView();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView getById(UUID accountId) {
        return accounts.findById(accountId)
                .map(Account::toView)
                .orElseThrow(() -> notFound("Account not found: " + accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView getByNumber(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber)
                .map(Account::toView)
                .orElseThrow(() -> notFound("No PayBridge account with number " + accountNumber));
    }

    @Override
    public String ledgerCodeOf(UUID accountId) {
        return "ACC-" + accountId;
    }

    @Transactional(readOnly = true)
    public Money balanceOf(UUID accountId) {
        getById(accountId); // 404 if the account doesn't exist
        return ledger.balanceOf(ledgerCodeOf(accountId));
    }

    /** DEV ONLY: gives an account test money from the funding account. */
    @Transactional
    public Money devDeposit(UUID accountId, BigDecimal amount) {
        AccountView account = getById(accountId);
        if (account.currency() != Currency.NGN) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CURRENCY_NOT_SUPPORTED",
                    "Test deposits are NGN only for now");
        }
        ledger.move("dev-deposit-" + UUID.randomUUID(), SystemAccounts.NGN_FUNDING,
                ledgerCodeOf(accountId), Money.of(amount, Currency.NGN), "Test deposit");
        return ledger.balanceOf(ledgerCodeOf(accountId));
    }

    private String newAccountNumber() {
        String number;
        do {
            number = String.format("%010d", RANDOM.nextLong(10_000_000_000L));
        } while (accounts.existsByAccountNumber(number));
        return number;
    }

    private static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", message);
    }
}
