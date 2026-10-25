package com.academy.paybridge.customer.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Read-only snapshot of a customer, safe to hand to other modules. Never contains a raw BVN/NIN. */
public record CustomerView(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        KycTier kycTier,
        CustomerStatus status,
        String maskedBvn,   // e.g. "*******4321", null until verified
        String maskedNin,
        Instant createdAt
) {
    public String fullName() {
        return firstName + " " + lastName;
    }
}
