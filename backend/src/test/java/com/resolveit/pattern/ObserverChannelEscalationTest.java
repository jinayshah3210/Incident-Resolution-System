package com.resolveit.pattern;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.TestData;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.notification.EmailOutboxEntry;
import com.resolveit.domain.notification.EmailStatus;
import com.resolveit.domain.notification.Notification;
import com.resolveit.domain.notification.NotificationType;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentEventPublisher;
import com.resolveit.event.IncidentEventType;
import com.resolveit.event.IncidentObserver;
import com.resolveit.exception.ExternalServiceException;
import com.resolveit.notify.EmailChannel;
import com.resolveit.repository.EmailOutboxRepository;
import com.resolveit.security.JwtTokenService;
import com.resolveit.exception.AuthenticationRequiredException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Observer isolation, the email Adapter and JWT handling, tested without Spring. */
class ObserverChannelEscalationTest {
    private final Incident incident = TestData.incidentIn(IncidentStatus.ASSIGNED);
    private final IncidentEvent event = IncidentEvent.of(IncidentEventType.OFFER_CREATED, incident, "SYSTEM", "offered", null, TestData.T0);

    @Test void publisherDeliversToObserversInOrder() {
        List<String> calls = new ArrayList<>();
        IncidentObserver second = new IncidentObserver() {
            public void onEvent(IncidentEvent e) { calls.add("second"); }
            public int order() { return 20; }
        };
        IncidentObserver first = new IncidentObserver() {
            public void onEvent(IncidentEvent e) { calls.add("first"); }
            public int order() { return 10; }
        };
        new IncidentEventPublisher(List.of(second, first)).publish(event);
        assertEquals(List.of("first", "second"), calls);
    }

    @Test void failingObserverDoesNotStopTheOthers() {
        List<String> calls = new ArrayList<>();
        IncidentObserver broken = e -> { throw new IllegalStateException("channel down"); };
        IncidentObserver healthy = e -> calls.add(e.type().name());
        assertDoesNotThrow(() -> new IncidentEventPublisher(List.of(broken, healthy)).publish(event));
        assertEquals(List.of("OFFER_CREATED"), calls);
    }

    @Test void eventAttributesAreImmutableCopies() {
        IncidentEvent withRecipient = event.with("recipient", "E101");
        assertNull(event.attribute("recipient"));
        assertEquals("E101", withRecipient.attribute("recipient"));
    }

    private static EmailOutboxRepository outbox(List<EmailOutboxEntry> sink) {
        return (EmailOutboxRepository) Proxy.newProxyInstance(EmailOutboxRepository.class.getClassLoader(),
                new Class[]{EmailOutboxRepository.class}, (proxy, method, args) -> {
                    if (method.getName().equals("save")) { sink.add((EmailOutboxEntry) args[0]); return args[0]; }
                    return null;
                });
    }

    private final Notification notification = new Notification("E101", "INC9999", NotificationType.MESSAGE, "Hi", "Body", TestData.T0);
    private final PortalUser recipient = TestData.REPORTER;

    @Test void emailAdapterWritesSimulatedOutboxEntry() {
        List<EmailOutboxEntry> sink = new ArrayList<>();
        new EmailChannel(outbox(sink), (to, s, b) -> fail("gateway must not be called when mail is disabled"), false)
                .deliver(notification, recipient);
        assertEquals(1, sink.size());
        assertEquals(EmailStatus.SIMULATED, sink.get(0).getStatus());
        assertTrue(sink.get(0).getSubject().contains("INC9999"));
    }

    @Test void networkFailureIsRecordedAndRaisedAsExternalServiceException() {
        List<EmailOutboxEntry> sink = new ArrayList<>();
        EmailChannel channel = new EmailChannel(outbox(sink), (to, s, b) -> { throw new RuntimeException("connection refused"); }, true);
        ExternalServiceException e = assertThrows(ExternalServiceException.class, () -> channel.deliver(notification, recipient));
        assertEquals("NETWORK_FAILURE", e.getErrorCode());
        assertEquals(EmailStatus.FAILED, sink.get(0).getStatus());
    }

    @Test void jwtIsSignedExpiresAfterFourHoursAndCanBeRevoked() {
        Instant base = Instant.parse("2026-11-21T00:00:00Z");
        String secret = "local-demo-signing-secret-change-in-env-please";
        JwtTokenService issuer = new JwtTokenService(secret, Clock.fixed(base, ZoneOffset.UTC));
        String token = issuer.issue("E101", "EMPLOYEE");
        assertEquals("E101", issuer.subject(token));
        String tampered = token.substring(0, token.length() - 2) + "AA";
        assertThrows(AuthenticationRequiredException.class, () -> issuer.subject(tampered));
        JwtTokenService later = new JwtTokenService(secret, Clock.fixed(base.plusSeconds(4 * 3600), ZoneOffset.UTC));
        assertThrows(AuthenticationRequiredException.class, () -> later.subject(token));
        issuer.revoke(token);
        assertThrows(AuthenticationRequiredException.class, () -> issuer.subject(token));
    }
}
