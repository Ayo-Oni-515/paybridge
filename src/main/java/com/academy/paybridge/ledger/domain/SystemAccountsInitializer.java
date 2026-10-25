package com.academy.paybridge.ledger.domain;

import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.shared.money.Currency;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates PayBridge's internal accounts on startup.
 * allowNegative = true for accounts that represent the outside world (money comes FROM them).
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
        ledger.openAccount(SystemAccounts.USD_FUNDING, Currency.USD, true);

        ledger.openAccount(SystemAccounts.NGN_PAYOUT_CLEARING, Currency.NGN, false);
        ledger.openAccount(SystemAccounts.NGN_EXTERNAL_PAYOUTS, Currency.NGN, false);

        ledger.openAccount(SystemAccounts.USD_FX_POSITION, Currency.USD, false); // can't sell dollars we don't hold
        ledger.openAccount(SystemAccounts.NGN_FX_POSITION, Currency.NGN, true);  // negative while naira is paid out
        ledger.openAccount(SystemAccounts.USD_FX_SOLD, Currency.USD, false);
        ledger.openAccount(SystemAccounts.NGN_FX_BOUGHT, Currency.NGN, true);
        ledger.openAccount(SystemAccounts.NGN_FX_REVENUE, Currency.NGN, true);   // negative = FX loss

        ledger.openAccount(SystemAccounts.USD_INBOUND_WIRES, Currency.USD, true);
    }
}
