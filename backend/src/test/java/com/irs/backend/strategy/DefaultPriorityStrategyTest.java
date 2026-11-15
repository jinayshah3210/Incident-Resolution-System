
package com.irs.backend.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.irs.backend.model.enums.*;

class DefaultPriorityStrategyTest {

    private final PriorityCalculationStrategy strategy =
            new DefaultPriorityStrategy();

    @Test
    void testAllNineCombinations() {

        assertEquals(Priority.P1,
                strategy.calculate(Impact.HIGH, Urgency.HIGH));

        assertEquals(Priority.P1,
                strategy.calculate(Impact.HIGH, Urgency.MEDIUM));

        assertEquals(Priority.P2,
                strategy.calculate(Impact.HIGH, Urgency.LOW));

        assertEquals(Priority.P2,
                strategy.calculate(Impact.MEDIUM, Urgency.HIGH));

        assertEquals(Priority.P2,
                strategy.calculate(Impact.MEDIUM, Urgency.MEDIUM));

        assertEquals(Priority.P3,
                strategy.calculate(Impact.MEDIUM, Urgency.LOW));

        assertEquals(Priority.P3,
                strategy.calculate(Impact.LOW, Urgency.HIGH));

        assertEquals(Priority.P3,
                strategy.calculate(Impact.LOW, Urgency.MEDIUM));

        assertEquals(Priority.P4,
                strategy.calculate(Impact.LOW, Urgency.LOW));
    }

    @Test
    void testNullInputs() {

        assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(null, Urgency.HIGH));

        assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(Impact.HIGH, null));
    }
}
