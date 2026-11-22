package com.resolveit.service;

import com.resolveit.domain.incident.Feedback;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.Priority;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Technician;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.exception.ValidationException;
import com.resolveit.repository.EscalationRepository;
import com.resolveit.repository.FeedbackRepository;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.repository.TechnicianRepository;
import com.resolveit.web.dto.DailyCount;
import com.resolveit.web.dto.MonthReportDto;
import com.resolveit.web.dto.ReportDto;
import com.resolveit.web.dto.TechnicianReportDto;
import com.resolveit.web.mapper.IncidentMapper;
import com.resolveit.web.mapper.UserMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Analytics computed on demand from live data (overview, per month, per technician). */
@Service
public class ReportService {
    private final IncidentRepository incidents;
    private final FeedbackRepository feedback;
    private final EscalationRepository escalations;
    private final TechnicianRepository technicians;
    private final IncidentMapper incidentMapper;
    private final UserMapper userMapper;
    private final AssignmentService assignment;

    public ReportService(IncidentRepository incidents, FeedbackRepository feedback, EscalationRepository escalations,
                         TechnicianRepository technicians, IncidentMapper incidentMapper, UserMapper userMapper,
                         AssignmentService assignment) {
        this.incidents = incidents;
        this.feedback = feedback;
        this.escalations = escalations;
        this.technicians = technicians;
        this.incidentMapper = incidentMapper;
        this.userMapper = userMapper;
        this.assignment = assignment;
    }

    @Transactional(readOnly = true)
    public ReportDto overview(PortalUser admin) {
        AccessGuard.requireAdmin(admin);
        return build("all", incidents.findAll());
    }

    @Transactional(readOnly = true)
    public List<MonthReportDto> months(PortalUser admin) {
        AccessGuard.requireAdmin(admin);
        Map<YearMonth, List<Incident>> byMonth = new TreeMap<>(Comparator.reverseOrder());
        for (Incident i : incidents.findAll()) byMonth.computeIfAbsent(YearMonth.from(i.getCreatedAt().atZone(ZoneOffset.UTC)), k -> new ArrayList<>()).add(i);
        if (byMonth.isEmpty()) byMonth.put(YearMonth.now(ZoneOffset.UTC), List.of());
        List<MonthReportDto> result = new ArrayList<>();
        byMonth.forEach((month, list) -> result.add(new MonthReportDto(month.toString(), build(month.toString(), list))));
        return result;
    }

    @Transactional(readOnly = true)
    public ReportDto month(PortalUser admin, String month) {
        AccessGuard.requireAdmin(admin);
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw ValidationException.field("month", "Month must be written as yyyy-MM.");
        }
        Instant from = ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1000);
        return build(ym.toString(), incidents.findByCreatedAtBetweenOrderByCreatedAtAsc(from, to));
    }

    @Transactional(readOnly = true)
    public TechnicianReportDto technician(PortalUser admin, String technicianId) {
        AccessGuard.requireAdmin(admin);
        Technician tech = technicians.findById(technicianId).orElseThrow(() -> new ResourceNotFoundException("Technician not found."));
        List<Incident> mine = incidents.findByTechnicianIdOrderByCreatedAtDesc(tech.getId());
        long active = mine.stream().filter(i -> i.getStatus().countsTowardWorkload()).count();
        long resolved = mine.stream().filter(i -> i.getResolvedAt() != null).count();
        long violated = mine.stream().filter(Incident::isSlaViolated).count();
        double avgResolution = mine.stream().filter(i -> i.getResolvedAt() != null)
                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes()).average().orElse(0);
        double rating = feedback.findByIncidentIn(mine).stream().mapToInt(Feedback::getRating).average().orElse(0);
        Map<String, Long> priorities = new LinkedHashMap<>();
        for (Priority p : Priority.values()) priorities.put(p.name(), mine.stream().filter(i -> i.getPriority() == p).count());
        return new TechnicianReportDto(userMapper.toDto(tech, assignment.workload(tech.getId()), null, resolved), active, resolved,
                violated, round(avgResolution), round(rating), priorities, incidentMapper.toDtos(mine.stream().limit(10).toList()));
    }

    private ReportDto build(String period, List<Incident> list) {
        Map<String, Long> statuses = new LinkedHashMap<>();
        for (IncidentStatus s : IncidentStatus.values()) {
            long count = list.stream().filter(i -> i.getStatus() == s).count();
            if (count > 0) statuses.put(s.name(), count);
        }
        Map<String, Long> priorities = new LinkedHashMap<>();
        for (Priority p : Priority.values()) priorities.put(p.name(), list.stream().filter(i -> i.getPriority() == p).count());
        Map<String, Long> categories = counts(list, Incident::getCategory);
        Map<String, Long> services = counts(list, Incident::getServiceName);
        Map<String, Long> techs = new LinkedHashMap<>();
        list.stream().filter(i -> i.getTechnicianName() != null && i.getStatus().countsTowardWorkload())
                .forEach(i -> techs.merge(i.getTechnicianName(), 1L, Long::sum));
        long open = list.stream().filter(Incident::isOpen).count();
        long closed = list.stream().filter(i -> i.getStatus() == IncidentStatus.CLOSED).count();
        long resolved = list.stream().filter(i -> i.getResolvedAt() != null).count();
        long violated = list.stream().filter(Incident::isSlaViolated).count();
        long measurable = list.stream().filter(i -> i.getResolvedAt() != null || i.isSlaViolated()).count();
        double compliance = measurable == 0 ? 100 : 100.0 * (measurable - violated) / measurable;
        double avgResponse = list.stream().filter(i -> i.getAcknowledgedAt() != null)
                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getAcknowledgedAt()).toMinutes()).average().orElse(0);
        double avgResolution = list.stream().filter(i -> i.getResolvedAt() != null)
                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes()).average().orElse(0);
        List<Feedback> ratings = list.isEmpty() ? List.of() : feedback.findByIncidentIn(list);
        double avgRating = ratings.stream().mapToInt(Feedback::getRating).average().orElse(0);
        long escalated = list.stream().filter(i -> i.getEscalationLevel() > 0).count();
        return new ReportDto(period, list.size(), open, resolved, closed,
                list.stream().filter(i -> i.getPriority() == Priority.P1).count(), violated, round(compliance), round(avgResponse),
                round(avgResolution), round(avgRating), ratings.size(), escalated, statuses, priorities, categories, services,
                techs, trend(list));
    }

    private static Map<String, Long> counts(List<Incident> list, java.util.function.Function<Incident, String> key) {
        Map<String, Long> result = new TreeMap<>();
        list.stream().map(key).filter(Objects::nonNull).forEach(k -> result.merge(k, 1L, Long::sum));
        return result;
    }

    private static List<DailyCount> trend(List<Incident> list) {
        TreeSet<LocalDate> days = new TreeSet<>();
        Map<LocalDate, long[]> counts = new TreeMap<>();
        for (Incident i : list) {
            LocalDate created = i.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            days.add(created);
            counts.computeIfAbsent(created, d -> new long[2])[0]++;
            if (i.getClosedAt() != null) {
                LocalDate closedDay = i.getClosedAt().atZone(ZoneOffset.UTC).toLocalDate();
                counts.computeIfAbsent(closedDay, d -> new long[2])[1]++;
            }
        }
        List<DailyCount> out = new ArrayList<>();
        counts.forEach((day, c) -> out.add(new DailyCount(day.toString(), c[0], c[1])));
        return out;
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }
}
