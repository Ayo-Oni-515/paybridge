package com.academy.paybridge.account.repository;

import com.academy.paybridge.account.domain.Account;
import com.academy.paybridge.shared.money.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByCustomerIdAndCurrency(UUID customerId, Currency currency);

    List<Account> findByCustomerIdOrderByCreatedAt(UUID customerId);
}
