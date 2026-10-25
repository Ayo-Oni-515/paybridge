package com.academy.paybridge.customer.api;

/**
 * Simplified CBN-style tiers. Higher tiers unlock more products.
 * TIER_1: name, phone, email, date of birth (self-declared)
 * TIER_2: BVN verified
 * TIER_3: NIN verified as well
 */
public enum KycTier {
    TIER_1,
    TIER_2,
    TIER_3;

    public boolean isAtLeast(KycTier other) {
        return compareTo(other) >= 0;
    }
}
