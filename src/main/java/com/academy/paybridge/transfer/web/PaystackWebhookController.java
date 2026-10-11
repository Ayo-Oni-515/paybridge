package com.academy.paybridge.transfer.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.academy.paybridge.shared.exception.BusinessException;
import com.academy.paybridge.transfer.client.GatewayStatus;
import com.academy.paybridge.transfer.domain.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.JacksonException;          // Jackson 3: was com.fasterxml.jackson.*
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Paystack calls this URL when a transfer finishes.
 * Set it in Paystack Dashboard -> Settings -> API Keys & Webhooks,
 * e.g. https://<your-ngrok-id>.ngrok-free.app/api/webhooks/paystack
 */
@RestController
@RequestMapping("/api/webhooks/paystack")
@ConditionalOnProperty(name = "paybridge.gateway", havingValue = "paystack")
public class PaystackWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaystackWebhookController.class);

    /** Only the fields we need from Paystack's webhook body. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaystackEvent(String event, EventData data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EventData(String reference, @JsonProperty("transfer_code") String transferCode) {}

    private final TransferService transfers;
    private final JsonMapper jsonMapper;
    private final byte[] secretKey;

    public PaystackWebhookController(TransferService transfers, JsonMapper jsonMapper,
                                     @Value("${paystack.secret-key}") String secretKey) {
        this.transfers = transfers;
        this.jsonMapper = jsonMapper;
        this.secretKey = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestHeader(value = "x-paystack-signature", required = false) String signature,
                                        @RequestBody byte[] body) {
        // Anyone can POST to this URL. The signature proves it really came from Paystack.
        if (!signatureIsValid(body, signature)) {
            log.warn("Rejected webhook with invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaystackEvent event;
        try {
            event = jsonMapper.readValue(body, PaystackEvent.class); // Jackson 3 exceptions are unchecked
        } catch (JacksonException e) {
            log.warn("Unreadable webhook body", e);
            return ResponseEntity.badRequest().build();
        }

        GatewayStatus outcome = switch (event.event()) {
            case "transfer.success" -> GatewayStatus.SUCCESS;
            case "transfer.failed", "transfer.reversed" -> GatewayStatus.FAILED;
            case null, default -> null; // other events aren't handled yet
        };

        if (outcome != null && event.data() != null && event.data().reference() != null) {
            try {
                transfers.applyOutcome(event.data().reference(), outcome, event.data().transferCode(), event.event());
            } catch (BusinessException e) {
                log.warn("Webhook for unknown transfer reference {}", event.data().reference());
            }
        }
        // Reply 200 quickly, otherwise Paystack keeps retrying.
        return ResponseEntity.ok().build();
    }

    private boolean signatureIsValid(byte[] body, String signature) {
        if (signature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA512"));
            String expected = HexFormat.of().formatHex(mac.doFinal(body));
            // Constant-time comparison so attackers can't guess the signature byte by byte.
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC not available", e);
        }
    }
}
