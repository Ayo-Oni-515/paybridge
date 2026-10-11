package com.academy.paybridge.transfer.domain;

import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import com.academy.paybridge.transfer.api.TransferStatus;
import com.academy.paybridge.transfer.api.TransferType;
import com.academy.paybridge.transfer.api.TransferView;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Our reference, sent to the payment company and echoed back in webhooks.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    /**
     * Client-supplied key: the same key twice returns the first result instead of paying twice.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(nullable = false)
    private UUID sourceAccountId;

    @Column(nullable = false, length = 10)
    private String destinationAccountNumber;

    private String destinationBankCode;      // null for internal transfers
    private String destinationAccountName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    private String narration;
    private String providerReference;
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Transfer() {
    }

    private Transfer(String idempotencyKey, TransferType type, UUID sourceAccountId,
                     String destinationAccountNumber, String destinationBankCode,
                     String destinationAccountName, Money money, String narration) {
        this.reference = UUID.randomUUID().toString();
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.status = TransferStatus.PENDING;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountNumber = destinationAccountNumber;
        this.destinationBankCode = destinationBankCode;
        this.destinationAccountName = destinationAccountName;
        this.amount = money.amount();
        this.currency = money.currency();
        this.narration = narration;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    static Transfer internal(String idempotencyKey, UUID sourceAccountId, String destinationAccountNumber,
                             String destinationAccountName, Money money, String narration) {
        return new Transfer(idempotencyKey, TransferType.INTERNAL, sourceAccountId,
                destinationAccountNumber, null, destinationAccountName, money, narration);
    }

    static Transfer external(String idempotencyKey, UUID sourceAccountId, String destinationAccountNumber,
                             String bankCode, String destinationAccountName, Money money, String narration) {
        return new Transfer(idempotencyKey, TransferType.EXTERNAL, sourceAccountId,
                destinationAccountNumber, bankCode, destinationAccountName, money, narration);
    }

    void markSuccessful() {
        this.status = TransferStatus.SUCCESSFUL;
        this.updatedAt = Instant.now();
    }

    void markFailed(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 255));
        this.updatedAt = Instant.now();
    }

    void recordProviderReference(String providerReference) {
        if (providerReference != null && this.providerReference == null) {
            this.providerReference = providerReference;
            this.updatedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public String getNarration() {
        return narration;
    }

    public Money getMoney() {
        return Money.of(amount, currency);
    }

    public TransferView toView() {
        return new TransferView(id, reference, type, status, amount, currency, sourceAccountId,
                destinationAccountNumber, destinationBankCode, destinationAccountName,
                narration, providerReference, failureReason, createdAt);
    }
}
