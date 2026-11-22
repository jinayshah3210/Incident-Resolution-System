package com.resolveit.seed;

import com.resolveit.config.SwitchableClock;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.escalation.EscalationService;
import com.resolveit.exception.ItsmException;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.repository.UserRepository;
import com.resolveit.service.ChatService;
import com.resolveit.service.IncidentWorkflowService;
import com.resolveit.service.SlaService;
import com.resolveit.web.dto.CreateIncidentRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo mode: generates November 2026 incidents by driving the real services with a pinned clock, so every
 * incident has a consistent lifecycle, audit trail, notifications, SLA warnings/violations and escalations.
 * Runs only when the incidents table is empty.
 */
@Component
public class DemoIncidentSeeder {
    private static final Logger log = LoggerFactory.getLogger(DemoIncidentSeeder.class);

    /** How a scenario ends. Fractions are of the incident's own response / resolution windows. */
    enum Outcome {
        CLOSED(0.4, 0.6), CLOSED_WARNED(0.5, 0.9), CLOSED_LATE_RESPONSE(1.6, 0.8), CLOSED_LATE_RESOLUTION(0.5, 1.5),
        RESOLVED(0.4, 0.6), IN_PROGRESS(0.5, 0), ACKNOWLEDGED(0.6, 0), ASSIGNED(0, 0), ESCALATED(0, 0);
        final double ack;
        final double resolve;
        Outcome(double ack, double resolve) { this.ack = ack; this.resolve = resolve; }
    }

    record Scenario(int day, int hour, int minute, String reporter, String title, String description, Outcome outcome,
                    String resolution, String rootCause, List<String> chat, Integer rating) { }

