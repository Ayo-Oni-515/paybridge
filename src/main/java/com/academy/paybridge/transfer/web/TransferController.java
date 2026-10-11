package com.academy.paybridge.transfer.web;

import com.academy.paybridge.transfer.api.ExternalTransferRequest;
import com.academy.paybridge.transfer.api.InternalTransferRequest;
import com.academy.paybridge.transfer.api.TransferStatus;
import com.academy.paybridge.transfer.api.TransferView;
import com.academy.paybridge.transfer.domain.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping("/internal")
    public TransferView internal(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                 @Valid @RequestBody InternalTransferRequest request) {
        return transfers.sendInternal(idempotencyKey, request);
    }

    /**
     * 200 if finished, 202 Accepted if still PENDING (check again later).
     */
    @PostMapping("/external")
    public ResponseEntity<TransferView> external(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                 @Valid @RequestBody ExternalTransferRequest request) {
        TransferView view = transfers.sendExternal(idempotencyKey, request);
        HttpStatus status = view.status() == TransferStatus.PENDING ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(view);
    }

    @GetMapping("/{id}")
    public TransferView get(@PathVariable UUID id) {
        return transfers.get(id);
    }
}
