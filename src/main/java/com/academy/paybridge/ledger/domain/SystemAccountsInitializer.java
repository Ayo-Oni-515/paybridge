package com.academy.paybridge.ledger.domain;

import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.shared.money.Currency;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates PayBridge's internal accounts on startup.
 */
@Component
class SystemAccountsInitializer implements ApplicationRunner {

    private final LedgerApi ledger;

    SystemAccountsInitializer(LedgerApi ledger) {
        this.ledger = ledger;
    }

    @Override
    public void run(ApplicationArguments args) {
        ledger.openAccount(SystemAccounts.NGN_FUNDING, Currency.NGN, true);
        ledger.openAccount(SystemAccounts.NGN_PAYOUT_CLEARING, Currency.NGN, false);
        ledger.openAccount(SystemAccounts.NGN_EXTERNAL_PAYOUTS, Currency.NGN, false);
    }
}
