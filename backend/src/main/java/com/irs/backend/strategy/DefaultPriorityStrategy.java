
package com.irs.backend.strategy;
import org.springframework.stereotype.Component;
import com.irs.backend.model.enums.Priority;
import com.irs.backend.model.enums.Impact;
import com.irs.backend.model.enums.Urgency;
@Component
public class DefaultPriorityStrategy
        implements PriorityCalculationStrategy {

    @Override
    public Priority calculate(Impact impact, Urgency urgency) {

        if (impact == null || urgency == null) {
            throw new IllegalArgumentException(
                "Impact and urgency cannot be null"
            );
        }

        switch (impact) {

            case HIGH:
                switch (urgency) {
                    case HIGH:
                    case MEDIUM:
                        return Priority.P1;

                    case LOW:
                        return Priority.P2;
                }
                break;

            case MEDIUM:
                switch (urgency) {
                    case HIGH:
                    case MEDIUM:
                        return Priority.P2;

                    case LOW:
                        return Priority.P3;
                }
                break;

            case LOW:
                switch (urgency) {
                    case HIGH:
                    case MEDIUM:
                        return Priority.P3;

                    case LOW:
                        return Priority.P4;
                }
                break;
        }

        throw new IllegalStateException(
            "Unexpected impact or urgency value"
        );
    }
}
