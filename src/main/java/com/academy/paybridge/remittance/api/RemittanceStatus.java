package com.academy.paybridge.remittance.api;

public enum RemittanceStatus {
    PAID_TO_WALLET,   // naira credited to the beneficiary's PayBridge account (final if no bank payout)
    PAYOUT_PENDING,   // naira credited, now being sent on to their bank
    PAID_TO_BANK,
    PAYOUT_FAILED     // bank payout failed; the naira stays in the beneficiary's wallet
}
