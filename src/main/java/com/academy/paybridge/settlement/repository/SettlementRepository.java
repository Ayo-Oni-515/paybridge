package com.academy.paybridge.settlement.repository;

import com.academy.paybridge.settlement.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Optional<Settlement> findByPartnerCodeAndBankReference(String partnerCode, String bankReference);
}
