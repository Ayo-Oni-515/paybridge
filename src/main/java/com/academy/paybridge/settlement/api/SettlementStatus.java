package com.academy.paybridge.settlement.api;

public enum SettlementStatus {
    RECONCILED,     // every line matched and the wire equals the report total
    BREAKS_FOUND
}
