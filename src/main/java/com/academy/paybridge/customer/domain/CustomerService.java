package com.academy.paybridge.customer.domain;

import com.academy.paybridge.customer.api.CustomerApi;
import com.academy.paybridge.customer.api.CustomerView;
import com.academy.paybridge.customer.api.KycTier;
import com.academy.paybridge.customer.client.IdentityVerifier;
import com.academy.paybridge.customer.client.VerificationResult;
import com.academy.paybridge.customer.repository.CustomerRepository;
import com.academy.paybridge.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class CustomerService implements CustomerApi {

    private final CustomerRepository customers;
    private final IdentityVerifier verifier;
    private final IdentifierHasher hasher;
    private final TransactionTemplate tx;

    public CustomerService(CustomerRepository customers, IdentityVerifier verifier, IdentifierHasher hasher,
                           PlatformTransactionManager txManager) {
        this.customers = customers;
        this.verifier = verifier;
        this.hasher = hasher;
        this.tx = new TransactionTemplate(txManager);
    }

    @Transactional
    public CustomerView register(String firstName, String lastName, String email, String phoneNumber,
                                 LocalDate dateOfBirth) {
        String normalisedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (customers.existsByEmail(normalisedEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "A customer with this email already exists");
        }
        Customer customer = new Customer(firstName.trim(), lastName.trim(), normalisedEmail,
                toInternationalFormat(phoneNumber), dateOfBirth);
        return customers.save(customer).toView();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerView getById(UUID customerId) {
        return load(customerId).toView();
    }

    /**
     * TIER_1 -> TIER_2. Like external transfers, the provider call happens OUTSIDE a transaction:
     * never hold DB connections while waiting on the network.
     */
    public CustomerView verifyBvn(UUID customerId, String bvn) {
        Customer customer = load(customerId);
        if (customer.hasBvn()) {
            throw new BusinessException(HttpStatus.CONFLICT, "BVN_ALREADY_VERIFIED", "This customer already has a verified BVN");
        }
        String hash = hasher.hash(bvn);
        if (customers.existsByBvnHash(hash)) {
            throw new BusinessException(HttpStatus.CONFLICT, "BVN_IN_USE", "This BVN belongs to another customer");
        }

        requireMatch(verifier.verifyBvn(bvn, customer.getFirstName(), customer.getLastName(), customer.getDateOfBirth()),
                "BVN_NOT_VERIFIED");

        return tx.execute(status -> {
            Customer fresh = load(customerId);
            fresh.recordVerifiedBvn(hash, bvn.substring(bvn.length() - 4));
            return fresh.toView(); // unique bvnHash column catches a race between two customers
        });
    }

    /**
     * TIER_2 -> TIER_3.
     */
    public CustomerView verifyNin(UUID customerId, String nin) {
        Customer customer = load(customerId);
        if (!customer.getKycTier().isAtLeast(KycTier.TIER_2)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "BVN_REQUIRED_FIRST", "Verify a BVN before a NIN");
        }
        if (customer.hasNin()) {
            throw new BusinessException(HttpStatus.CONFLICT, "NIN_ALREADY_VERIFIED", "This customer already has a verified NIN");
        }
        String hash = hasher.hash(nin);
        if (customers.existsByNinHash(hash)) {
            throw new BusinessException(HttpStatus.CONFLICT, "NIN_IN_USE", "This NIN belongs to another customer");
        }

        requireMatch(verifier.verifyNin(nin, customer.getFirstName(), customer.getLastName(), customer.getDateOfBirth()),
                "NIN_NOT_VERIFIED");

        return tx.execute(status -> {
            Customer fresh = load(customerId);
            fresh.recordVerifiedNin(hash, nin.substring(nin.length() - 4));
            return fresh.toView();
        });
    }

    private Customer load(UUID customerId) {
        return customers.findById(customerId).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found: " + customerId));
    }

    private static void requireMatch(VerificationResult result, String code) {
        if (!result.matched()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, code, result.reason());
        }
    }

    /**
     * 08031234567 -> +2348031234567, so the same phone is always stored the same way.
     */
    static String toInternationalFormat(String phone) {
        String p = phone.trim();
        return p.startsWith("0") ? "+234" + p.substring(1) : p;
    }
}
