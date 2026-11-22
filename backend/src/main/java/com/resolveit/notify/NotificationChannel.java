package com.resolveit.notify;

import com.resolveit.domain.notification.Notification;
import com.resolveit.domain.user.PortalUser;

/**
 * Strategy pattern for delivery: every enabled channel receives every notification.
 * A new channel (SMS, Teams) is one new bean implementing this interface; NotificationService does not change.
 */
public interface NotificationChannel {
    String name();

    boolean isEnabled();

    void deliver(Notification notification, PortalUser recipient);
}
