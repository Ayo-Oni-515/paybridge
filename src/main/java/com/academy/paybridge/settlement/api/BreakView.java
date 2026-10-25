package com.academy.paybridge.settlement.api;

import java.math.BigDecimal;

public record BreakView(BreakType type, String partnerReference, BigDecimal expectedUsd, BigDecimal actualUsd,
                        String detail) {}
