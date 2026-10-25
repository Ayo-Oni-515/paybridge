package com.academy.paybridge.fx.api;

public enum QuoteStatus {
    OPEN,      // can still be used
    USED,      // a remittance consumed it
    EXPIRED    // lock time passed; ask for a new quote
}
