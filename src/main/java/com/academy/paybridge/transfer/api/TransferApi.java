package com.academy.paybridge.transfer.api;

import com.academy.paybridge.shared.money.Money;

import java.util.UUID;

/** The only way other modules may use transfers (remittance bank payouts, settlement checks). */
public interface TransferApi {

    /** Same rules as POST /api/transfers/external, including idempotency. */
    TransferView sendExternal(String idempotencyKey, ExternalTransferRequest request);

    TransferView get(UUID transferId);

    /** Total of EXTERNAL transfers still PENDING. Must equal the payout clearing balance. */
    Money pendingExternalTotal();
}
