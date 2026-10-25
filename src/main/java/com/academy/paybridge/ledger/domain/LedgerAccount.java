package com.academy.paybridge.ledger.domain;

import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.exception.BusinessException;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccount {

    @Id
    @Column(length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private boolean allowNegative;

    protected LedgerAccount() {
    }

    LedgerAccount(String code, Currency currency, boolean allowNegative) {
        this.code = code;
        this.currency = currency;
        this.allowNegative = allowNegative;
        this.balance = BigDecimal.ZERO.setScale(2);
    }

    void debit(BigDecimal amount) {
        BigDecimal newBalance = balance.subtract(amount);
        if (!allowNegative && newBalance.signum() < 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INSUFFICIENT_FUNDS", "Insufficient funds");
        }
        balance = newBalance;
    }

    void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public String getCode() {
        return code;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
