package com.resolveit.observer;

import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentObserver;
import com.resolveit.factory.NotificationFactory;
import com.resolveit.service.NotificationService;
import org.springframework.stereotype.Component;

/** Observer pattern: turns incident events into role-specific notifications (recipient matrix in NotificationFactory). */
@Component
public class NotificationObserver implements IncidentObserver {
    private final NotificationFactory factory;
    private final NotificationService notifications;

    public NotificationObserver(NotificationFactory factory, NotificationService notifications) {
        this.factory = factory;
        this.notifications = notifications;
    }

    @Override public int order() { return 30; }

    @Override
    public void onEvent(IncidentEvent event) {
        factory.forEvent(event).forEach(notifications::send);
    }
}
