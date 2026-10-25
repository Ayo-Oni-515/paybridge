package com.academy.paybridge.account.domain;

import com.academy.paybridge.account.api.AccountApi;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.account.repository.AccountRepository;
import com.academy.paybridge.customer.api.CustomerApi;
import com.academy.paybridge.customer.api.CustomerStatus;
import com.academy.paybridge.customer.api.CustomerView;
import com.academy.paybridge.customer.api.KycTier;
import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.shared.exception.BusinessException;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService implements AccountApi {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final LedgerApi ledger;
    private final CustomerApi customers;

    public AccountService(AccountRepository accounts, LedgerApi ledger, CustomerApi customers) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.customers = customers;
    }

    /**
     * Opens the account and its matching ledger account in one transaction.
     * One account per currency per customer. USD (domiciliary) needs TIER_2 (verified BVN).
     */
    @Transactional
    public AccountView open(UUID customerId, Currency currency) {
        CustomerView customer = customers.getById(customerId);
        if (customer.status() != CustomerStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "CUSTOMER_INACTIVE",
                    "Customer is " + customer.status());
        }
        if (currency == Currency.USD && !customer.kycTier().isAtLeast(KycTier.TIER_2)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "KYC_TIER_TOO_LOW",
                    "USD (domiciliary) accounts need a verified BVN (TIER_2)");
        }
        if (accounts.existsByCustomerIdAndCurrency(customerId, currency)) {
            throw new BusinessException(HttpStatus.CONFLICT, "ACCOUNT_EXISTS",
                    "Customer already has a " + currency + " account");
        }

        Account account = accounts.save(new Account(newAccountNumber(), customerId, customer.fullName(), currency));
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
    @Transactional(readOnly = true)
    public List<AccountView> listByCustomer(UUID customerId) {
        return accounts.findByCustomerIdOrderByCreatedAt(customerId).stream().map(Account::toView).toList();
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

    /** DEV ONLY: gives an account test money from the funding account of its currency. */
    @Transactional
    public Money devDeposit(UUID accountId, BigDecimal amount) {
        AccountView account = getById(accountId);
        String funding = account.currency() == Currency.USD ? SystemAccounts.USD_FUNDING : SystemAccounts.NGN_FUNDING;
        ledger.move("dev-deposit-" + UUID.randomUUID(), funding,
                ledgerCodeOf(accountId), Money.of(amount, account.currency()), "Test deposit");
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