    private static final List<Scenario> SCENARIOS = List.of(
            s(2, 9, 10, "E101", "Payment gateway returns error 502", "Payment gateway returns error 502 at checkout and customers are unable to complete transactions.", Outcome.CLOSED_LATE_RESPONSE, "Restarted the payment gateway pods and raised the connection pool size", "Connection pool exhausted after a traffic spike", List.of("Customers are calling about failed payments.", "I am on it, restarting the gateway now."), 4),
            s(2, 11, 30, "E302", "Cannot log in to HR portal", "I cannot log in to the HR portal, it says my password is wrong after the reset.", Outcome.CLOSED, "Cleared the cached credentials and re-synced the directory account", "Stale password hash in the SSO cache", List.of(), 5),
            s(3, 10, 5, "E303", "Laptop battery drains in one hour", "My laptop battery drains in about an hour even when idle.", Outcome.CLOSED, "Replaced the degraded battery", "Battery health at 41 percent", List.of(), 4),
            s(3, 14, 20, "E304", "Shared drive quota full for operations", "The operations shared drive is full and the team cannot save files today.", Outcome.CLOSED_WARNED, "Archived old project folders and raised the quota by 200 GB", "Quota not reviewed since last year", List.of("Which folders can be archived?", "Anything under Projects/2024 can go."), 5),
            s(4, 9, 45, "E101", "Excel crashes when opening macro workbook", "Excel crashes on open whenever I use the monthly macro workbook.", Outcome.CLOSED, "Repaired the Office installation and re-enabled trusted macros", "Corrupted Office add-in", List.of(), 4),
            s(4, 15, 0, "E305", "Wifi very slow in sales area", "The wifi is very slow in the sales area, some users get timeouts.", Outcome.CLOSED_LATE_RESOLUTION, "Replaced the faulty access point on floor two", "Access point firmware crash loop", List.of("Is it every device or only laptops?", "Every device, phones too."), 3),
            s(5, 10, 15, "E306", "Purchase order screen error in ERP", "The ERP purchase order screen throws an error when I submit.", Outcome.CLOSED, "Fixed the missing approval workflow mapping", "Workflow table missing after the upgrade", List.of(), 5),
            s(5, 13, 40, "E307", "Suspicious phishing email received", "I received a suspicious phishing email asking for my password, several colleagues got it too.", Outcome.CLOSED, "Blocked the sender domain and purged the message from all mailboxes", "Phishing campaign from a look-alike domain", List.of(), 5),
            s(6, 9, 20, "E308", "Database reports running very slow", "The finance database reports are running very slow since this morning.", Outcome.CLOSED_WARNED, "Rebuilt fragmented indexes and updated statistics", "Index fragmentation after bulk load", List.of(), 4),
            s(6, 16, 10, "E309", "Outlook calendar not syncing", "My Outlook calendar is not syncing with my phone.", Outcome.CLOSED, "Re-created the mobile mail profile", "Expired device token", List.of(), null),
            s(9, 9, 5, "E101", "VPN disconnects every few minutes", "The VPN disconnects every few minutes, I cannot work from home today.", Outcome.CLOSED, "Updated the VPN client to the supported version", "Outdated VPN client", List.of("Which client version do you have?", "Version 4.2.1.", "Please update to 5.0, link sent."), 5),
            s(9, 11, 0, "E310", "Printer paper jam on design floor", "The printer on the design floor keeps jamming.", Outcome.CLOSED, "Cleaned the rollers and replaced the pickup tray", "Worn pickup roller", List.of(), 4),
            s(10, 10, 30, "E102", "Authentication service down for all users", "The authentication service is down for everyone, nobody can sign in, this is urgent.", Outcome.CLOSED_LATE_RESPONSE, "Failed over to the secondary identity node and renewed the certificate", "Expired signing certificate on the primary node", List.of("Whole office is locked out.", "Failover in progress, five minutes."), 3),
            s(10, 14, 45, "E301", "Payroll export failed in SAP", "The payroll export failed in SAP with a timeout, important for today.", Outcome.CLOSED, "Increased the export batch timeout and re-ran the job", "Batch timeout too low for month-end volume", List.of(), 5),
            s(11, 9, 50, "E302", "Monitor flickering at my desk", "My second monitor keeps flickering.", Outcome.CLOSED, "Replaced the display cable", "Damaged DisplayPort cable", List.of(), null),
            s(11, 15, 15, "E303", "Cloud storage sync stuck", "OneDrive sync is stuck for some users in marketing.", Outcome.CLOSED_LATE_RESOLUTION, "Reset the sync client and removed the invalid file names", "File names with unsupported characters", List.of("Any error code?", "0x8004de40"), 2),
            s(12, 10, 0, "E304", "Software licence expired for Adobe", "Adobe says my licence expired and I cannot open files.", Outcome.CLOSED, "Re-assigned a licence from the pool", "Licence not renewed for the new joiner group", List.of(), 4),
            s(12, 13, 30, "E101", "Checkout page timing out", "The checkout page is timing out for multiple users and transactions are failing.", Outcome.CLOSED_WARNED, "Rolled back the faulty release", "Release introduced a slow query", List.of("Started right after lunch.", "Thanks, that matches the deployment time."), 4),
            s(13, 9, 30, "E305", "Keyboard keys not working", "Several keys on my keyboard do not work.", Outcome.CLOSED, "Replaced the keyboard", "Liquid damage", List.of(), 5),
            s(13, 11, 45, "E306", "Database replication lag", "Database replication lag is growing on the reporting replica.", Outcome.CLOSED, "Increased replica resources and fixed the long-running transaction", "Long-running transaction blocking replication", List.of(), null),
            s(16, 10, 10, "E307", "Email bouncing for external domain", "Emails to one external customer domain are bouncing.", Outcome.CLOSED, "Corrected the SPF record", "SPF record missing the new mail relay", List.of(), 4),
            s(16, 14, 0, "E308", "ERP inventory screen error", "The ERP inventory screen shows an error for the whole warehouse team.", Outcome.CLOSED_LATE_RESPONSE, "Restored the missing role permissions", "Role permissions removed by a script", List.of(), 3),
            s(17, 9, 15, "E309", "Login loop on customer portal", "The customer portal sends me back to the login page after signing in.", Outcome.CLOSED, "Fixed the cookie domain setting", "Cookie domain changed during migration", List.of(), 5),
            s(17, 16, 30, "E310", "Docking station not detecting monitors", "My docking station does not detect the monitors.", Outcome.CLOSED, "Updated the dock firmware", "Firmware incompatibility", List.of(), null),
            s(18, 10, 20, "E101", "Invoice generation failing", "Invoice generation is failing for the billing team, blocking month-end.", Outcome.RESOLVED, "Re-generated the invoice templates", "Template corrupted after edit", List.of("Can you retry now?", "Retrying."), null),
            s(18, 14, 40, "E301", "VPN access denied after password change", "VPN access denied after my password change.", Outcome.RESOLVED, "Re-synchronised the VPN credentials", "Credential cache not refreshed", List.of(), null),
            s(19, 9, 0, "E302", "Network latency to cloud apps", "Network latency to cloud apps is high for the HR team.", Outcome.IN_PROGRESS, null, null, List.of("Is it all cloud apps?", "Mostly Teams and SharePoint."), null),
            s(19, 13, 10, "E101", "Payment refunds not processing", "Payment refunds are not processing and customers are waiting, urgent.", Outcome.IN_PROGRESS, null, null, List.of("Refund queue shows 40 pending.", "Investigating the refund service."), null),
            s(20, 10, 30, "E304", "Backup job failed overnight", "The overnight backup job failed for the file server.", Outcome.IN_PROGRESS, null, null, List.of(), null),
            s(20, 15, 0, "E305", "Teams calls dropping", "Teams calls keep dropping for some users.", Outcome.ACKNOWLEDGED, null, null, List.of(), null),
            s(21, 9, 40, "E306", "Projector not turning on in board room", "The board room projector does not turn on.", Outcome.ACKNOWLEDGED, null, null, List.of(), null),
            s(21, 11, 0, "E307", "VPN connection keeps dropping for our team", "The VPN connection keeps dropping every few minutes for our team.", Outcome.ASSIGNED, null, null, List.of(), null),
            s(21, 11, 20, "E308", "VPN connection dropping for finance team", "VPN connection keeps dropping every few minutes for the finance team.", Outcome.ASSIGNED, null, null, List.of(), null),
            s(21, 11, 45, "E309", "VPN connection keeps dropping again", "The VPN connection keeps dropping every few minutes, whole team affected.", Outcome.ASSIGNED, null, null, List.of(), null),
            s(21, 14, 0, "E102", "Payment application unavailable", "Payment application is unavailable and users are unable to complete transactions.", Outcome.ESCALATED, null, null, List.of(), null),
            s(22, 9, 30, "E101", "New starter needs software installed", "Please install the design software for a new starter in my team.", Outcome.ASSIGNED, null, null, List.of(), null));

