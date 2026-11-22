package com.resolveit.service;

import com.resolveit.domain.incident.ChatMessage;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Role;
import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentEventPublisher;
import com.resolveit.event.IncidentEventType;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.web.dto.ChatMessageDto;
import com.resolveit.web.dto.ContactDto;
import com.resolveit.web.mapper.IncidentMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Incident-scoped conversation between the reporter, the assigned technician and administrators. */
@Service
public class ChatService {
    private final IncidentWorkflowService workflow;
    private final IncidentRepository incidents;
    private final IncidentEventPublisher events;
    private final IncidentMapper mapper;
    private final Clock clock;

    public ChatService(IncidentWorkflowService workflow, IncidentRepository incidents, IncidentEventPublisher events,
                       IncidentMapper mapper, Clock clock) {
        this.workflow = workflow;
        this.incidents = incidents;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> thread(PortalUser user, String incidentId) {
        Incident incident = workflow.find(incidentId);
        AccessGuard.requireView(user, incident);
        return incident.getChatMessages().stream().map(mapper::chat).toList();
    }

    @Transactional
    public List<ChatMessageDto> post(PortalUser user, String incidentId, String content) {
        Incident incident = workflow.find(incidentId);
        AccessGuard.requireView(user, incident);
        Instant at = clock.instant();
        ChatMessage message = incident.addChatMessage(user, content, at);
        String recipient = user.getId().equals(incident.getReporterId()) ? incident.getTechnicianId() : incident.getReporterId();
        events.publish(IncidentEvent.of(IncidentEventType.CHAT_MESSAGE_SENT, incident, user.getId(),
                "A message was added to the incident conversation.", null, at)
                .with("recipient", recipient).with("senderName", message.getSenderName()));
        return incident.getChatMessages().stream().map(mapper::chat).toList();
    }

    /** People the user currently shares an open incident conversation with. */
    @Transactional(readOnly = true)
    public List<ContactDto> contacts(PortalUser user) {
        Map<String, ContactDto> contacts = new LinkedHashMap<>();
        List<Incident> source = user.hasRole(Role.EMPLOYEE) ? incidents.findByReporterIdOrderByCreatedAtDesc(user.getId())
                : user.hasRole(Role.TECHNICIAN) ? incidents.findByTechnicianIdOrderByCreatedAtDesc(user.getId())
                : incidents.findByStatusNot(IncidentStatus.CLOSED);
        for (Incident i : source) {
            if (!i.isOpen()) continue;
            if (!user.getId().equals(i.getReporterId())) {
                contacts.putIfAbsent(i.getReporterId(), new ContactDto(i.getReporterId(), i.getReporterName(), "EMPLOYEE", i.getPublicId()));
            }
            if (i.getTechnicianId() != null && !user.getId().equals(i.getTechnicianId())) {
                contacts.putIfAbsent(i.getTechnicianId(), new ContactDto(i.getTechnicianId(), i.getTechnicianName(), "TECHNICIAN", i.getPublicId()));
            }
        }
        return List.copyOf(contacts.values());
    }
}
