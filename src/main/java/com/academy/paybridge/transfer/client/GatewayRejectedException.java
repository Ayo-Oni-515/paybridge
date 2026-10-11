package com.academy.paybridge.transfer.client;

/** The provider definitely refused the payout, so it is safe to refund the sender. */
public class GatewayRejectedException extends RuntimeException {

    public GatewayRejectedException(String message) {
        super(message);
    }
}
