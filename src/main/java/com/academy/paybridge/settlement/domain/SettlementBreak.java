package com.academy.paybridge.settlement.domain;

import com.academy.paybridge.settlement.api.BreakType;
import com.academy.paybridge.settlement.api.BreakView;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

@Embeddable
public class SettlementBreak {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BreakType type;

    @Column(length = 64)
    private String partnerReference;

    @Column(precision = 19, scale = 2)
    private BigDecimal expectedUsd;

    @Column(precision = 19, scale = 2)
    private BigDecimal actualUsd;

    private String detail;

    protected SettlementBreak() {}

    SettlementBreak(Reconciler.Break b) {
        this.type = b.type();
        this.partnerReference = b.partnerReference();
        this.expectedUsd = b.expected();
        this.actualUsd = b.actual();
        this.detail = b.detail();
    }

    BreakView toView() {
        return new BreakView(type, partnerReference, expectedUsd, actualUsd, detail);
    }
}
