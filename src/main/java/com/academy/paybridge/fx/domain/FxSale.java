package com.academy.paybridge.fx.domain;

import com.academy.paybridge.shared.money.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** PayBridge selling the dollars it collected from remittances (notes.md case 4, steps 3-4). */
@Entity
@Table(name = "fx_sales")
public class FxSale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal usdSold;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal ngnReceived;

    /** Set when this sale emptied the USD position and the spread was booked. Negative = loss. */
    @Column(precision = 19, scale = 2)
    private BigDecimal realisedProfit;

    @Column(nullable = false)
    private Instant createdAt;

    protected FxSale() {}

    FxSale(String idempotencyKey, Money usd, BigDecimal rate, Money ngn, Instant now) {
        this.idempotencyKey = idempotencyKey;
        this.usdSold = usd.amount();
        this.rate = rate;
        this.ngnReceived = ngn.amount();
        this.createdAt = now;
    }

    void recordRealisedProfit(BigDecimal profit) {
        this.realisedProfit = profit;
    }

    public UUID getId() { return id; }

    public record View(UUID id, BigDecimal usdSold, BigDecimal rate, BigDecimal ngnReceived,
                       BigDecimal realisedProfit, Instant createdAt) {}

    View toView() {
        return new View(id, usdSold, rate, ngnReceived, realisedProfit, createdAt);
    }
}
