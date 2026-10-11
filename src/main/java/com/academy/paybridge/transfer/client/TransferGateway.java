package com.academy.paybridge.transfer.client;

/**
 * What PayBridge needs from ANY payment company.
 * Paystack, Flutterwave, etc. each get their own implementation;
 * TransferService never knows which one it is talking to.
 */
public interface TransferGateway {

    /** Name enquiry: confirm who owns this account before sending money. */
    ResolvedAccount resolveAccount(String accountNumber, String bankCode);

    /**
     * Ask the payment company to send the money.
     * @throws GatewayRejectedException if the provider clearly refused (no money moved)
     * @throws RuntimeException for timeouts/5xx - the outcome is UNKNOWN
     */
    GatewayResult send(PayoutRequest request);
}
