package com.academy.paybridge.settlement.domain;

import com.academy.paybridge.ledger.api.LedgerApi;
import com.academy.paybridge.ledger.api.SystemAccounts;
import com.academy.paybridge.ledger.api.TrialBalanceLine;
import com.academy.paybridge.remittance.api.RemittanceApi;
import com.academy.paybridge.remittance.api.RemittanceView;
import com.academy.paybridge.settlement.api.LedgerCheckView;
import com.academy.paybridge.settlement.api.SettlementRequest;
import com.academy.paybridge.settlement.api.SettlementView;
import com.academy.paybridge.settlement.repository.SettlementRepository;
import com.academy.paybridge.shared.exception.BusinessException;
import com.academy.paybridge.shared.money.Currency;
import com.academy.paybridge.shared.money.Money;
import com.academy.paybridge.transfer.api.TransferApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** notes.md case 5: the partner pays in bulk, then we reconcile the three lists. */
@Service
public class SettlementService {

    private final SettlementRepository settlements;
    private final RemittanceApi remittances;
    private final LedgerApi ledger;
    private final TransferApi transfers;
    private final Clock clock;

    public SettlementService(SettlementRepository settlements, RemittanceApi remittances, LedgerApi ledger,
                             TransferApi transfers, Clock clock) {
        this.settlements = settlements;
        this.remittances = remittances;
        this.ledger = ledger;
        this.transfers = transfers;
        this.clock = clock;
    }

    @Transactional
    public SettlementView settle(SettlementRequest request) {
        String partner = request.partnerCode();
        if (!remittances.partnerCodes().contains(partner)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "UNKNOWN_PARTNER", "Partner " + partner + " is not onboarded");
        }
        // The same wire can't be booked twice.
        Optional<Settlement> existing = settlements.findByPartnerCodeAndBankReference(partner, request.bankReference());
        if (existing.isPresent()) {
            return existing.get().toView();
        }

        List<RemittanceView> unsettled = remittances.findUnsettled(partner);
        Set<String> unsettledRefs = new HashSet<>();
        unsettled.forEach(r -> unsettledRefs.add(r.partnerReference()));

        Set<String> alreadySettled = new HashSet<>();
        for (SettlementRequest.Line line : request.lines()) {
            String ref = line.partnerReference();
            if (!unsettledRefs.contains(ref) && remittances.findByPartnerReference(partner, ref).isPresent()) {
                alreadySettled.add(ref);
            }
        }

        Reconciler.Result result = Reconciler.reconcile(
                request.lines().stream().map(l -> new Reconciler.ReportedLine(l.partnerReference(), l.usdAmount())).toList(),
                unsettled.stream().map(r -> new Reconciler.OurRemittance(r.id(), r.partnerReference(), r.usdAmount(),
                        LocalDate.ofInstant(r.createdAt(), ZoneOffset.UTC))).toList(),
                alreadySettled, request.wireAmount(), request.settlementDate());

        Settlement settlement = settlements.save(new Settlement(partner, request.settlementDate(),
                request.bankReference(), request.wireAmount(), result));

        // Book the money that actually arrived, matched or not. Whatever doesn't match
        // stays visible as a non-zero receivable until someone resolves the break.
        if (request.wireAmount().signum() > 0) {
            ledger.move("settle-" + settlement.getId(), SystemAccounts.USD_INBOUND_WIRES,
                    SystemAccounts.partnerReceivable(partner), Money.of(request.wireAmount(), Currency.USD),
                    "Partner wire " + request.bankReference());
        }
        remittances.markSettled(result.matchedIds(), settlement.getId());
        settlement.recordReceivableAfter(ledger.balanceOf(SystemAccounts.partnerReceivable(partner)).amount());

        return settlement.toView();
    }

    @Transactional(readOnly = true)
    public SettlementView get(UUID id) {
        return settlements.findById(id).map(Settlement::toView)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "SETTLEMENT_NOT_FOUND",
                        "Settlement not found: " + id));
    }

    /**
     * Not an atomic snapshot: a transfer finishing between the two reads can cause a brief false alarm.
     * Re-run before investigating.
     */
    @Transactional(readOnly = true)
    public LedgerCheckView ledgerCheck() {
        List<TrialBalanceLine> trialBalance = ledger.trialBalance();
        boolean balanced = trialBalance.stream().allMatch(TrialBalanceLine::isBalanced);

        BigDecimal clearing = ledger.balanceOf(SystemAccounts.NGN_PAYOUT_CLEARING).amount();
        BigDecimal pending = transfers.pendingExternalTotal().amount();

        return new LedgerCheckView(trialBalance, balanced, clearing, pending,
                clearing.compareTo(pending) == 0, clock.instant());
    }
}
