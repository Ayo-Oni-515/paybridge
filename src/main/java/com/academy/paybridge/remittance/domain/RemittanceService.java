package com.academy.paybridge.remittance.domain;

import com.academy.paybridge.account.api.AccountApi;
import com.academy.paybridge.account.api.AccountStatus;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.fx.api.FxApi;
import com.academy.paybridge.fx.api.QuoteView;
import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.remittance.api.CreateRemittanceRequest;
import com.academy.paybridge.remittance.api.RemittanceApi;
import com.academy.paybridge.remittance.api.RemittancePaid;
import com.academy.paybridge.remittance.api.RemittanceStatus;
import com.academy.paybridge.remittance.api.RemittanceView;
import com.academy.paybridge.remittance.repository.RemittanceRepository;
import com.academy.paybridge.shared.exception.BusinessException;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import com.academy.paybridge.transfer.api.ExternalTransferRequest;
import com.academy.paybridge.transfer.api.TransferApi;
import com.academy.paybridge.transfer.api.TransferStatus;
import com.academy.paybridge.transfer.api.TransferView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inbound remittance (notes.md case 4, steps 1-2):
 *   USD journal: partner receivable -> USD FX position
 *   NGN journal: NGN FX position     -> beneficiary's wallet
 * Optionally the naira is then sent on to a bank account through the transfer module (case 3).
 */
@Service
public class RemittanceService implements RemittanceApi {

    private static final Logger log = LoggerFactory.getLogger(RemittanceService.class);

    private final RemittanceRepository remittances;
    private final AccountApi accounts;
    private final FxApi fx;
    private final LedgerApi ledger;
    private final TransferApi transfers;
    private final RemittanceProperties properties;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    public RemittanceService(RemittanceRepository remittances, AccountApi accounts, FxApi fx, LedgerApi ledger,
                             TransferApi transfers, RemittanceProperties properties,
                             ApplicationEventPublisher events, PlatformTransactionManager txManager) {
        this.remittances = remittances;
        this.accounts = accounts;
        this.fx = fx;
        this.ledger = ledger;
        this.transfers = transfers;
        this.properties = properties;
        this.events = events;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Not one big transaction: the bank payout calls the network, so it runs after the wallet credit commits. */
    public RemittanceView receive(CreateRemittanceRequest request) {
        requireKnownPartner(request.partnerCode());

        // The partner's own reference is the idempotency key: the same remittance sent twice is paid once.
        Optional<Remittance> existing = remittances.findByPartnerCodeAndPartnerReference(
                request.partnerCode(), request.partnerReference());
        if (existing.isPresent()) {
            Remittance r = existing.get();
            // A crash after the wallet credit could leave the bank payout unstarted. Retrying is safe:
            // the transfer module returns the existing transfer for the same idempotency key.
            return r.getStatus() == RemittanceStatus.PAYOUT_PENDING ? payoutToBank(r) : r.toView();
        }

        Remittance remittance = tx.execute(status -> creditWallet(request));
        if (!remittance.wantsBankPayout()) {
            return remittance.toView();
        }
        return payoutToBank(remittance);
    }

    private Remittance creditWallet(CreateRemittanceRequest request) {
        AccountView beneficiary = accounts.getByNumber(request.beneficiaryAccountNumber());
        if (beneficiary.status() != AccountStatus.ACTIVE || beneficiary.currency() != Currency.NGN) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "BENEFICIARY_NOT_ELIGIBLE",
                    "Beneficiary must have an ACTIVE NGN account");
        }

