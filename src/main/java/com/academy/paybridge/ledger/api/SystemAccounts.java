package com.academy.paybridge.ledger.api;

/**
 * PayBridge's own internal ledger accounts (not owned by any customer). See notes.md, cases 1-5.
 */
public final class SystemAccounts {

    // ---- Funding (dev deposits) ----
    /**
     * Source of test money for /api/dev deposits. Allowed to go negative.
     */
    public static final String NGN_FUNDING = "SYS-NGN-FUNDING";
    public static final String USD_FUNDING = "SYS-USD-FUNDING";

    // ---- External payouts (case 3) ----
    /**
     * Money held while an external transfer is in progress. Should equal the sum of PENDING transfers.
     */
    public static final String NGN_PAYOUT_CLEARING = "SYS-NGN-PAYOUT-CLEARING";
    /**
     * Money that has successfully left PayBridge to other banks.
     */
    public static final String NGN_EXTERNAL_PAYOUTS = "SYS-NGN-EXTERNAL-PAYOUTS";

    // ---- FX (case 4). A conversion is two journals, one per currency, meeting at the positions. ----
    /**
     * Dollars PayBridge holds from remittances and hasn't sold yet.
     */
    public static final String USD_FX_POSITION = "SYS-USD-FX-POSITION";
    /**
     * Naira paid out against those dollars (negative) plus naira received from selling them.
     */
    public static final String NGN_FX_POSITION = "SYS-NGN-FX-POSITION";
    /**
     * Dollars sold to the FX desk / bank.
     */
    public static final String USD_FX_SOLD = "SYS-USD-FX-SOLD";
    /**
     * Naira received from the FX desk / bank for those dollars.
     */
    public static final String NGN_FX_BOUGHT = "SYS-NGN-FX-BOUGHT";
    /**
     * Realised spread. Negative means PayBridge lost money on FX.
     */
    public static final String NGN_FX_REVENUE = "SYS-NGN-FX-REVENUE";

    // ---- Settlement (case 5) ----
    /**
     * Dollars wired in by partners to PayBridge's domiciliary account.
     */
    public static final String USD_INBOUND_WIRES = "SYS-USD-INBOUND-WIRES";

    /**
     * What a partner owes PayBridge. Negative = partner owes us; 0 = fully settled.
     */
    public static String partnerReceivable(String partnerCode) {
        return "SYS-USD-RECV-" + partnerCode;
    }

    private SystemAccounts() {
    }
}
