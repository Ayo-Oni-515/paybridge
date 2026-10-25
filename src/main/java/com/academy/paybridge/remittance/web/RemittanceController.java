package com.academy.paybridge.remittance.web;

import com.academy.paybridge.remittance.api.CreateRemittanceRequest;
import com.academy.paybridge.remittance.api.RemittanceView;
import com.academy.paybridge.remittance.domain.RemittanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Called by partner IMTOs. Flow: POST /api/fx/quotes -> POST /api/remittances with the quoteId. */
@RestController
@RequestMapping("/api/remittances")
public class RemittanceController {

    private final RemittanceService remittances;

    public RemittanceController(RemittanceService remittances) {
        this.remittances = remittances;
    }

    @PostMapping
    public RemittanceView receive(@Valid @RequestBody CreateRemittanceRequest request) {
        return remittances.receive(request);
    }

    @GetMapping("/{id}")
    public RemittanceView get(@PathVariable UUID id) {
        return remittances.get(id);
    }
}
