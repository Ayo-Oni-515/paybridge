package com.academy.paybridge.settlement.domain;

import com.academy.paybridge.settlement.api.BreakType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Compares three lists: the partner's report, our ledger's unsettled remittances, and the wire.
 * Pure Java, no Spring or database, so every rule is easy to unit test.
 */
public final class Reconciler {

    public record ReportedLine(String partnerReference, BigDecimal usdAmount) {}

    public record OurRemittance(UUID id, String partnerReference, BigDecimal usdAmount, LocalDate createdOn) {}

    public record Break(BreakType type, String partnerReference, BigDecimal expected, BigDecimal actual, String detail) {}

    public record Result(List<UUID> matchedIds, BigDecimal matchedTotal, BigDecimal reportedTotal, List<Break> breaks) {}

    private Reconciler() {}

    /**
     * @param alreadySettledRefs references from the report that we settled in an earlier cycle
     */
    public static Result reconcile(List<ReportedLine> report, List<OurRemittance> unsettled,
                                   Set<String> alreadySettledRefs, BigDecimal wireAmount, LocalDate settlementDate) {
        Map<String, OurRemittance> ours = new HashMap<>();
        for (OurRemittance r : unsettled) ours.put(r.partnerReference(), r);

        List<UUID> matched = new ArrayList<>();
        BigDecimal matchedTotal = BigDecimal.ZERO;
        BigDecimal reportedTotal = BigDecimal.ZERO;
        List<Break> breaks = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ReportedLine line : report) {
            String ref = line.partnerReference();
            reportedTotal = reportedTotal.add(line.usdAmount());

            if (!seen.add(ref)) {
                breaks.add(new Break(BreakType.DUPLICATE_IN_REPORT, ref, null, line.usdAmount(),
                        "Reference appears more than once in the report"));
                continue;
            }
            OurRemittance mine = ours.get(ref);
            if (mine == null) {
                BreakType type = alreadySettledRefs.contains(ref) ? BreakType.ALREADY_SETTLED : BreakType.MISSING_IN_PAYBRIDGE;
                breaks.add(new Break(type, ref, null, line.usdAmount(),
                        type == BreakType.ALREADY_SETTLED ? "Settled in an earlier cycle" : "No remittance with this reference"));
            } else if (mine.usdAmount().compareTo(line.usdAmount()) != 0) { // compareTo: 100.0 equals 100.00
                breaks.add(new Break(BreakType.AMOUNT_MISMATCH, ref, mine.usdAmount(), line.usdAmount(),
                        "Partner reported a different amount"));
            } else {
                matched.add(mine.id());
                matchedTotal = matchedTotal.add(mine.usdAmount());
            }
        }

        // Ours but not in the report. Ignore anything newer than the settlement date: it belongs to the next cycle.
        for (OurRemittance r : unsettled) {
            if (!seen.contains(r.partnerReference()) && !r.createdOn().isAfter(settlementDate)) {
                breaks.add(new Break(BreakType.MISSING_IN_REPORT, r.partnerReference(), r.usdAmount(), null,
                        "We paid this out but the partner didn't report it"));
            }
        }

        if (wireAmount.compareTo(reportedTotal) != 0) {
            breaks.add(new Break(BreakType.WIRE_MISMATCH, null, reportedTotal, wireAmount,
                    "Wire differs from report total by " + wireAmount.subtract(reportedTotal).toPlainString()));
        }

        return new Result(List.copyOf(matched), matchedTotal, reportedTotal, List.copyOf(breaks));
    }
}
