package com.academy.paybridge.account.domain;

import com.academy.paybridge.account.api.AccountStatus;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.shared.money.Currency;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Account details only. The balance lives in the ledger, so there is one source of truth.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String accountNumber;

    @Column(nullable = false)
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Account() {}

    Account(String accountNumber, String holderName, Currency currency) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.currency = currency;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }

    AccountView toView() {
        return new AccountView(id, accountNumber, holderName, currency, status);
    }
}
