package com.academy.paybridge.remittance.repository;

import com.academy.paybridge.remittance.domain.Remittance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RemittanceRepository extends JpaRepository<Remittance, UUID> {

    Optional<Remittance> findByPartnerCodeAndPartnerReference(String partnerCode, String partnerReference);

    List<Remittance> findByPartnerCodeAndSettlementIdIsNullOrderByCreatedAt(String partnerCode);

    /** Locked: the transfer webhook and the original request may both try to update the payout. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Remittance r where r.payoutIdempotencyKey = :key")
    Optional<Remittance> findByPayoutKeyForUpdate(@Param("key") String payoutIdempotencyKey);
}
