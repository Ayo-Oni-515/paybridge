package com.academy.paybridge.settlement;

import com.academy.paybridge.settlement.api.BreakType;
import com.academy.paybridge.settlement.domain.Reconciler;
import com.academy.paybridge.settlement.domain.Reconciler.OurRemittance;
import com.academy.paybridge.settlement.domain.Reconciler.ReportedLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReconcilerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    private static BigDecimal usd(String v) { return new BigDecimal(v); }

    private static OurRemittance ours(String ref, String amount, LocalDate on) {
        return new OurRemittance(UUID.randomUUID(), ref, usd(amount), on);
    }

    @Test
    void cleanDayReconciles() {
        var a = ours("A", "100.00", DAY);
        var b = ours("B", "250.00", DAY);

        var result = Reconciler.reconcile(
                List.of(new ReportedLine("A", usd("100.00")), new ReportedLine("B", usd("250"))),
                List.of(a, b), Set.of(), usd("350.00"), DAY);

        assertThat(result.breaks()).isEmpty();
        assertThat(result.matchedIds()).containsExactly(a.id(), b.id());
        assertThat(result.matchedTotal()).isEqualByComparingTo("350.00");
    }

    @Test
    void findsEveryKindOfBreak() {
        var result = Reconciler.reconcile(
                List.of(new ReportedLine("A", usd("100.00")),    // matches
                        new ReportedLine("B", usd("90.00")),     // we have 100
                        new ReportedLine("X", usd("40.00")),     // we never saw it
                        new ReportedLine("OLD", usd("10.00")),   // settled last week
                        new ReportedLine("A", usd("100.00"))),   // duplicate line
                List.of(ours("A", "100.00", DAY), ours("B", "100.00", DAY),
                        ours("C", "70.00", DAY),                 // partner forgot it
                        ours("TOMORROW", "5.00", DAY.plusDays(1))), // next cycle: not a break
                Set.of("OLD"), usd("300.00"), DAY);

        assertThat(result.breaks()).extracting(Reconciler.Break::type).containsExactlyInAnyOrder(
                BreakType.AMOUNT_MISMATCH, BreakType.MISSING_IN_PAYBRIDGE, BreakType.ALREADY_SETTLED,
                BreakType.DUPLICATE_IN_REPORT, BreakType.MISSING_IN_REPORT, BreakType.WIRE_MISMATCH);
        assertThat(result.matchedIds()).hasSize(1);
        assertThat(result.reportedTotal()).isEqualByComparingTo("340.00");
    }

    @Test
    void shortWireIsABreakEvenWhenEveryLineMatches() {
        var result = Reconciler.reconcile(List.of(new ReportedLine("A", usd("100.00"))),
                List.of(ours("A", "100.00", DAY)), Set.of(), usd("99.00"), DAY);

        assertThat(result.matchedIds()).hasSize(1);
        assertThat(result.breaks()).singleElement()
                .satisfies(b -> assertThat(b.type()).isEqualTo(BreakType.WIRE_MISMATCH));
    }
}
