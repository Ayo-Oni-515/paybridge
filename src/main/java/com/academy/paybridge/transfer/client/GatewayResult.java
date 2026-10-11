package com.academy.paybridge.transfer.client;

public record GatewayResult(GatewayStatus status, String providerReference, String message) {

    public static GatewayResult failed(String message) {
        return new GatewayResult(GatewayStatus.FAILED, null, message);
    }
}
