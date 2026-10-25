package com.academy.paybridge.transfer.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.paybridge.transfer.api.TransferStatus;
import com.academy.paybridge.transfer.api.TransferType;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    /**
     * Locked, so a webhook and the initial API response can't both settle the same transfer.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transfer t where t.reference = :reference")
    Optional<Transfer> findByReferenceForUpdate(@Param("reference") String reference);

    /**
     * Returns null (not 0) when no rows match.
     */
    @Query("select sum(t.amount) from Transfer t where t.type = :type and t.status = :status")
    BigDecimal sumAmount(@Param("type") TransferType type, @Param("status") TransferStatus status);
}
