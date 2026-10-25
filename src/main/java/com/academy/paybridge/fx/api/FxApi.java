package com.academy.paybridge.fx.api;

import java.util.UUID;

/**
 * The only way other modules may talk to the fx module.
 */
public interface FxApi {

    /**
     * Locks in the quote for one use. Must run inside the caller's transaction:
     * if the caller rolls back (e.g. ledger failure), the quote becomes usable again.
     *
     * @throws com.academy.paybridge.shared.exception.BusinessException if expired, used or unknown
     */
    QuoteView consumeQuote(UUID quoteId, String usedBy);
}
