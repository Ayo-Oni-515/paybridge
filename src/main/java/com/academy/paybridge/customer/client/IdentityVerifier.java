package com.academy.paybridge.customer.client;

import java.time.LocalDate;

/**
 * What PayBridge needs from an identity provider (NIBSS BVN lookup, NIMC NIN lookup).
 * In Nigeria these are usually reached through KYC aggregators; check their current docs and sandbox rules.
 */
public interface IdentityVerifier {

    VerificationResult verifyBvn(String bvn, String firstName, String lastName, LocalDate dateOfBirth);

    VerificationResult verifyNin(String nin, String firstName, String lastName, LocalDate dateOfBirth);
}
