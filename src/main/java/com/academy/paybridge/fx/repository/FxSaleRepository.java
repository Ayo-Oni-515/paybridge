package com.academy.paybridge.fx.repository;

import com.academy.paybridge.fx.domain.FxSale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FxSaleRepository extends JpaRepository<FxSale, UUID> {

    Optional<FxSale> findByIdempotencyKey(String idempotencyKey);
}
