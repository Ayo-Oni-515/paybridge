package com.academy.paybridge.settlement.api;

/** A "reconciliation break": something that doesn't match and needs a human to investigate. */
public enum BreakType {
    MISSING_IN_PAYBRIDGE,   // partner says they sent it, we have no record
    MISSING_IN_REPORT,      // we paid it, partner didn't include it in their report
    AMOUNT_MISMATCH,        // both have it, USD amounts differ
    ALREADY_SETTLED,        // partner reported it again in a later settlement
    DUPLICATE_IN_REPORT,    // same reference twice in one report
    WIRE_MISMATCH           // money wired != total of the report
}
