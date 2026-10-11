package com.academy.paybridge.ledger.api;

/** PayBridge's own internal ledger accounts (not owned by any customer). */
public final class SystemAccounts {

    /** Source of test money for /api/dev deposits. Allowed to go negative. */
    public static final String NGN_FUNDING = "SYS-NGN-FUNDING";

    /** Money held while an external transfer is in progress. Should be 0 when nothing is pending. */
    public static final String NGN_PAYOUT_CLEARING = "SYS-NGN-PAYOUT-CLEARING";

    /** Money that has successfully left PayBridge to other banks. */
    public static final String NGN_EXTERNAL_PAYOUTS = "SYS-NGN-EXTERNAL-PAYOUTS";

    private SystemAccounts() {}
}
