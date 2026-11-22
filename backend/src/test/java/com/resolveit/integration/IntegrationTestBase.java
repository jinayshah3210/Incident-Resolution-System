package com.resolveit.integration;

import com.resolveit.config.SwitchableClock;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.repository.*;
import com.resolveit.service.*;
import com.resolveit.web.dto.CreateIncidentRequest;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring context on H2 (PostgreSQL mode) with the real Flyway migrations and ddl-auto=validate.
 * Each test runs in a transaction that is rolled back, so tests are independent.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestBase {
    static final String PAYMENT_TITLE = "Payment application unavailable";
    static final String PAYMENT_TEXT = "Payment application is unavailable and users are unable to complete transactions.";

    @Autowired SwitchableClock clock;
    @Autowired UserRepository users;
    @Autowired IncidentRepository incidents;
    @Autowired AuditEventRepository audit;
    @Autowired NotificationRepository notifications;
    @Autowired SlaViolationRepository violations;
    @Autowired EscalationRepository escalations;
    @Autowired SystemSettingRepository settingRows;
    @Autowired IncidentWorkflowService workflow;
    @Autowired IncidentQueryService queries;
    @Autowired SlaService sla;
    @Autowired SettingsService settings;
    @Autowired AuthService auth;

    @AfterEach void resetClock() { clock.reset(); }

    PortalUser user(String id) { return users.findById(id).orElseThrow(); }

    Incident create(String reporter, String title, String text, String service) {
        return workflow.createIncident(user(reporter), new CreateIncidentRequest(title, text, service, null, false));
    }

    Incident payment() { return create("E101", PAYMENT_TITLE, PAYMENT_TEXT, null); }

    void act(String userId, Incident incident, String action, java.util.Map<String, String> body) {
        workflow.perform(user(userId), incident, action, body);
    }

    long auditCount(Incident incident, String action) {
        return audit.findByIncidentIdIgnoreCaseAndAction(incident.getPublicId(), action).size();
    }
}
