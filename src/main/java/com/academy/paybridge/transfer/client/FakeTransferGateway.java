package com.academy.paybridge.transfer.client;

import com.academy.paybridge.shared.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Offline simulator so you can build and test without internet or API keys.
 *   account number starting with 8 -> name enquiry fails (account not found)
 *   account number starting with 9 -> payout fails (sender is refunded)
 *   anything else                  -> instant success
 */
@Component
@ConditionalOnProperty(name = "paybridge.gateway", havingValue = "fake", matchIfMissing = true)
class FakeTransferGateway implements TransferGateway {

    @Override
    public ResolvedAccount resolveAccount(String accountNumber, String bankCode) {
        if (accountNumber.startsWith("8")) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_NOT_RESOLVED",
                    "Could not verify the destination account");
        }
        return new ResolvedAccount(accountNumber, "TEST ACCOUNT " + accountNumber.substring(6));
    }

    @Override
    public GatewayResult send(PayoutRequest request) {
        if (request.accountNumber().startsWith("9")) {
            return GatewayResult.failed("Simulated failure: destination bank unavailable");
        }
        return new GatewayResult(GatewayStatus.SUCCESS, "FAKE-" + UUID.randomUUID(), "Simulated success");
    }
}
