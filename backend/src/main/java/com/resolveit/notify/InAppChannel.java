package com.resolveit.notify;

import com.resolveit.domain.notification.Notification;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.repository.NotificationRepository;
import org.springframework.stereotype.Component;

/** Strategy pattern: the in-app channel stores the notification for the bell panel. */
@Component
public class InAppChannel implements NotificationChannel {
    private final NotificationRepository repository;

    public InAppChannel(NotificationRepository repository) { this.repository = repository; }

    @Override public String name() { return "IN_APP"; }
    @Override public boolean isEnabled() { return true; }

    @Override public void deliver(Notification notification, PortalUser recipient) { repository.save(notification); }
}
