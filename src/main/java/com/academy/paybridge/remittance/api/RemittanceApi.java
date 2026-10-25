package com.academy.paybridge.remittance.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Used by the settlement module to reconcile partner reports. */
public interface RemittanceApi {

    Set<String> partnerCodes();

    /** Remittances the partner hasn't paid us for yet. */
    List<RemittanceView> findUnsettled(String partnerCode);

    Optional<RemittanceView> findByPartnerReference(String partnerCode, String partnerReference);

    /** Must run inside the caller's transaction. */
    void markSettled(Collection<UUID> remittanceIds, UUID settlementId);
}