        // Locks the rate. If anything below fails, the whole transaction (and the quote use) rolls back.
        QuoteView quote = fx.consumeQuote(request.quoteId(),
                "remittance:" + request.partnerCode() + ":" + request.partnerReference());
        if (quote.sourceCurrency() != Currency.USD || quote.targetCurrency() != Currency.NGN) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "QUOTE_WRONG_PAIR", "Quote must be USD -> NGN");
        }
        Money usd = Money.of(quote.sourceAmount(), Currency.USD);
        Money ngn = Money.of(quote.targetAmount(), Currency.NGN);

        CreateRemittanceRequest.BankPayout bank = request.payoutBank();
        Remittance remittance = remittances.save(new Remittance(
                request.partnerCode(), request.partnerReference(), request.senderName().trim(), request.senderCountry(),
                quote.id(), usd, ngn, quote.customerRate(),
                beneficiary.id(), beneficiary.accountNumber(), beneficiary.holderName(),
                bank == null ? null : bank.accountNumber(), bank == null ? null : bank.bankCode()));
        remittance.assignPayoutKey();

        String ref = "remit-" + remittance.getId();
        // Step 1 (USD): the partner now owes us these dollars.
        ledger.move(ref + ":usd", SystemAccounts.partnerReceivable(request.partnerCode()),
                SystemAccounts.USD_FX_POSITION, usd, "Inbound remittance " + request.partnerReference());
        // Step 2 (NGN): we pay the beneficiary from our own naira.
        ledger.move(ref + ":ngn", SystemAccounts.NGN_FX_POSITION,
                accounts.ledgerCodeOf(beneficiary.id()), ngn, "Remittance from " + remittance.getSenderName());

        events.publishEvent(new RemittancePaid(remittance.toView()));
        return remittance;
    }

    private RemittanceView payoutToBank(Remittance remittance) {
        String key = remittance.getPayoutIdempotencyKey();
        try {
            TransferView transfer = transfers.sendExternal(key, new ExternalTransferRequest(
                    remittance.getBeneficiaryAccountId(),
                    remittance.getPayoutAccountNumber(),
                    remittance.getPayoutBankCode(),
                    remittance.getNgn().amount(),
                    narration(remittance)));
            return tx.execute(s -> applyPayout(key, transfer.id(), transfer.status(), transfer.failureReason()));
        } catch (BusinessException e) {
            // e.g. name enquiry failed. No money left the wallet, so this is a clean failure.
            log.info("Bank payout for remittance {} rejected: {}", remittance.getId(), e.getMessage());
            return tx.execute(s -> applyPayout(key, null, TransferStatus.FAILED, e.getMessage()));
        }
        // Other exceptions (timeouts) propagate: the transfer stays PENDING and its webhook finishes the job.
    }

    /** Called by TransferOutcomeListener when a payout transfer finishes later (e.g. via webhook). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPayoutFinished(String payoutKey, UUID transferId, TransferStatus status, String reason) {
        // A client could pick a key that happens to start with "remit-": ignore keys we don't own.
        if (remittances.findByPayoutKeyForUpdate(payoutKey).isPresent()) {
            applyPayout(payoutKey, transferId, status, reason);
        }
    }

    /** Safe to call twice (listener + original request): only the first final outcome counts. */
    private RemittanceView applyPayout(String payoutKey, UUID transferId, TransferStatus status, String reason) {
        Remittance remittance = remittances.findByPayoutKeyForUpdate(payoutKey)
                .orElseThrow(() -> new IllegalStateException("No remittance for payout key " + payoutKey));
        remittance.recordPayoutTransfer(transferId);
        if (remittance.getStatus() == RemittanceStatus.PAYOUT_PENDING) {
            switch (status) {
                case SUCCESSFUL -> remittance.payoutSucceeded();
                case FAILED -> remittance.payoutFailed(reason);
                case PENDING -> { } // still waiting for the webhook
            }
        }
        return remittance.toView();
    }

    @Transactional(readOnly = true)
    public RemittanceView get(UUID id) {
        return remittances.findById(id).map(Remittance::toView)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REMITTANCE_NOT_FOUND",
                        "Remittance not found: " + id));
    }

    // ---------------- RemittanceApi (for settlement) ----------------

    @Override
    public Set<String> partnerCodes() {
        return new LinkedHashSet<>(properties.partners());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RemittanceView> findUnsettled(String partnerCode) {
        return remittances.findByPartnerCodeAndSettlementIdIsNullOrderByCreatedAt(partnerCode)
                .stream().map(Remittance::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RemittanceView> findByPartnerReference(String partnerCode, String partnerReference) {
        return remittances.findByPartnerCodeAndPartnerReference(partnerCode, partnerReference).map(Remittance::toView);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markSettled(Collection<UUID> remittanceIds, UUID settlementId) {
        for (Remittance r : remittances.findAllById(remittanceIds)) {
            if (r.getSettlementId() == null) {
                r.markSettled(settlementId);
            }
        }
    }

    private void requireKnownPartner(String partnerCode) {
        if (!properties.partners().contains(partnerCode)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "UNKNOWN_PARTNER", "Partner " + partnerCode + " is not onboarded");
        }
    }

    private static String narration(Remittance r) {
        String text = "Remittance from " + r.getSenderName();
        return text.substring(0, Math.min(text.length(), 100));
    }
}
