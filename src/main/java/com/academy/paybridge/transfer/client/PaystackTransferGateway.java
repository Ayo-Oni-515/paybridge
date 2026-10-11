package com.academy.paybridge.transfer.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.academy.paybridge.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Paystack Transfers API. Flow: resolve account -> create recipient -> initiate transfer -> webhook.
 * Check Paystack's current docs; field names and test-mode rules can change.
 */
@Component
@ConditionalOnProperty(name = "paybridge.gateway", havingValue = "paystack")
class PaystackTransferGateway implements TransferGateway {

    // Paystack wraps every response as { "status": true, "message": "...", "data": {...} }
    record PaystackResponse<T>(boolean status, String message, T data) {
    }

    record ResolveData(@JsonProperty("account_number") String accountNumber,
                       @JsonProperty("account_name") String accountName) {
    }

    record RecipientData(@JsonProperty("recipient_code") String recipientCode) {
    }

    record TransferData(@JsonProperty("transfer_code") String transferCode, String status) {
    }

    private final RestClient http;

    PaystackTransferGateway(RestClient.Builder builder,
                            @Value("${paystack.base-url}") String baseUrl,
                            @Value("${paystack.secret-key}") String secretKey) {
        if (secretKey.isBlank()) {
            throw new IllegalStateException("Set the PAYSTACK_SECRET_KEY environment variable");
        }
        this.http = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .build();
    }

    @Override
    public ResolvedAccount resolveAccount(String accountNumber, String bankCode) {
        try {
            PaystackResponse<ResolveData> response = http.get()
                    .uri(uri -> uri.path("/bank/resolve")
                            .queryParam("account_number", accountNumber)
                            .queryParam("bank_code", bankCode)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<PaystackResponse<ResolveData>>() {
                    });
            if (response == null || !response.status() || response.data() == null) {
                throw notResolved();
            }
            return new ResolvedAccount(response.data().accountNumber(), response.data().accountName());
        } catch (HttpClientErrorException e) {
            throw notResolved();
        }
    }

    @Override
    public GatewayResult send(PayoutRequest request) {
        try {
            String recipientCode = createRecipient(request);

            PaystackResponse<TransferData> response = http.post()
                    .uri("/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "source", "balance",
                            "amount", request.amount().toMinorUnits(),   // kobo
                            "currency", "NGN",
                            "recipient", recipientCode,
                            "reason", request.narration(),
                            "reference", request.reference()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<PaystackResponse<TransferData>>() {
                    });

            if (response == null || response.data() == null) {
                return new GatewayResult(GatewayStatus.PENDING, null, "Empty response; waiting for webhook");
            }
            return new GatewayResult(mapStatus(response.data().status()),
                    response.data().transferCode(), response.message());

        } catch (HttpClientErrorException e) {
            // 4xx: Paystack refused the request, so no money left. Safe to refund.
            throw new GatewayRejectedException("Paystack rejected the transfer: " + e.getResponseBodyAsString());
        }
        // Timeouts and 5xx are NOT caught: we don't know if money moved, so the caller keeps it PENDING.
    }

    private String createRecipient(PayoutRequest request) {
        PaystackResponse<RecipientData> response = http.post()
                .uri("/transferrecipient")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "type", "nuban",
                        "name", request.accountName(),
                        "account_number", request.accountNumber(),
                        "bank_code", request.bankCode(),
                        "currency", "NGN"))
                .retrieve()
                .body(new ParameterizedTypeReference<PaystackResponse<RecipientData>>() {
                });
        if (response == null || response.data() == null) {
            throw new GatewayRejectedException("Paystack did not return a recipient code");
        }
        return response.data().recipientCode();
    }

    private static GatewayStatus mapStatus(String status) {
        if (status == null) return GatewayStatus.PENDING;
        return switch (status.toLowerCase()) {
            case "success" -> GatewayStatus.SUCCESS;
            case "failed", "reversed", "rejected", "abandoned" -> GatewayStatus.FAILED;
            default -> GatewayStatus.PENDING; // pending, otp, queued, received...
        };
    }

    private static BusinessException notResolved() {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_NOT_RESOLVED",
                "Could not verify the destination account");
    }
}
