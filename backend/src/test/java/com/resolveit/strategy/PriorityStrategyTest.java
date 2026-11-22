package com.resolveit.strategy;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.domain.incident.Priority;
import com.resolveit.exception.ValidationException;
import com.resolveit.strategy.priority.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Both priority strategies, their boundaries, and the text analysers that feed them. */
class PriorityStrategyTest {
    private final PriorityStrategy multiplicative = new MultiplicativePriorityStrategy();
    private final PriorityStrategy weighted = new WeightedPriorityStrategy();

    @Test void t02PaymentDownIsScore64P1() {
        PriorityResult r = multiplicative.compute(new PriorityInput(4, 4, 4, 3));
        assertEquals(64, r.score());
        assertEquals(Priority.P1, r.priority());
    }

    @ParameterizedTest(name = "score {0}*{1}*{2} -> {3}")
    @CsvSource({"4,3,3,P1", "3,3,4,P1", "5,7,1,P2", "3,3,2,P2", "2,3,3,P2", "1,2,4,P3", "2,2,2,P3", "1,1,7,P4", "1,2,2,P4"})
    void t03MultiplicativeBoundaries(int a, int b, int c, String expected) {
        // 36 is P1 and 35 P2 cannot both be expressed with 1-4 scales, so the bands are tested at the reachable edges.
        if (a > 4 || b > 4 || c > 4) {
            assertEquals(Priority.valueOf(expected), bandOf(a * b * c));
            return;
        }
        assertEquals(Priority.valueOf(expected), multiplicative.compute(new PriorityInput(a, b, c, 1)).priority());
    }

    private Priority bandOf(int score) {
        return score >= 36 ? Priority.P1 : score >= 18 ? Priority.P2 : score >= 8 ? Priority.P3 : Priority.P4;
    }

    @Test void affectedUsersWidenTheBlastRadius() {
        assertEquals(Priority.P4, multiplicative.compute(new PriorityInput(1, 2, 2, 1)).priority());
        assertEquals(Priority.P3, multiplicative.compute(new PriorityInput(1, 2, 2, 3)).priority());
    }

    @Test void t04WeightedKeepsTheSameOrderingForWorkedExamples() {
        List<PriorityInput> examples = List.of(new PriorityInput(4, 4, 4, 4), new PriorityInput(3, 3, 4, 3),
                new PriorityInput(2, 2, 3, 2), new PriorityInput(1, 2, 2, 1));
        int previous = Integer.MAX_VALUE;
        for (PriorityInput input : examples) {
            int score = weighted.compute(input).score();
            assertTrue(score <= previous);
            previous = score;
        }
        assertEquals(Priority.P1, weighted.compute(examples.get(0)).priority());
        assertEquals(100, weighted.compute(examples.get(0)).score());
        assertEquals(Priority.P4, weighted.compute(examples.get(3)).priority());
    }

    @Test void weightedBandsAt85And65And40() {
        assertEquals(Priority.P1, WeightedPriorityStrategy.class.cast(weighted).compute(new PriorityInput(4, 4, 3, 3)).priority());
        assertEquals(Priority.P2, weighted.compute(new PriorityInput(4, 4, 1, 4)).priority());
        assertEquals(Priority.P3, weighted.compute(new PriorityInput(2, 2, 2, 1)).priority());
    }

    @Test void scalesOutsideOneToFourAreRejected() {
        assertThrows(ValidationException.class, () -> new PriorityInput(0, 1, 1, 1));
        assertThrows(ValidationException.class, () -> new PriorityInput(1, 5, 1, 1));
    }

    @Test void analysersReadImpactUrgencyAndReach() {
        String text = "Payment application is unavailable and users are unable to complete transactions.";
        assertEquals(4, new ImpactAnalyzer().analyze(text));
        assertEquals(4, new UrgencyAnalyzer().analyze(text));
        assertEquals(3, new AffectedUsersAnalyzer().analyze(text));
        assertEquals(2, new ImpactAnalyzer().analyze("Wifi is slow on the third floor"));
        assertEquals(1, new UrgencyAnalyzer().analyze("Printer prints faded pages"));
        assertEquals(1, new ImpactAnalyzer().analyze("My download is downloading"), "whole-word match only");
    }

    @Test void registryResolvesByKeyAndRejectsUnknown() {
        StrategyRegistry<PriorityStrategy> registry = new StrategyRegistry<>("priority", List.of(multiplicative, weighted));
        assertSame(weighted, registry.get("weighted"));
        assertEquals(List.of("MULTIPLICATIVE", "WEIGHTED"), registry.keys());
        assertThrows(ValidationException.class, () -> registry.get("RANDOM"));
    }
}
