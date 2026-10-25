package com.academy.paybridge.remittance.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * paybridge.remittance.partners: the IMTOs allowed to send us money.
 * Real partners also authenticate (API keys, mTLS, IP allow-lists); left out to keep the flow visible.
 */
@ConfigurationProperties("paybridge.remittance")
public record RemittanceProperties(List<String> partners) {

    public RemittanceProperties {
        partners = partners == null ? List.of() : List.copyOf(partners);
    }
}
