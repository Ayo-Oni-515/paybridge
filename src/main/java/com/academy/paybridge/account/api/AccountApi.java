package com.academy.paybridge.account.api;

import java.util.UUID;

/**
 * The only way other modules may talk to the account module.
 */
public interface AccountApi {

    AccountView getById(UUID accountId);

    AccountView getByNumber(String accountNumber);

    /**
     * The ledger account code that holds this account's money.
     */
    String ledgerCodeOf(UUID accountId);
}
