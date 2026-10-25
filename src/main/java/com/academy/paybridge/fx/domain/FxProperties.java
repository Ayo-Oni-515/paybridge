package com.academy.paybridge.fx.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * paybridge.fx.* in application.yml
 * @param spreadPercent how far below the market rate customers are paid (our margin), e.g. 1.5
 * @param quoteTtl      how long a quote is locked for
 * @param fixedUsdNgn   market rate used by FixedRateProvider
 */
@ConfigurationProperties("paybridge.fx")
public record FxProperties(BigDecimal spreadPercent, Duration quoteTtl, BigDecimal fixedUsdNgn) {

    public FxProperties {
        if (spreadPercent == null) spreadPercent = new BigDecimal("1.5");
        if (quoteTtl == null) quoteTtl = Duration.ofMinutes(5);
        if (fixedUsdNgn == null) fixedUsdNgn = new BigDecimal("1520.00");
    }
}
