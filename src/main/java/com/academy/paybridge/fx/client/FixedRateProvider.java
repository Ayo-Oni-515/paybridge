package com.academy.paybridge.fx.client;

import com.academy.paybridge.fx.domain.FxProperties;
import com.academy.paybridge.shared.exception.BusinessException;
import com.academy.paybridge.shared.money.Currency;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Reads the rate from application.yml (paybridge.fx.fixed-usd-ngn). Change it to simulate a moving market. */
@Component
@ConditionalOnProperty(name = "paybridge.fx.provider", havingValue = "fixed", matchIfMissing = true)
class FixedRateProvider implements RateProvider {

    private final FxProperties properties;

    FixedRateProvider(FxProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal midRate(Currency from, Currency to) {
        if (from == Currency.USD && to == Currency.NGN) {
            return properties.fixedUsdNgn();
        }
        throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "PAIR_NOT_SUPPORTED",
                "Only USD -> NGN is supported");
    }
}
