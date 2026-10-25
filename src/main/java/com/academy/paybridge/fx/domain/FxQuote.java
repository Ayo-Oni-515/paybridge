package com.academy.paybridge.fx.domain;

import com.academy.paybridge.fx.api.QuoteStatus;
import com.academy.paybridge.fx.api.QuoteView;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fx_quotes")
public class FxQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal sourceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency sourceCurrency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency targetCurrency;

    /** Kept for profit analysis; never shown to partners. */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal midRate;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal customerRate;

    @Column(nullable = false)
    private Instant expiresAt;

    /** Only OPEN or USED are stored; EXPIRED is worked out from expiresAt. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuoteStatus status;

    private String usedBy;
    private Instant usedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected FxQuote() {}

    FxQuote(Money source, Money target, BigDecimal midRate, BigDecimal customerRate, Instant now, Instant expiresAt) {
        this.sourceAmount = source.amount();
        this.sourceCurrency = source.currency();
        this.targetAmount = target.amount();
        this.targetCurrency = target.currency();
        this.midRate = midRate;
        this.customerRate = customerRate;
        this.status = QuoteStatus.OPEN;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    boolean isUsed() {
        return status == QuoteStatus.USED;
    }

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    void markUsed(String usedBy, Instant now) {
        this.status = QuoteStatus.USED;
        this.usedBy = usedBy;
        this.usedAt = now;
    }

    public UUID getId() { return id; }
    public Instant getExpiresAt() { return expiresAt; }

    QuoteView toView(Instant now) {
        QuoteStatus shown = status == QuoteStatus.OPEN && isExpired(now) ? QuoteStatus.EXPIRED : status;
        return new QuoteView(id, sourceAmount, sourceCurrency, targetAmount, targetCurrency,
                customerRate, expiresAt, shown);
    }
}
