package com.resolveit.service;

import com.resolveit.domain.catalog.Category;
import com.resolveit.domain.catalog.ItService;
import com.resolveit.domain.catalog.ServiceTechnician;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Technician;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.exception.ServiceNotFoundException;
import com.resolveit.exception.ValidationException;
import com.resolveit.repository.CategoryRepository;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.repository.ItServiceRepository;
import com.resolveit.repository.ServiceTechnicianRepository;
import com.resolveit.repository.TechnicianRepository;
import com.resolveit.util.TextNormalizer;
import com.resolveit.web.dto.CategoryDto;
import com.resolveit.web.dto.KeywordsRequest;
import com.resolveit.web.dto.CategoryRequest;
import com.resolveit.web.dto.ServiceDto;
import com.resolveit.web.dto.ServiceRequest;
import com.resolveit.web.dto.UserDto;
import com.resolveit.web.mapper.UserMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IT service catalog, technician-service associations, and dynamic categories with classifier keywords. */
@Service
public class CatalogService {
    public static final String GENERAL_IT = "General IT";
    private final ItServiceRepository services;
    private final ServiceTechnicianRepository links;
    private final CategoryRepository categories;
    private final TechnicianRepository technicians;
    private final IncidentRepository incidents;
    private final AuditService audit;
    private final UserMapper userMapper;

    public CatalogService(ItServiceRepository services, ServiceTechnicianRepository links, CategoryRepository categories,
                          TechnicianRepository technicians, IncidentRepository incidents, AuditService audit, UserMapper userMapper) {
        this.services = services;
        this.links = links;
        this.categories = categories;
        this.technicians = technicians;
        this.incidents = incidents;
        this.audit = audit;
        this.userMapper = userMapper;
    }

    // ------------------------------------------------------------------ lookups used by the workflow

    /** Employee's choice, else best keyword match in the text, else General IT. Never "not found" for auto-detection. */
    public ItService resolveService(String requested, String text) {
        if (requested != null && !requested.isBlank()) {
            return services.findByNameIgnoreCase(requested.strip()).filter(ItService::isActive)
                    .orElseThrow(ServiceNotFoundException::new);
        }
        String normalized = TextNormalizer.normalize(text);
        ItService best = null;
        int bestScore = 0;
        for (ItService service : services.findByActiveTrueOrderByNameAsc()) {
            int score = service.keywordList().stream().mapToInt(k -> TextNormalizer.countPhrase(normalized, k)).sum();
            if (score > bestScore || (score == bestScore && score > 0 && best != null && service.getCriticality() > best.getCriticality())) {
                best = service;
                bestScore = score;
            }
        }
        if (best != null) return best;
        return services.findByNameIgnoreCase(GENERAL_IT).filter(ItService::isActive)
                .or(() -> services.findByActiveTrueOrderByNameAsc().stream().findFirst())
                .orElseThrow(ServiceNotFoundException::new);
    }

    public boolean canServe(Technician technician, String serviceName) {
        return services.findByNameIgnoreCase(serviceName == null ? "" : serviceName).filter(ItService::isActive)
                .map(s -> links.existsByService_IdAndTechnicianId(s.getId(), technician.getId())).orElse(false);
    }

    public List<Category> activeCategories() { return categories.findByActiveTrueOrderByNameAsc(); }

    // ------------------------------------------------------------------ public catalog

