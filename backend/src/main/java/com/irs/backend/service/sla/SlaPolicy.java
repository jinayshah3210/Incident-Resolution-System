package com.irs.backend.service.sla;
import org.springframework.stereotype.Component;
import java.time.Duration;
import com.irs.backend.model.enums.Priority;
@Component
public class SlaPolicy {

    public Duration getResponseTime(Priority priority) {
        return switch (priority) {
            case P1 -> Duration.ofMinutes(15);
            case P2 -> Duration.ofMinutes(30);
            case P3 -> Duration.ofHours(4);
            case P4 -> Duration.ofHours(8);
        };
    }

    public Duration getResolutionTime(Priority priority) {
        return switch (priority) {
            case P1 -> Duration.ofHours(4);
            case P2 -> Duration.ofHours(8);
            case P3 -> Duration.ofHours(24);
            case P4 -> Duration.ofHours(72);
        };
    }
}