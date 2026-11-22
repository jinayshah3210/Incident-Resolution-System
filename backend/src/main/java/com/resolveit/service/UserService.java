package com.resolveit.service;

import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Role;
import com.resolveit.domain.user.Technician;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.exception.ValidationException;
import com.resolveit.factory.NewUserSpec;
import com.resolveit.factory.UserFactory;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.repository.UserRepository;
import com.resolveit.web.dto.ManagedUserRequest;
import com.resolveit.web.dto.UserDto;
import com.resolveit.web.mapper.UserMapper;
import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account directory and administration (soft delete keeps history). */
@Service
public class UserService {
    private final UserRepository users;
    private final IncidentRepository incidents;
    private final UserFactory factory;
    private final UserMapper mapper;
    private final AuditService audit;
    private final AssignmentService assignment;
    private final CatalogService catalog;
    private final Clock clock;

    public UserService(UserRepository users, IncidentRepository incidents, UserFactory factory, UserMapper mapper,
                       AuditService audit, AssignmentService assignment, CatalogService catalog, Clock clock) {
        this.users = users;
        this.incidents = incidents;
        this.factory = factory;
        this.mapper = mapper;
        this.audit = audit;
        this.assignment = assignment;
        this.catalog = catalog;
        this.clock = clock;
    }

    public static boolean validPassword(String password) {
        return password != null && password.length() >= 8 && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*") && password.matches(".*\\d.*");
    }

    public UserDto view(PortalUser user) {
        Map<String, Long> loads = assignment.workloads();
        return switchView(user, loads);
    }

