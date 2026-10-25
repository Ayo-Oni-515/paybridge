package com.academy.paybridge.transfer.api;

/**
 * Published when a transfer reaches SUCCESSFUL or FAILED.
 * Listeners run after the transaction commits, so they never see a transfer that was rolled back.
 */
public record TransferFinished(TransferView transfer, String idempotencyKey) {
}
