package com.academy.paybridge.ledger.domain;

import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One line in the record book. Never updated or deleted: mistakes are fixed with new entries.
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_entry_account", columnList = "accountCode"),
        @Index(name = "idx_entry_journal", columnList = "journalReference")
})
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String journalReference;

    @Column(nullable = false, length = 64)
    private String accountCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    LedgerEntry(String journalReference, String accountCode, EntryDirection direction, Money money, String description) {
        this.journalReference = journalReference;
        this.accountCode = accountCode;
        this.direction = direction;
        this.amount = money.amount();
        this.currency = money.currency();
        this.description = description;
        this.createdAt = Instant.now();
    }
}
