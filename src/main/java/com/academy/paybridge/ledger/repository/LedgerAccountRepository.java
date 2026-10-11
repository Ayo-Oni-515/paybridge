package com.academy.paybridge.ledger.repository;

import com.academy.paybridge.ledger.domain.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, String> {

    /**
     * SELECT ... FOR UPDATE: other transactions wait until we commit. Prevents double-spending.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LedgerAccount a where a.code = :code")
    Optional<LedgerAccount> findForUpdate(@Param("code") String code);
}
