package com.resolveit.web.controller;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.security.CurrentUser;
import com.resolveit.domain.user.Role;
import com.resolveit.domain.user.Technician;
import com.resolveit.service.AccessGuard;
import com.resolveit.service.AssignmentService;
import com.resolveit.service.IncidentQueryService;
import com.resolveit.service.IncidentWorkflowService;
import com.resolveit.web.dto.IncidentDto;
import com.resolveit.web.dto.WorkloadDto;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Technician queue: offers, own incidents, workload and offer accept/reject. */
@RestController
@RequestMapping("/api")
public class TechnicianController {
    private final IncidentQueryService queries;
    private final IncidentWorkflowService workflow;
    private final AssignmentService assignment;

    public TechnicianController(IncidentQueryService queries, IncidentWorkflowService workflow, AssignmentService assignment) {
        this.queries = queries;
        this.workflow = workflow;
        this.assignment = assignment;
    }

    @GetMapping("/technician/offers")
    public List<IncidentDto> offers(@CurrentUser PortalUser user) { return queries.offers(user); }

    @GetMapping("/technician/incidents")
    public List<IncidentDto> incidents(@CurrentUser PortalUser user, @RequestParam(defaultValue = "ongoing") String scope) {
        AccessGuard.requireRole(user, Role.TECHNICIAN);
        return queries.list(user, scope);
    }

    @GetMapping("/technician/workload")
    public WorkloadDto workload(@CurrentUser PortalUser user) {
        AccessGuard.requireRole(user, Role.TECHNICIAN);
        return new WorkloadDto(assignment.workload(user.getId()), ((Technician) user).getMaxWorkload());
    }

    @PostMapping("/offers/{id}/{action}")
    public IncidentDto offer(@CurrentUser PortalUser user, @PathVariable String id, @PathVariable String action,
                             @RequestBody(required = false) Map<String, String> body) {
        String mapped = "accept".equals(action) || "reject".equals(action) ? action : "unknown";
        return workflow.action(user, id, mapped, body);
    }
}
