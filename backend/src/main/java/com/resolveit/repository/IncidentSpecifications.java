package com.resolveit.repository;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.Priority;
import com.resolveit.domain.sla.SlaStatus;
import java.time.Instant;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Composable JPA Specifications behind GET /api/incidents/search. Each method is one filter criterion. */
public final class IncidentSpecifications {
    private IncidentSpecifications() { }

    public static Specification<Incident> all() { return (root, query, cb) -> cb.conjunction(); }

    public static Specification<Incident> reportedBy(String reporterId) {
        return (root, query, cb) -> cb.equal(root.get("reporterId"), reporterId);
    }

    public static Specification<Incident> assignedTo(String technicianId) {
        return (root, query, cb) -> cb.equal(root.get("technicianId"), technicianId);
    }

    /** Free text over incident id, title and description (case-insensitive, partial match). */
    public static Specification<Incident> text(String q) {
        String like = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("publicId")), like),
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like));
    }

    public static Specification<Incident> status(IncidentStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> priority(Priority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Incident> category(String category) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.strip().toLowerCase(Locale.ROOT));
    }

    public static Specification<Incident> service(String service) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("serviceName")), service.strip().toLowerCase(Locale.ROOT));
    }

    /** Matches technician id exactly or technician name partially. */
    public static Specification<Incident> technician(String technician) {
        String value = technician.strip().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.or(
                cb.equal(cb.lower(root.get("technicianId")), value),
                cb.like(cb.lower(root.get("technicianName")), "%" + value + "%"));
    }

    public static Specification<Incident> slaStatus(SlaStatus slaStatus) {
        return (root, query, cb) -> cb.equal(root.get("slaStatus"), slaStatus);
    }

    public static Specification<Incident> createdFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Incident> createdTo(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<Incident> open(boolean open) {
        return (root, query, cb) -> open ? cb.notEqual(root.get("status"), IncidentStatus.CLOSED)
                : cb.equal(root.get("status"), IncidentStatus.CLOSED);
    }
}
