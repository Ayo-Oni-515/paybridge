package com.academy.paybridge.transfer.api;

public enum TransferStatus {
    PENDING,      // money held, waiting for the payment company
    SUCCESSFUL,
    FAILED        // money returned to the sender
}
