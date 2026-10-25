package com.academy.paybridge.customer.domain;

import com.academy.paybridge.customer.api.CustomerStatus;
import com.academy.paybridge.customer.api.CustomerView;
import com.academy.paybridge.customer.api.KycTier;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * NDPA: a BVN/NIN is sensitive personal data. We store only a keyed hash (to stop one BVN
 * being used for two customers) and the last 4 digits (to show the customer). Never the raw number.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 14)
    private String phoneNumber;   // stored as +234XXXXXXXXXX

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private KycTier kycTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CustomerStatus status;

    @Column(unique = true, length = 64)
    private String bvnHash;

    @Column(length = 4)
    private String bvnLast4;

    @Column(unique = true, length = 64)
    private String ninHash;

    @Column(length = 4)
    private String ninLast4;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Customer() {
    }

    Customer(String firstName, String lastName, String email, String phoneNumber, LocalDate dateOfBirth) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.kycTier = KycTier.TIER_1;
        this.status = CustomerStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    void recordVerifiedBvn(String hash, String last4) {
        this.bvnHash = hash;
        this.bvnLast4 = last4;
        if (!kycTier.isAtLeast(KycTier.TIER_2)) kycTier = KycTier.TIER_2;
        this.updatedAt = Instant.now();
    }

    void recordVerifiedNin(String hash, String last4) {
        this.ninHash = hash;
        this.ninLast4 = last4;
        this.kycTier = KycTier.TIER_3;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public KycTier getKycTier() {
        return kycTier;
    }

    public boolean hasBvn() {
        return bvnHash != null;
    }

    public boolean hasNin() {
        return ninHash != null;
    }

    CustomerView toView() {
        return new CustomerView(id, firstName, lastName, email, phoneNumber, dateOfBirth, kycTier, status,
                mask(bvnLast4), mask(ninLast4), createdAt);
    }

    private static String mask(String last4) {
        return last4 == null ? null : "*******" + last4;
    }
}
