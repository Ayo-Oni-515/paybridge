package com.academy.paybridge.fx.client;

import com.academy.paybridge.shared.money.Currency;

import java.math.BigDecimal;

/**
 * Where the market (mid) rate comes from: a fixed config value now, an FX provider's API later.
 */
public interface RateProvider {

    /**
     * Units of {@code to} for one unit of {@code from}, e.g. USD->NGN = 1520.00
     */
    BigDecimal midRate(Currency from, Currency to);
}
