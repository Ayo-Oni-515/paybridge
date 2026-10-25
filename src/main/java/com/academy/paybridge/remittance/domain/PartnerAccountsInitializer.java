package com.academy.paybridge.remittance.domain;

import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.shared.money.Currency;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** One receivable ledger account per partner, so we always know who owes us what. */
@Component
class PartnerAccountsInitializer implements ApplicationRunner {

    private final LedgerApi ledger;
    private final RemittanceProperties properties;

    PartnerAccountsInitializer(LedgerApi ledger, RemittanceProperties properties) {
        this.ledger = ledger;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String partner : properties.partners()) {
            ledger.openAccount(SystemAccounts.partnerReceivable(partner), Currency.USD, true);
        }
    }
}
