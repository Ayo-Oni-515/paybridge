package com.academy.paybridge.customer.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Offline simulator.
 * ID starting with 0 -> not found
 * ID starting with 1 -> found, but the name doesn't match
 * anything else      -> verified
 */
@Component
@ConditionalOnProperty(name = "paybridge.kyc.provider", havingValue = "fake", matchIfMissing = true)
class FakeIdentityVerifier implements IdentityVerifier {

    @Override
    public VerificationResult verifyBvn(String bvn, String firstName, String lastName, LocalDate dateOfBirth) {
        return simulate(bvn, "BVN");
    }

    @Override
    public VerificationResult verifyNin(String nin, String firstName, String lastName, LocalDate dateOfBirth) {
        return simulate(nin, "NIN");
    }

    private static VerificationResult simulate(String id, String kind) {
        if (id.startsWith("0")) return VerificationResult.rejected(kind + " not found");
        if (id.startsWith("1")) return VerificationResult.rejected("Name on " + kind + " does not match the customer");
        return VerificationResult.ok();
    }
}
