package com.academy.paybridge.transfer.api;

public enum TransferType {
    INTERNAL,   // PayBridge -> PayBridge: ledger only
    EXTERNAL    // PayBridge -> another bank: through the payment company
}
