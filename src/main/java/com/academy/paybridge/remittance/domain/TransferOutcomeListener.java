package com.academy.paybridge.remittance.domain;

import com.academy.paybridge.transfer.api.TransferFinished;
import com.academy.paybridge.transfer.api.TransferType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The remittance module doesn't poll the transfer module: it listens for TransferFinished.
 * Only our own payouts matter (their idempotency key starts with "remit-").
 */
@Component
class TransferOutcomeListener {

    private final RemittanceService remittances;

    TransferOutcomeListener(RemittanceService remittances) {
        this.remittances = remittances;
    }

    @TransactionalEventListener(fallbackExecution = true) // AFTER_COMMIT by default
    void on(TransferFinished event) {
        String key = event.idempotencyKey();
        if (event.transfer().type() == TransferType.EXTERNAL && key != null && key.startsWith("remit-")) {
            remittances.onPayoutFinished(key, event.transfer().id(), event.transfer().status(),
                    event.transfer().failureReason());
        }
    }
}
