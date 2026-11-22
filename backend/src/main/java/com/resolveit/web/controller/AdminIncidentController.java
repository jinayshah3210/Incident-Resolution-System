package com.resolveit.web.controller;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.security.CurrentUser;
import com.resolveit.service.AccessGuard;
import com.resolveit.service.AuditService;
import com.resolveit.service.IncidentQueryService;
import com.resolveit.service.IncidentWorkflowService;
import com.resolveit.service.SlaService;
import com.resolveit.web.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Administrator incident oversight: lists, edits, lifecycle override, reassignment, SLA violations and audit. */
@RestController
@RequestMapping("/api/admin")
public class AdminIncidentController {
    private final IncidentQueryService queries;
    private final IncidentWorkflowService workflow;
    private final SlaService sla;
    private final AuditService audit;

    public AdminIncidentController(IncidentQueryService queries, IncidentWorkflowService workflow, SlaService sla, AuditService audit) {
        this.queries = queries;
        this.workflow = workflow;
        this.sla = sla;
        this.audit = audit;
    }

    @GetMapping("/incidents")
    public List<IncidentDto> all(@CurrentUser PortalUser user, @RequestParam(defaultValue = "ongoing") String scope) {
        AccessGuard.requireAdmin(user);
        return queries.list(user, scope);
    }

    @RequestMapping(value = "/incidents/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public IncidentDto edit(@CurrentUser PortalUser user, @PathVariable String id, @RequestBody AdminIncidentUpdate body) {
        return workflow.adminEdit(user, id, body);
    }

    @PostMapping("/incidents/{id}/assign")
    public IncidentDto assign(@CurrentUser PortalUser user, @PathVariable String id, @Valid @RequestBody AssignRequest body) {
        return workflow.reassign(user, id, body.technicianId());
    }

    @PostMapping("/incidents/{id}/unassign")
    public IncidentDto unassign(@CurrentUser PortalUser user, @PathVariable String id) { return workflow.unassign(user, id); }

    @PostMapping("/incidents/{id}/advance")
    public IncidentDto advance(@CurrentUser PortalUser user, @PathVariable String id, @Valid @RequestBody AdvanceRequest body) {
        return workflow.adminAdvance(user, id, body.status());
    }

    @GetMapping("/incidents/{id}/recommendations")
    public List<RecommendationDto> recommendations(@CurrentUser PortalUser user, @PathVariable String id) {
        return workflow.recommendations(user, id);
    }

    @GetMapping("/sla-violations")
    public List<Map<String, Object>> violations(@CurrentUser PortalUser user) { return sla.openViolations(user); }

    @PostMapping("/sla-violations/{id}/dismiss")
    public Map<String, Object> dismiss(@CurrentUser PortalUser user, @PathVariable String id) { return sla.dismiss(user, id); }

    @GetMapping("/audit")
    public List<AuditEventDto> audit(@CurrentUser PortalUser user, @RequestParam(defaultValue = "0") int offset,
                                     @RequestParam(defaultValue = "100") int limit, @RequestParam(required = false) String incident,
                                     @RequestParam(required = false) String actor, @RequestParam(required = false) String action) {
        return audit.page(user, offset, limit, incident, actor, action);
    }

    @GetMapping("/audit/incidents/{incidentId}")
    public List<AuditEventDto> incidentAudit(@CurrentUser PortalUser user, @PathVariable String incidentId) {
        return audit.forIncidentAdmin(user, incidentId);
    }
}