    private UserDto switchView(PortalUser user, Map<String, Long> loads) {
        if (user.hasRole(Role.TECHNICIAN)) {
            long resolved = incidents.findByTechnicianIdOrderByCreatedAtDesc(user.getId()).stream()
                    .filter(i -> i.getStatus() == IncidentStatus.CLOSED).count();
            return mapper.toDto(user, loads.getOrDefault(user.getId(), 0L), null, resolved);
        }
        if (user.hasRole(Role.EMPLOYEE)) {
            return mapper.toDto(user, null, incidents.countByReporterId(user.getId()),
                    incidents.countByReporterIdAndStatus(user.getId(), IncidentStatus.CLOSED));
        }
        return mapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> directory(PortalUser actor, String role) {
        AccessGuard.requireAdmin(actor);
        Role filter = role == null || role.isBlank() ? null : Role.parse(role);
        Map<String, Long> loads = assignment.workloads();
        return users.findAllByOrderByIdAsc().stream().filter(u -> filter == null || u.hasRole(filter))
                .sorted(Comparator.comparing(PortalUser::getId)).map(u -> switchView(u, loads)).toList();
    }

    @Transactional
    public UserDto create(PortalUser actor, ManagedUserRequest request) {
        AccessGuard.requireAdmin(actor);
        Role role = Role.parse(request.role());
        if (role == Role.ADMIN) throw new ValidationException("Only employees and technicians can be created here.");
        String name = request.name() == null ? "" : request.name().strip();
        String email = request.email() == null ? "" : request.email().strip().toLowerCase();
        if (name.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || !validPassword(request.password())) {
            throw new ValidationException("Enter a name, valid email, and password with 8+ characters including uppercase, lowercase, and a digit.");
        }
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("An account with this email already exists.");
        String id = nextId(role);
        NewUserSpec spec;
        if (role == Role.TECHNICIAN) {
            Set<String> skills = skills(request.skills());
            int max = request.maxWorkload() == null ? 8 : request.maxWorkload();
            if (max < 1 || max > 50) throw ValidationException.field("maxWorkload", "Maximum workload must be between 1 and 50.");
            int experience = request.experienceYears() == null ? 1 : Math.max(0, request.experienceYears());
            spec = NewUserSpec.technician(id, name, email, request.password(), request.department(), skills, max, experience,
                    Boolean.TRUE.equals(request.teamLead()));
        } else {
            spec = NewUserSpec.employee(id, name, email, request.password(), request.department());
        }
        PortalUser created = users.save(factory.create(role, spec));
        audit.recordSystem(actor.getId(), "ACCOUNT_CREATED", role + " " + id + " (" + name + ") created.");
        return view(created);
    }

    @Transactional
    public UserDto update(PortalUser actor, String id, ManagedUserRequest request) {
        AccessGuard.requireAdmin(actor);
        PortalUser target = find(id);
        target.updateContact(request.name(), request.department(), request.phone());
        if (target instanceof Technician technician && (request.skills() != null || request.maxWorkload() != null || request.experienceYears() != null)) {
            Set<String> skills = request.skills() == null ? technician.getSkills() : skills(request.skills());
            int max = request.maxWorkload() == null ? technician.getMaxWorkload() : request.maxWorkload();
            if (max < 1 || max > 50) throw ValidationException.field("maxWorkload", "Maximum workload must be between 1 and 50.");
            technician.updateProfile(skills, max, request.experienceYears() == null ? technician.getExperienceYears() : request.experienceYears());
        }
        audit.recordSystem(actor.getId(), "ACCOUNT_UPDATED", target.getRole() + " " + target.getId() + " updated.");
        return view(target);
    }

    @Transactional
    public void deactivate(PortalUser actor, String id) {
        AccessGuard.requireAdmin(actor);
        PortalUser target = find(id);
        if (target.hasRole(Role.ADMIN)) throw new ValidationException("The administrator account cannot be deactivated here.");
        if (!target.isActive()) throw new ConflictException("This account is already inactive.");
        target.deactivate();
        audit.recordSystem(actor.getId(), "ACCOUNT_DEACTIVATED", target.getRole() + " " + target.getId() + " (" + target.getName() + ") deactivated.");
        if (target instanceof Technician technician) assignment.releaseAllFor(actor, technician, clock.instant());
    }

    @Transactional
    public UserDto reactivate(PortalUser actor, String id) {
        AccessGuard.requireAdmin(actor);
        PortalUser target = find(id);
        if (target.hasRole(Role.ADMIN)) throw new ValidationException("Administrator account status cannot be changed here.");
        if (target.isActive()) throw new ConflictException("This account is already active.");
        target.activate();
        audit.recordSystem(actor.getId(), "ACCOUNT_REACTIVATED", target.getRole() + " " + target.getId() + " (" + target.getName() + ") reactivated; old assignments not restored.");
        return view(target);
    }

    public PortalUser find(String id) {
        return users.findByIdIgnoreCase(id == null ? "" : id.strip()).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
    }

    /** Next public id per role prefix: E101.., T201.., A001.. */
    public String nextId(Role role) {
        String prefix = switchPrefix(role);
        long max = users.findIdsStartingWith(prefix).stream().map(x -> x.substring(1)).mapToLong(x -> {
            try {
                return Long.parseLong(x);
            } catch (NumberFormatException e) {
                return 0;
            }
        }).max().orElse(role == Role.EMPLOYEE ? 100 : role == Role.TECHNICIAN ? 200 : 0);
        return role == Role.ADMIN ? String.format("A%03d", max + 1) : prefix + (max + 1);
    }

    private static String switchPrefix(Role role) { return role.name().substring(0, 1); }

    private Set<String> skills(List<String> raw) {
        Set<String> valid = catalog.validCategoryNames();
        Set<String> skills = new LinkedHashSet<>();
        if (raw != null) raw.stream().map(String::strip).filter(s -> !s.isEmpty()).forEach(skills::add);
        if (skills.isEmpty() || skills.stream().anyMatch(s -> valid.stream().noneMatch(v -> v.equalsIgnoreCase(s)))) {
            throw ValidationException.field("skills", "Choose one or more valid technical skills.");
        }
        return skills;
    }
}
