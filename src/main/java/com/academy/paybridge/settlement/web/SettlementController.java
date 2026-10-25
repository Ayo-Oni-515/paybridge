package com.academy.paybridge.settlement.web;

import com.academy.paybridge.settlement.api.LedgerCheckView;
import com.academy.paybridge.settlement.api.SettlementRequest;
import com.academy.paybridge.settlement.api.SettlementView;
import com.academy.paybridge.settlement.domain.SettlementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlements;

    public SettlementController(SettlementService settlements) {
        this.settlements = settlements;
    }

    @PostMapping
    public SettlementView settle(@Valid @RequestBody SettlementRequest request) {
        return settlements.settle(request);
    }

    @GetMapping("/{id}")
    public SettlementView get(@PathVariable UUID id) {
        return settlements.get(id);
    }

    @GetMapping("/ledger-check")
    public LedgerCheckView ledgerCheck() {
        return settlements.ledgerCheck();
    }
}
