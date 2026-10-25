package com.academy.paybridge.notification.listener;

import com.academy.paybridge.account.api.AccountApi;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.notification.domain.NotificationService;
import com.academy.paybridge.remittance.api.RemittancePaid;
import com.academy.paybridge.remittance.api.RemittanceView;
import com.academy.paybridge.transfer.api.TransferFinished;
import com.academy.paybridge.transfer.api.TransferType;
import com.academy.paybridge.transfer.api.TransferView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * Transfer and remittance never call notification directly; they publish events and this reacts.
 * @TransactionalEventListener: runs only after the payment commits (no alert for a rolled-back transfer).
 * @Async: a slow SMS gateway never slows down the payment response.
 */
@Component
class PaymentEventsListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsListener.class);

    private final NotificationService notifications;
    private final AccountApi accounts;

    PaymentEventsListener(NotificationService notifications, AccountApi accounts) {
        this.notifications = notifications;
        this.accounts = accounts;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onTransferFinished(TransferFinished event) {
        TransferView t = event.transfer();
        try {
            switch (t.status()) {
                case SUCCESSFUL -> {
                    notifications.notifyAccountHolder(t.sourceAccountId(), "Debit: %s %s to %s (%s). Ref: %s"
                            .formatted(t.currency(), money(t.amount()), t.destinationAccountName(),
                                    t.destinationAccountNumber(), shortRef(t.reference())));
                    if (t.type() == TransferType.INTERNAL) {
                        AccountView from = accounts.getById(t.sourceAccountId());
                        AccountView to = accounts.getByNumber(t.destinationAccountNumber());
                        notifications.notifyAccountHolder(to.id(), "Credit: %s %s from %s. Narration: %s"
                                .formatted(t.currency(), money(t.amount()), from.holderName(), t.narration()));
                    }
                }
                case FAILED -> notifications.notifyAccountHolder(t.sourceAccountId(),
                        "Your transfer of %s %s to %s failed. The money has been returned to your account."
                                .formatted(t.currency(), money(t.amount()), t.destinationAccountName()));
                case PENDING -> { }
            }
        } catch (RuntimeException e) {
            log.warn("Notification for transfer {} failed", t.reference(), e);
        }
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onRemittancePaid(RemittancePaid event) {
        RemittanceView r = event.remittance();
        try {
            notifications.notifyAccountHolder(r.beneficiaryAccountId(),
                    "Credit: NGN %s from %s (%s). USD %s at %s."
                            .formatted(money(r.ngnAmount()), r.senderName(), r.senderCountry(),
                                    money(r.usdAmount()), r.customerRate().stripTrailingZeros().toPlainString()));
        } catch (RuntimeException e) {
            log.warn("Notification for remittance {} failed", r.id(), e);
        }
    }

    private static String money(BigDecimal amount) {
        return "%,.2f".formatted(amount); // 150000.5 -> 150,000.50
    }

    private static String shortRef(String reference) {
        return reference.length() > 8 ? reference.substring(0, 8) : reference;
    }
}
