package com.academy.paybridge.ledger.domain;

import com.academy.paybridge.ledger.repository.LedgerAccountRepository;
import com.academy.paybridge.ledger.repository.LedgerEntryRepository;
import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import com.academy.paybridge.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
public class LedgerService implements LedgerApi {

    private final LedgerAccountRepository accounts;
    private final LedgerEntryRepository entries;

    public LedgerService(LedgerAccountRepository accounts, LedgerEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    @Override
    @Transactional
    public void openAccount(String code, Currency currency, boolean allowNegative) {
        if (!accounts.existsById(code)) {
            accounts.save(new LedgerAccount(code, currency, allowNegative));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Money balanceOf(String code) {
        LedgerAccount account = accounts.findById(code).orElseThrow(() -> notFound(code));
        return Money.of(account.getBalance(), account.getCurrency());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY) // fails loudly if called outside a transaction
    public void move(String journalReference, String fromCode, String toCode, Money amount, String description) {
        if (!amount.isPositive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero");
        }
        if (fromCode.equals(toCode)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT", "Cannot move money to the same account");
        }

        // Lock both rows in a fixed (alphabetical) order. If one transfer locked A then B
        // while another locked B then A at the same moment, they would deadlock.
        Map<String, LedgerAccount> locked = new HashMap<>();
        for (String code : new TreeSet<>(List.of(fromCode, toCode))) {
            locked.put(code, accounts.findForUpdate(code).orElseThrow(() -> notFound(code)));
        }
        LedgerAccount from = locked.get(fromCode);
        LedgerAccount to = locked.get(toCode);

        if (from.getCurrency() != amount.currency() || to.getCurrency() != amount.currency()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "CURRENCY_MISMATCH",
                    "Both accounts must be in " + amount.currency());
        }

        from.debit(amount.amount());   // throws INSUFFICIENT_FUNDS -> whole transaction rolls back
        to.credit(amount.amount());

        // Double entry: two equal and opposite lines sharing one journal reference.
        entries.save(new LedgerEntry(journalReference, fromCode, EntryDirection.DEBIT, amount, description));
        entries.save(new LedgerEntry(journalReference, toCode, EntryDirection.CREDIT, amount, description));
    }

    private static BusinessException notFound(String code) {
        return new BusinessException(HttpStatus.NOT_FOUND, "LEDGER_ACCOUNT_NOT_FOUND", "Ledger account not found: " + code);
    }
}
