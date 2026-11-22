package com.resolveit.event;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observer pattern subject: forwards each event to every registered observer. Each observer runs in its own
 * try/catch so a failing observer (for example a broken notification channel) never fails the main workflow.
 */
@Component
public class IncidentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(IncidentEventPublisher.class);
    private final List<IncidentObserver> observers;

    public IncidentEventPublisher(List<IncidentObserver> observers) {
        this.observers = observers.stream().sorted(Comparator.comparingInt(IncidentObserver::order)).toList();
    }

    public void publish(IncidentEvent event) {
        for (IncidentObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (RuntimeException failure) {
                log.warn("Observer {} failed on {} for {}: {}", observer.getClass().getSimpleName(), event.type(),
                        event.incident().getPublicId(), failure.getMessage());
            }
        }
    }

    public List<IncidentObserver> observers() { return observers; }
}
