package com.academy.paybridge.customer.client;

/**
 * matched = the ID exists AND its name/date of birth match the customer.
 */
public record VerificationResult(boolean matched, String reason) {

    public static VerificationResult ok() {
        return new VerificationResult(true, null);
    }

    public static VerificationResult rejected(String reason) {
        return new VerificationResult(false, reason);
    }
}
