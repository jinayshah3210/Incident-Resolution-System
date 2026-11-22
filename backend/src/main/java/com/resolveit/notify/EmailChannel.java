package com.resolveit.notify;

import com.resolveit.domain.notification.EmailOutboxEntry;
import com.resolveit.domain.notification.EmailStatus;
import com.resolveit.domain.notification.Notification;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.exception.ExternalServiceException;
import com.resolveit.repository.EmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern: adapts a Notification into an email. While real mail is disabled (default) the email is
 * written to email_outbox as SIMULATED and logged. When enabled, the MailGateway is called; a network
 * failure is caught, recorded as FAILED and reported as ExternalServiceException to the caller.
 */
@Component
public class EmailChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);
    private final EmailOutboxRepository outbox;
    private final MailGateway gateway;
    private final boolean mailEnabled;

    public EmailChannel(EmailOutboxRepository outbox, MailGateway gateway,
                        @Value("${resolveit.mail.enabled:false}") boolean mailEnabled) {
        this.outbox = outbox;
        this.gateway = gateway;
        this.mailEnabled = mailEnabled;
    }

    @Override public String name() { return "EMAIL"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public void deliver(Notification notification, PortalUser recipient) {
        if (recipient == null || recipient.getEmail() == null) return;
        String subject = "[ResolveIT] " + notification.getTitle()
                + (notification.getIncidentId() == null ? "" : " - " + notification.getIncidentId());
        String body = "Hello " + recipient.getName() + ",\n\n" + notification.getMessage() + "\n\n- ResolveIT Service Desk";
        if (!mailEnabled) {
            outbox.save(new EmailOutboxEntry(recipient.getEmail(), subject, body, EmailStatus.SIMULATED, null, notification.getCreatedAt()));
            log.debug("Simulated email to {}: {}", recipient.getEmail(), subject);
            return;
        }
        try {
            gateway.send(recipient.getEmail(), subject, body);
            outbox.save(new EmailOutboxEntry(recipient.getEmail(), subject, body, EmailStatus.SENT, null, notification.getCreatedAt()));
        } catch (RuntimeException networkFailure) {
            outbox.save(new EmailOutboxEntry(recipient.getEmail(), subject, body, EmailStatus.FAILED,
                    networkFailure.getMessage(), notification.getCreatedAt()));
            throw new ExternalServiceException("The mail server could not be reached.", networkFailure);
        }
    }
}