    private static Scenario s(int day, int hour, int minute, String reporter, String title, String description, Outcome outcome,
                              String resolution, String rootCause, List<String> chat, Integer rating) {
        return new Scenario(day, hour, minute, reporter, title, description, outcome, resolution, rootCause, chat, rating);
    }

    private final IncidentRepository incidents;
    private final UserRepository users;
    private final IncidentWorkflowService workflow;
    private final ChatService chat;
    private final SlaService sla;
    private final EscalationService escalation;
    private final SwitchableClock clock;
    private final TransactionTemplate transaction;

    public DemoIncidentSeeder(IncidentRepository incidents, UserRepository users, IncidentWorkflowService workflow, ChatService chat,
                              SlaService sla, EscalationService escalation, SwitchableClock clock, PlatformTransactionManager transactions) {
        this.incidents = incidents;
        this.users = users;
        this.workflow = workflow;
        this.chat = chat;
        this.sla = sla;
        this.escalation = escalation;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactions);
    }

    public int seedIfEmpty() {
        if (incidents.count() > 0) return 0;
        int created = 0;
        try {
            for (Scenario scenario : SCENARIOS) {
                try {
                    transaction.executeWithoutResult(tx -> play(scenario));
                    created++;
                } catch (ItsmException | IllegalStateException e) {
                    log.warn("Demo scenario '{}' skipped: {}", scenario.title(), e.getMessage());
                }
            }
        } finally {
            clock.reset();
        }
        log.info("Demo seed: {} November incidents generated.", created);
        return created;
    }

    private void play(Scenario s) {
        Instant start = LocalDateTime.of(2026, 11, s.day(), s.hour(), s.minute()).toInstant(ZoneOffset.UTC);
        clock.setInstant(start);
        PortalUser reporter = users.findById(s.reporter()).orElseThrow();
        Incident incident = workflow.createIncident(reporter, new CreateIncidentRequest(s.title(), s.description(), null, null, false));
        if (incident.getTechnicianId() == null) return;
        Duration response = Duration.between(start, incident.getResponseDueAt());
        Duration resolution = Duration.between(start, incident.getResolutionDueAt());

        if (s.outcome() == Outcome.ESCALATED) {
            at(start.plus(response.multipliedBy(5)), incident);
            return;
        }
        if (s.outcome() == Outcome.ASSIGNED) return;

        Instant ack = start.plus(scale(response, s.outcome().ack));
        at(ack, incident);
        act(incident.getTechnicianId(), incident, "accept", Map.of());
        if (s.outcome() == Outcome.ACKNOWLEDGED) return;

        Instant begin = ack.plusSeconds(120);
        at(begin, incident);
        act(incident.getTechnicianId(), incident, "start", Map.of());
        Instant talk = begin;
        for (int n = 0; n < s.chat().size(); n++) {
            talk = talk.plusSeconds(240);
            clock.setInstant(talk);
            String sender = n % 2 == 0 ? s.reporter() : incident.getTechnicianId();
            chat.post(users.findById(sender).orElseThrow(), incident.getPublicId(), s.chat().get(n));
        }
        if (s.outcome() == Outcome.IN_PROGRESS) {
            clock.setInstant(talk.plusSeconds(60));
            act(incident.getTechnicianId(), incident, "note", Map.of("content", "Investigating; collected logs from the affected systems."));
            return;
        }

        Instant resolved = start.plus(scale(resolution, s.outcome().resolve));
        if (!resolved.isAfter(talk)) resolved = talk.plusSeconds(300);
        Instant warnCheck = start.plus(scale(resolution, 0.85));
        if (s.outcome() == Outcome.CLOSED_WARNED && warnCheck.isBefore(resolved)) at(warnCheck, incident);
        at(resolved, incident);
        act(incident.getTechnicianId(), incident, "resolve", Map.of("summary", s.resolution(), "rootCause", s.rootCause(),
                "steps", "Diagnosed the issue, applied the fix and confirmed with the employee."));
        Instant techApproval = resolved.plusSeconds(180);
        clock.setInstant(techApproval);
        act(incident.getTechnicianId(), incident, "approve", Map.of());
        if (s.outcome() == Outcome.RESOLVED) return;

        clock.setInstant(techApproval.plus(Duration.ofMinutes(25)));
        act(s.reporter(), incident, "approve", Map.of());
        if (s.rating() != null && incident.getStatus() == IncidentStatus.CLOSED) {
            clock.setInstant(techApproval.plus(Duration.ofMinutes(40)));
            workflow.leaveFeedback(users.findById(s.reporter()).orElseThrow(), incident.getPublicId(), s.rating(),
                    s.rating() >= 4 ? "Quick and clear communication." : "Fixed, but it took longer than expected.");
        }
    }

    /** Moves the pinned clock and lets the SLA monitor and escalation chain observe the incident at that moment. */
    private void at(Instant moment, Incident incident) {
        clock.setInstant(moment);
        sla.evaluate(incident, moment);
        escalation.evaluate(incident, moment);
    }

    private void act(String userId, Incident incident, String action, Map<String, String> body) {
        workflow.perform(users.findById(userId).orElseThrow(), incident, action, body);
    }

    private static Duration scale(Duration d, double factor) {
        return Duration.ofSeconds(Math.max(60, Math.round(d.toSeconds() * factor)));
    }
}
