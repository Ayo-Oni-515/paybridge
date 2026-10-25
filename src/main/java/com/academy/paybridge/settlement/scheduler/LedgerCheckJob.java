package com.academy.paybridge.settlement.scheduler;

import com.academy.paybridge.settlement.api.LedgerCheckView;
import com.academy.paybridge.settlement.domain.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the ledger health check every night. In production this would page someone, not just log. */
@Component
class LedgerCheckJob {

    private static final Logger log = LoggerFactory.getLogger(LedgerCheckJob.class);

    private final SettlementService settlement;

    LedgerCheckJob(SettlementService settlement) {
        this.settlement = settlement;
    }

    @Scheduled(cron = "${paybridge.settlement.ledger-check-cron:0 0 1 * * *}", zone = "Africa/Lagos")
    void run() {
        LedgerCheckView check = settlement.ledgerCheck();
        if (check.healthy()) {
            log.info("Ledger check OK: {}", check.trialBalance());
        } else {
            log.error("LEDGER CHECK FAILED: balanced={}, clearing={}, pending={}",
                    check.ledgerBalanced(), check.payoutClearing(), check.pendingExternalTransfers());
        }
    }
}
