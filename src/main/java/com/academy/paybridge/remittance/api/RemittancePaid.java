package com.academy.paybridge.remittance.api;

/** Published after the beneficiary's wallet is credited. */
public record RemittancePaid(RemittanceView remittance) {
}
