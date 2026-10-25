package com.academy.paybridge.transfer.domain;

import com.academy.paybridge.account.api.AccountApi;
import com.academy.paybridge.account.api.AccountStatus;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import com.academy.paybridge.shared.exception.BusinessException;
import com.academy.paybridge.transfer.api.ExternalTransferRequest;
import com.academy.paybridge.transfer.api.TransferApi;
import com.academy.paybridge.transfer.api.TransferFinished;
import com.academy.paybridge.transfer.api.TransferType;
import com.academy.paybridge.transfer.api.InternalTransferRequest;
import com.academy.paybridge.transfer.api.TransferStatus;
import com.academy.paybridge.transfer.api.TransferView;
import com.academy.paybridge.transfer.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService implements TransferApi {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transfers;
    private final AccountApi accounts;
    private final LedgerApi ledger;
    private final TransferGateway gateway;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;

    public TransferService(TransferRepository transfers, AccountApi accounts, LedgerApi ledger,
                           TransferGateway gateway, PlatformTransactionManager txManager,
                           ApplicationEventPublisher events) {
        this.transfers = transfers;
        this.accounts = accounts;
        this.ledger = ledger;
        this.gateway = gateway;
        this.tx = new TransactionTemplate(txManager);
        this.events = events;
    }

    // ------------------------------------------------------------------
    // INTERNAL: PayBridge -> PayBridge. No payment company, just the ledger.
    // ------------------------------------------------------------------
    @Transactional
    public TransferView sendInternal(String idempotencyKey, InternalTransferRequest request) {
        requireValidKey(idempotencyKey);
        Optional<Transfer> existing = transfers.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().toView(); // retry: return the original result, don't pay twice
        }

        AccountView source = accounts.getById(request.sourceAccountId());
        AccountView destination = accounts.getByNumber(request.destinationAccountNumber());
        requireActiveNgn(source);
        requireActiveNgn(destination);
        if (source.id().equals(destination.id())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT", "Cannot transfer to the same account");
        }

        Money amount = Money.of(request.amount(), Currency.NGN);
        Transfer transfer = transfers.save(Transfer.internal(idempotencyKey, source.id(),
                destination.accountNumber(), destination.holderName(), amount, narration(request.narration())));

        // One ledger journal: debit sender, credit receiver. Insufficient funds rolls everything back.
        ledger.move(transfer.getReference(), accounts.ledgerCodeOf(source.id()),
                accounts.ledgerCodeOf(destination.id()), amount, transfer.getNarration());

        transfer.markSuccessful();
        publishFinished(transfer);
        return transfer.toView();
    }

    // ------------------------------------------------------------------
    // EXTERNAL: PayBridge -> another bank, through the payment company.
    // Deliberately NOT one big transaction: never hold DB locks while waiting on the network.
    // ------------------------------------------------------------------
    @Override
    public TransferView sendExternal(String idempotencyKey, ExternalTransferRequest request) {
        requireValidKey(idempotencyKey);
        Optional<Transfer> existing = transfers.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().toView();
        }

        AccountView source = accounts.getById(request.sourceAccountId());
        requireActiveNgn(source);
        Money amount = Money.of(request.amount(), Currency.NGN);

        // Step 1: name enquiry - who owns this account?
        ResolvedAccount recipient = gateway.resolveAccount(request.destinationAccountNumber(), request.bankCode());

        // Step 2: save the transfer and HOLD the money in clearing (sender can't spend it twice).
        Transfer transfer = tx.execute(status -> {
            Transfer t = transfers.save(Transfer.external(idempotencyKey, source.id(),
                    request.destinationAccountNumber(), request.bankCode(), recipient.accountName(),
                    amount, narration(request.narration())));
            ledger.move(t.getReference(), accounts.ledgerCodeOf(source.id()),
                    SystemAccounts.NGN_PAYOUT_CLEARING, amount, "Payout hold: " + t.getNarration());
            return t;
        });

        // Step 3: ask the payment company to send it.
        GatewayResult result;
        try {
            result = gateway.send(new PayoutRequest(transfer.getReference(), recipient.accountName(),
                    request.destinationAccountNumber(), request.bankCode(), amount, transfer.getNarration()));
        } catch (GatewayRejectedException e) {
            result = GatewayResult.failed(e.getMessage());
        } catch (RuntimeException e) {
            // Timeout / 5xx: money MAY have left. Never refund on a guess.
            // Stay PENDING; the webhook (or a later status check) decides.
            log.warn("Outcome unknown for transfer {}; leaving it PENDING", transfer.getReference(), e);
            return transfer.toView();
        }

        // Step 4: record the outcome (a webhook may settle it later instead).
        GatewayResult outcome = result;
        return tx.execute(status -> applyOutcome(transfer.getReference(), outcome.status(),
                outcome.providerReference(), outcome.message()));
    }

    /**
     * Settles a PENDING external transfer. Called after the provider replies AND from webhooks.
     * Safe to call more than once: only the first final outcome counts.
     */
    @Transactional
    public TransferView applyOutcome(String reference, GatewayStatus outcome, String providerReference, String reason) {
        Transfer transfer = transfers.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND",
                        "No transfer with reference " + reference));

        transfer.recordProviderReference(providerReference);
        if (transfer.getStatus() != TransferStatus.PENDING || outcome == GatewayStatus.PENDING) {
            return transfer.toView(); // already settled, or still waiting
        }

        Money amount = transfer.getMoney();
        if (outcome == GatewayStatus.SUCCESS) {
            // Money has left PayBridge: clearing -> external payouts.
            ledger.move(reference + ":settle", SystemAccounts.NGN_PAYOUT_CLEARING,
                    SystemAccounts.NGN_EXTERNAL_PAYOUTS, amount, "Payout completed");
            transfer.markSuccessful();
        } else {
            // Failed: release the hold back to the sender.
            ledger.move(reference + ":refund", SystemAccounts.NGN_PAYOUT_CLEARING,
                    accounts.ledgerCodeOf(transfer.getSourceAccountId()), amount, "Payout failed, refund");
            transfer.markFailed(reason);
        }
        publishFinished(transfer);
        return transfer.toView();
    }

    @Override
    @Transactional(readOnly = true)
    public TransferView get(UUID id) {
        return transfers.findById(id).map(Transfer::toView)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND",
                        "Transfer not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Money pendingExternalTotal() {
        BigDecimal total = transfers.sumAmount(TransferType.EXTERNAL, TransferStatus.PENDING);
        return Money.of(total == null ? BigDecimal.ZERO : total, Currency.NGN);
    }

    /** Notification and remittance listen for this; they run only after our transaction commits. */
    private void publishFinished(Transfer transfer) {
        events.publishEvent(new TransferFinished(transfer.toView(), transfer.getIdempotencyKey()));
    }

    private static void requireValidKey(String key) {
        if (key == null || key.isBlank() || key.length() > 64) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key header is required (max 64 characters)");
        }
    }

    private static void requireActiveNgn(AccountView account) {
        if (account.status() != AccountStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "ACCOUNT_INACTIVE",
                    "Account " + account.accountNumber() + " is " + account.status());
        }
        if (account.currency() != Currency.NGN) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "CURRENCY_NOT_SUPPORTED",
                    "Only NGN accounts can make transfers for now");
        }
    }

    private static String narration(String narration) {
        return narration == null || narration.isBlank() ? "PayBridge transfer" : narration.trim();
    }
}
