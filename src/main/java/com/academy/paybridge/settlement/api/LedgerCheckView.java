package com.academy.paybridge.settlement.api;

import com.academy.paybridge.ledger.api.TrialBalanceLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The daily health check from notes.md.
 *   ledgerBalanced:       every currency's accounts add up to 0 (double entry intact)
 *   clearingMatchesPending: money held in payout clearing = sum of PENDING external transfers
 */
public record LedgerCheckView(
        List<TrialBalanceLine> trialBalance,
        boolean ledgerBalanced,
        BigDecimal payoutClearing,
        BigDecimal pendingExternalTransfers,
        boolean clearingMatchesPending,
        Instant checkedAt
) {
    public boolean healthy() {
        return ledgerBalanced && clearingMatchesPending;
    }
}
