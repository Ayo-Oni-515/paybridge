package com.academy.paybridge.customer.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 with a secret key, not plain SHA-256: a BVN is only 11 digits, so a plain hash
 * could be reversed by trying all 10^11 numbers. Without the key, that attack doesn't work.
 */
@Component
class IdentifierHasher {

    private final SecretKeySpec key;

    IdentifierHasher(@Value("${paybridge.kyc.hash-secret}") String secret) {
        if (secret.length() < 16) {
            throw new IllegalStateException("paybridge.kyc.hash-secret must be at least 16 characters");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    String hash(String identifier) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(identifier.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC not available", e);
        }
    }
}