    @Transactional(readOnly = true)
    public List<ServiceDto> catalog() {
        return services.findByActiveTrueOrderByNameAsc().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public List<String> categoryNames() {
        return categories.findByActiveTrueOrderByNameAsc().stream().map(Category::getName).toList();
    }

    // ------------------------------------------------------------------ admin: services

    @Transactional(readOnly = true)
    public List<ServiceDto> adminServices(PortalUser actor) {
        AccessGuard.requireAdmin(actor);
        return services.findAllByOrderByNameAsc().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public ServiceDto serviceDetail(PortalUser actor, long id) {
        AccessGuard.requireAdmin(actor);
        ItService service = byId(id);
        List<UserDto> techs = links.findByService_Id(id).stream()
                .map(link -> technicians.findById(link.getTechnicianId()).orElse(null)).filter(Objects::nonNull)
                .sorted(Comparator.comparing(Technician::getId))
                .map(t -> userMapper.toDto(t, incidents.countByTechnicianIdAndStatusIn(t.getId(), IncidentStatus.WORKLOAD), null, null))
                .toList();
        ServiceDto base = dto(service);
        return new ServiceDto(base.id(), base.name(), base.description(), base.category(), base.criticality(), base.keywords(),
                base.active(), base.technicianCount(), base.incidentCount(), techs);
    }

    @Transactional
    public ServiceDto createService(PortalUser actor, ServiceRequest request) {
        AccessGuard.requireAdmin(actor);
        String name = serviceName(request);
        if (services.findByNameIgnoreCase(name).isPresent()) throw new ConflictException("A service with this name already exists.");
        ItService saved = services.save(new ItService(name, description(request), category(request), criticality(request), request.keywords()));
        audit.recordSystem(actor.getId(), "SERVICE_CREATED", "Service " + name + " created.");
        return dto(saved);
    }

    @Transactional
    public ServiceDto updateService(PortalUser actor, long id, ServiceRequest request) {
        AccessGuard.requireAdmin(actor);
        ItService service = byId(id);
        String previous = service.getName();
        String next = serviceName(request);
        if (services.findByNameIgnoreCase(next).filter(other -> !other.getId().equals(id)).isPresent()) {
            throw new ConflictException("A service with this name already exists.");
        }
        if (!previous.equalsIgnoreCase(next) && incidents.countByServiceNameIgnoreCase(previous) > 0) {
            throw new ConflictException("A service referenced by incidents cannot be renamed; you may edit its details or archive it.");
        }
        service.update(next, description(request), category(request), criticality(request),
                request.keywords() == null ? service.getKeywords() : request.keywords());
        if (Boolean.TRUE.equals(request.active())) service.reactivate();
        if (Boolean.FALSE.equals(request.active())) service.archive();
        audit.recordSystem(actor.getId(), "SERVICE_UPDATED", "Service " + previous + " updated to " + next + "; active=" + service.isActive() + ".");
        return dto(service);
    }

    @Transactional
    public Map<String, Object> removeService(PortalUser actor, long id) {
        AccessGuard.requireAdmin(actor);
        ItService service = byId(id);
        long references = incidents.countByServiceNameIgnoreCase(service.getName());
        if (references > 0) {
            service.archive();
            audit.recordSystem(actor.getId(), "SERVICE_ARCHIVED", "Service " + service.getName() + " archived to preserve " + references + " incident(s).");
            return Map.of("archived", true, "id", id);
        }
        links.deleteByService_Id(id);
        services.delete(service);
        audit.recordSystem(actor.getId(), "SERVICE_DELETED", "Unused service " + service.getName() + " deleted.");
        return Map.of("archived", false, "id", id);
    }

    @Transactional
    public ServiceDto addServiceTechnician(PortalUser actor, long id, String technicianId) {
        AccessGuard.requireAdmin(actor);
        ItService service = byId(id);
        if (!service.isActive()) throw new ConflictException("Reactivate the service before adding technicians.");
        Technician tech = technicians.findById(technicianId == null ? "" : technicianId.strip().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found."));
        if (!tech.isActive()) throw new ValidationException("Only active technicians can be assigned to services.");
        if (links.existsByService_IdAndTechnicianId(id, tech.getId())) throw new ConflictException("Technician is already assigned to this service.");
        links.save(new ServiceTechnician(service, tech.getId()));
        audit.recordSystem(actor.getId(), "SERVICE_TECHNICIAN_ADDED", "Technician " + tech.getId() + " linked to service " + service.getName() + ".");
        return serviceDetail(actor, id);
    }

    @Transactional
    public ServiceDto removeServiceTechnician(PortalUser actor, long id, String technicianId) {
        AccessGuard.requireAdmin(actor);
        ItService service = byId(id);
        if (!links.existsByService_IdAndTechnicianId(id, technicianId)) throw new ResourceNotFoundException("Technician is not linked to this service.");
        links.deleteByService_IdAndTechnicianId(id, technicianId);
        audit.recordSystem(actor.getId(), "SERVICE_TECHNICIAN_REMOVED", "Technician " + technicianId + " unlinked from service " + service.getName() + " (account retained).");
        return serviceDetail(actor, id);
    }

    // ------------------------------------------------------------------ admin: categories

    @Transactional(readOnly = true)
    public List<CategoryDto> adminCategories(PortalUser actor) {
        AccessGuard.requireAdmin(actor);
        return categories.findAllByOrderByNameAsc().stream().map(this::dto).toList();
    }

    @Transactional
    public CategoryDto createCategory(PortalUser actor, CategoryRequest request) {
        AccessGuard.requireAdmin(actor);
        String name = request.name() == null ? "" : request.name().strip();
        if (categories.findByNameIgnoreCase(name).isPresent()) throw new ConflictException("A category with this name already exists.");
        Category saved = categories.save(new Category(name, request.description()));
        audit.recordSystem(actor.getId(), "CATEGORY_CREATED", "Category " + saved.getName() + " created.");
        return dto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(PortalUser actor, long id, CategoryRequest request) {
        AccessGuard.requireAdmin(actor);
        Category category = categoryById(id);
        if (request.name() != null && !request.name().equalsIgnoreCase(category.getName())) {
            if (incidents.countByCategoryIgnoreCaseAndStatusNot(category.getName(), IncidentStatus.CLOSED) > 0) {
                throw new ConflictException("A category with open incidents cannot be renamed.");
            }
            category.rename(request.name());
        }
        if (request.description() != null) category.describe(request.description());
        if (Boolean.FALSE.equals(request.active())) category.deactivate();
        if (Boolean.TRUE.equals(request.active())) category.activate();
        audit.recordSystem(actor.getId(), "CATEGORY_UPDATED", "Category " + category.getName() + " updated; active=" + category.isActive() + ".");
        return dto(category);
    }

    /** Categories are never hard-deleted (incidents keep their category name); delete deactivates. */
    @Transactional
    public CategoryDto deactivateCategory(PortalUser actor, long id) {
        AccessGuard.requireAdmin(actor);
        Category category = categoryById(id);
        category.deactivate();
        audit.recordSystem(actor.getId(), "CATEGORY_DEACTIVATED", "Category " + category.getName() + " deactivated.");
        return dto(category);
    }

    @Transactional
    public CategoryDto replaceKeywords(PortalUser actor, long id, KeywordsRequest request) {
        AccessGuard.requireAdmin(actor);
        if (request.keywords() == null) throw new ValidationException("Keywords are required.");
        Category category = categoryById(id);
        category.replaceKeywords(request.keywords());
        audit.recordSystem(actor.getId(), "CATEGORY_KEYWORDS_CHANGED", "Category " + category.getName() + " now has " + category.getKeywords().size() + " keywords.");
        return dto(category);
    }

    // ------------------------------------------------------------------ helpers

    private ServiceDto dto(ItService s) {
        long activeTechs = links.findByService_Id(s.getId()).stream()
                .filter(l -> technicians.findById(l.getTechnicianId()).map(Technician::isActive).orElse(false)).count();
        return new ServiceDto(s.getId(), s.getName(), s.getDescription(), s.getCategory(), s.getCriticality(), s.getKeywords(),
                s.isActive(), activeTechs, incidents.countByServiceNameIgnoreCase(s.getName()), null);
    }

    private CategoryDto dto(Category c) {
        long techCount = technicians.findByActiveTrueOrderByIdAsc().stream().filter(t -> t.canHandle(c.getName())).count();
        return new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.isActive(), c.keywordWeights(), techCount,
                incidents.countByCategoryIgnoreCaseAndStatusNot(c.getName(), IncidentStatus.CLOSED));
    }

    private ItService byId(long id) { return services.findById(id).orElseThrow(() -> new ServiceNotFoundException("IT service not found.")); }

    private Category categoryById(long id) { return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found.")); }

    private String serviceName(ServiceRequest r) {
        String name = r.name() == null ? "" : r.name().strip();
        if (name.length() < 3 || name.length() > 120) throw ValidationException.field("name", "Service name must have 3-120 characters.");
        return name;
    }

    private String description(ServiceRequest r) {
        String d = r.description() == null ? "" : r.description().strip();
        if (d.length() < 8 || d.length() > 600) throw ValidationException.field("description", "Description must have 8-600 characters.");
        return d;
    }

    private String category(ServiceRequest r) {
        String c = r.category() == null ? "" : r.category().strip();
        return categories.findByNameIgnoreCase(c).map(Category::getName)
                .orElseThrow(() -> ValidationException.field("category", "Choose a valid IT service category."));
    }

    private int criticality(ServiceRequest r) {
        int value = r.criticality() == null ? 0 : r.criticality();
        if (value < 1 || value > 4) throw ValidationException.field("criticality", "Criticality must be between 1 and 4.");
        return value;
    }

    public Optional<ItService> findService(String name) { return services.findByNameIgnoreCase(name); }

    public Set<String> validCategoryNames() {
        return Set.copyOf(categories.findByActiveTrueOrderByNameAsc().stream().map(Category::getName).toList());
    }
}
