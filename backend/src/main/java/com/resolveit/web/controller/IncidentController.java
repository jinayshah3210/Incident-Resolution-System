package com.resolveit.web.controller;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.security.CurrentUser;
import com.resolveit.service.AuditService;
import com.resolveit.service.ChatService;
import com.resolveit.service.IncidentQueryService;
import com.resolveit.service.IncidentWorkflowService;
import com.resolveit.web.dto.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Employee and shared incident endpoints. Thin: every rule lives in the services. */
@RestController
@RequestMapping("/api")
public class IncidentController {
    private final IncidentWorkflowService workflow;
    private final IncidentQueryService queries;
    private final ChatService chat;
    private final AuditService audit;

    public IncidentController(IncidentWorkflowService workflow, IncidentQueryService queries, ChatService chat, AuditService audit) {
        this.workflow = workflow;
        this.queries = queries;
        this.chat = chat;
        this.audit = audit;
    }

    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.OK)
    public IncidentDto create(@CurrentUser PortalUser user, @Valid @RequestBody CreateIncidentRequest body) {
        return workflow.create(user, body);
    }

    @GetMapping("/incidents/mine")
    public List<IncidentDto> mine(@CurrentUser PortalUser user, @RequestParam(defaultValue = "ongoing") String scope) {
        return queries.list(user, scope);
    }

    @GetMapping("/incidents/search")
    public List<IncidentDto> search(@CurrentUser PortalUser user, @RequestParam(required = false) String q,
                                    @RequestParam(required = false) String status, @RequestParam(required = false) String priority,
                                    @RequestParam(required = false) String category, @RequestParam(required = false) String service,
                                    @RequestParam(required = false) String technician, @RequestParam(required = false) String slaStatus,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                    @RequestParam(required = false) String scope) {
        return queries.search(user, new IncidentSearchCriteria(q, status, priority, category, service, technician, slaStatus, from, to, scope));
    }

    @GetMapping("/incidents/{id}")
    public IncidentDetailDto detail(@CurrentUser PortalUser user, @PathVariable String id) { return queries.detail(user, id); }

    @GetMapping("/incidents/{id}/audit")
    public List<AuditEventDto> incidentAudit(@CurrentUser PortalUser user, @PathVariable String id) {
        return queries.detail(user, id).audit();
    }

    @GetMapping("/incidents/{id}/chat")
    public List<ChatMessageDto> thread(@CurrentUser PortalUser user, @PathVariable String id) { return chat.thread(user, id); }

    @PostMapping("/incidents/{id}/chat")
    public List<ChatMessageDto> post(@CurrentUser PortalUser user, @PathVariable String id, @Valid @RequestBody ChatRequest body) {
        return chat.post(user, id, body.content());
    }

    @PostMapping("/incidents/{id}/feedback")
    public IncidentDto feedback(@CurrentUser PortalUser user, @PathVariable String id, @Valid @RequestBody FeedbackRequest body) {
        return workflow.leaveFeedback(user, id, body.rating(), body.comment());
    }

    @GetMapping("/incidents/{id}/suggestions")
    public List<SuggestionDto> suggestions(@CurrentUser PortalUser user, @PathVariable String id) { return workflow.suggestions(user, id); }

    @PostMapping("/incidents/{id}/closure/approve")
    public IncidentDto approve(@CurrentUser PortalUser user, @PathVariable String id) { return workflow.action(user, id, "approve", Map.of()); }

    @PostMapping("/incidents/{id}/closure/reject")
    public IncidentDto rejectClosure(@CurrentUser PortalUser user, @PathVariable String id,
                                     @RequestBody(required = false) Map<String, String> body) {
        return workflow.action(user, id, "reject-closure", body);
    }

    /** accept, reject, start, note, info, request-info, resolve, approve, reject-closure. */
    @PostMapping("/incidents/{id}/{action}")
    public IncidentDto action(@CurrentUser PortalUser user, @PathVariable String id, @PathVariable String action,
                              @RequestBody(required = false) Map<String, String> body) {
        return workflow.action(user, id, action, body);
    }

    @GetMapping("/chat/contacts")
    public List<ContactDto> contacts(@CurrentUser PortalUser user) { return chat.contacts(user); }
}
