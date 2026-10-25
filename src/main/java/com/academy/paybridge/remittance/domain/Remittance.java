package com.academy.paybridge.remittance.domain;

import com.academy.paybridge.remittance.api.RemittanceStatus;
import com.academy.paybridge.remittance.api.RemittanceView;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "remittances",
        uniqueConstraints = @UniqueConstraint(name = "uk_remittance_partner_ref",
                columnNames = {"partnerCode", "partnerReference"}),
        indexes = @Index(name = "idx_remittance_unsettled", columnList = "partnerCode, settlementId"))
public class Remittance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String partnerCode;

    @Column(nullable = false, length = 64)
    private String partnerReference;

    @Column(nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false, length = 2)
    private String senderCountry;

    @Column(nullable = false, unique = true)
    private UUID quoteId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal usdAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal ngnAmount;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal customerRate;

    @Column(nullable = false)
    private UUID beneficiaryAccountId;

    @Column(nullable = false, length = 10)
    private String beneficiaryAccountNumber;

    @Column(nullable = false)
    private String beneficiaryName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RemittanceStatus status;

    @Column(length = 10)
    private String payoutAccountNumber;

    @Column(length = 10)
    private String payoutBankCode;

    /** Idempotency key we give the transfer module, so a retry never pays the bank twice. */
    @Column(unique = true, length = 64)
    private String payoutIdempotencyKey;

    private UUID payoutTransferId;
    private String failureReason;

    private UUID settlementId;
    private Instant settledAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Remittance() {}

    Remittance(String partnerCode, String partnerReference, String senderName, String senderCountry,
               UUID quoteId, Money usd, Money ngn, BigDecimal customerRate,
               UUID beneficiaryAccountId, String beneficiaryAccountNumber, String beneficiaryName,
               String payoutAccountNumber, String payoutBankCode) {
        this.partnerCode = partnerCode;
        this.partnerReference = partnerReference;
        this.senderName = senderName;
        this.senderCountry = senderCountry;
        this.quoteId = quoteId;
        this.usdAmount = usd.amount();
        this.ngnAmount = ngn.amount();
        this.customerRate = customerRate;
        this.beneficiaryAccountId = beneficiaryAccountId;
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
        this.beneficiaryName = beneficiaryName;
        this.payoutAccountNumber = payoutAccountNumber;
        this.payoutBankCode = payoutBankCode;
        this.status = wantsBankPayout() ? RemittanceStatus.PAYOUT_PENDING : RemittanceStatus.PAID_TO_WALLET;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    boolean wantsBankPayout() {
        return payoutAccountNumber != null;
    }

    /** Called right after save, once we have an ID. */
    void assignPayoutKey() {
        if (wantsBankPayout()) this.payoutIdempotencyKey = "remit-" + id;
    }

    void recordPayoutTransfer(UUID transferId) {
        if (transferId != null && payoutTransferId == null) {
            this.payoutTransferId = transferId;
            this.updatedAt = Instant.now();
        }
    }

    void payoutSucceeded() {
        this.status = RemittanceStatus.PAID_TO_BANK;
        this.updatedAt = Instant.now();
    }

    void payoutFailed(String reason) {
        this.status = RemittanceStatus.PAYOUT_FAILED;
        String r = (reason == null ? "Payout failed" : reason) + "; the naira remains in the beneficiary's wallet";
        this.failureReason = r.substring(0, Math.min(r.length(), 255));
        this.updatedAt = Instant.now();
    }

    void markSettled(UUID settlementId) {
        this.settlementId = settlementId;
        this.settledAt = Instant.now();
        this.updatedAt = settledAt;
    }

    public UUID getId() { return id; }
    public String getPartnerCode() { return partnerCode; }
    public String getPartnerReference() { return partnerReference; }
    public String getSenderName() { return senderName; }
    public RemittanceStatus getStatus() { return status; }
    public UUID getBeneficiaryAccountId() { return beneficiaryAccountId; }
    public String getPayoutAccountNumber() { return payoutAccountNumber; }
    public String getPayoutBankCode() { return payoutBankCode; }
    public String getPayoutIdempotencyKey() { return payoutIdempotencyKey; }
    public UUID getSettlementId() { return settlementId; }
    public Money getUsd() { return Money.of(usdAmount, Currency.USD); }
    public Money getNgn() { return Money.of(ngnAmount, Currency.NGN); }

    RemittanceView toView() {
        return new RemittanceView(id, partnerCode, partnerReference, senderName, senderCountry, quoteId,
                usdAmount, ngnAmount, customerRate, beneficiaryAccountId, beneficiaryAccountNumber, beneficiaryName,
                status, payoutAccountNumber, payoutBankCode, payoutTransferId, failureReason, settlementId, createdAt);
    }
}
