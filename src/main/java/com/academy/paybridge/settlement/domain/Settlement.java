package com.academy.paybridge.settlement.domain;

import com.academy.paybridge.settlement.api.SettlementStatus;
import com.academy.paybridge.settlement.api.SettlementView;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "settlements",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_wire", columnNames = {"partnerCode", "bankReference"}))
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String partnerCode;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Column(nullable = false, length = 64)
    private String bankReference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal wireAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal reportedTotal;

    @Column(nullable = false)
    private int matchedCount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(precision = 19, scale = 2)
    private BigDecimal receivableAfter;

    @ElementCollection
    @CollectionTable(name = "settlement_breaks", joinColumns = @JoinColumn(name = "settlement_id"))
    private List<SettlementBreak> breaks = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    protected Settlement() {}

    Settlement(String partnerCode, LocalDate settlementDate, String bankReference, BigDecimal wireAmount,
               Reconciler.Result result) {
        this.partnerCode = partnerCode;
        this.settlementDate = settlementDate;
        this.bankReference = bankReference;
        this.wireAmount = wireAmount;
        this.reportedTotal = result.reportedTotal();
        this.matchedCount = result.matchedIds().size();
        this.matchedTotal = result.matchedTotal();
        result.breaks().forEach(b -> breaks.add(new SettlementBreak(b)));
        this.status = breaks.isEmpty() ? SettlementStatus.RECONCILED : SettlementStatus.BREAKS_FOUND;
        this.createdAt = Instant.now();
    }

    void recordReceivableAfter(BigDecimal balance) {
        this.receivableAfter = balance;
    }

    public UUID getId() { return id; }

    SettlementView toView() {
        return new SettlementView(id, partnerCode, settlementDate, bankReference, wireAmount, reportedTotal,
                matchedCount, matchedTotal, status, receivableAfter,
                breaks.stream().map(SettlementBreak::toView).toList(), createdAt);
    }
}
