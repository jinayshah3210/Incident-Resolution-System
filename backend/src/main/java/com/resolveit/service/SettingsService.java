package com.resolveit.service;

import com.resolveit.domain.incident.Priority;
import com.resolveit.domain.settings.SystemSetting;
import com.resolveit.domain.sla.SlaTargets;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.exception.ValidationException;
import com.resolveit.repository.SystemSettingRepository;
import com.resolveit.strategy.StrategyRegistry;
import com.resolveit.strategy.assignment.AssignmentStrategy;
import com.resolveit.strategy.classify.CategoryClassifier;
import com.resolveit.strategy.priority.PriorityStrategy;
import com.resolveit.web.dto.SupportContactDto;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and validates the key/value system configuration stored in the settings table. */
@Service
public class SettingsService {
    public static final String PRIORITY_STRATEGY = "priority.strategy";
    public static final String ASSIGNMENT_STRATEGY = "assignment.strategy";
    public static final String CLASSIFIER_STRATEGY = "classifier.strategy";
    public static final String WARNING_PERCENT = "sla.warning.percent";
    public static final String ESCALATION_ENABLED = "escalation.enabled";

    private final SystemSettingRepository repository;
    private final StrategyRegistry<PriorityStrategy> priorityStrategies;
    private final StrategyRegistry<AssignmentStrategy> assignmentStrategies;
    private final StrategyRegistry<CategoryClassifier> classifiers;
    private final AuditService audit;
    private final String supportEmail;
    private final String supportPhone;

    public SettingsService(SystemSettingRepository repository, StrategyRegistry<PriorityStrategy> priorityStrategies,
                           StrategyRegistry<AssignmentStrategy> assignmentStrategies,
                           StrategyRegistry<CategoryClassifier> classifiers, AuditService audit,
                           @Value("${resolveit.support.email:support@resolveit.test}") String supportEmail,
                           @Value("${resolveit.support.phone:}") String supportPhone) {
        this.repository = repository;
        this.priorityStrategies = priorityStrategies;
        this.assignmentStrategies = assignmentStrategies;
        this.classifiers = classifiers;
        this.audit = audit;
        this.supportEmail = supportEmail;
        this.supportPhone = supportPhone;
    }

    public String get(String key, String fallback) {
        return repository.findById(key).map(SystemSetting::getValue).orElse(fallback);
    }

    public String priorityStrategy() { return get(PRIORITY_STRATEGY, "MULTIPLICATIVE"); }
    public String assignmentStrategy() { return get(ASSIGNMENT_STRATEGY, "LEAST_LOAD"); }
    public String classifierStrategy() { return get(CLASSIFIER_STRATEGY, "KEYWORD"); }
    public boolean escalationEnabled() { return Boolean.parseBoolean(get(ESCALATION_ENABLED, "true")); }

    public int warningPercent() {
        try {
            return Integer.parseInt(get(WARNING_PERCENT, "80"));
        } catch (NumberFormatException e) {
            return 80;
        }
    }

    /** The configured SLA policy for a priority, empty when the policy rows are missing or unreadable. */
    public Optional<SlaTargets> slaTargets(Priority priority) {
        Optional<String> response = repository.findById(responseKey(priority)).map(SystemSetting::getValue);
        Optional<String> resolution = repository.findById(resolutionKey(priority)).map(SystemSetting::getValue);
        if (response.isEmpty() || resolution.isEmpty()) return Optional.empty();
        try {
            return Optional.of(new SlaTargets(Integer.parseInt(response.get()), Integer.parseInt(resolution.get()), false));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static String responseKey(Priority p) { return "sla." + p.name() + ".response.minutes"; }
    public static String resolutionKey(Priority p) { return "sla." + p.name() + ".resolution.minutes"; }

    @Transactional(readOnly = true)
    public Map<String, String> all(PortalUser actor) {
        AccessGuard.requireAdmin(actor);
        return snapshot();
    }

    public Map<String, String> snapshot() {
        Map<String, String> values = new TreeMap<>();
        repository.findAll().forEach(s -> values.put(s.getKey(), s.getValue()));
        return values;
    }

    public Map<String, Map<String, String>> strategies() {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        result.put("priority", priorityStrategies.describe());
        result.put("assignment", assignmentStrategies.describe());
        result.put("classifier", classifiers.describe());
        return result;
    }

    @Transactional
    public Map<String, String> update(PortalUser actor, Map<String, String> values) {
        AccessGuard.requireAdmin(actor);
        if (values == null || values.isEmpty()) throw new ValidationException("Settings are required.");
        Set<String> allowed = new LinkedHashSet<>(Set.of(PRIORITY_STRATEGY, ASSIGNMENT_STRATEGY, CLASSIFIER_STRATEGY,
                WARNING_PERCENT, ESCALATION_ENABLED));
        for (Priority p : Priority.values()) {
            allowed.add(responseKey(p));
            allowed.add(resolutionKey(p));
        }
        for (String key : values.keySet()) {
            if (!allowed.contains(key)) throw new ValidationException("Unsupported setting: " + key);
        }
        Map<String, String> merged = new HashMap<>(snapshot());
        merged.putAll(values);
        if (!priorityStrategies.supports(merged.get(PRIORITY_STRATEGY))) throw new ValidationException("Invalid priority strategy.");
        if (!assignmentStrategies.supports(merged.get(ASSIGNMENT_STRATEGY))) throw new ValidationException("Invalid assignment strategy.");
        if (!classifiers.supports(merged.getOrDefault(CLASSIFIER_STRATEGY, "KEYWORD"))) throw new ValidationException("Invalid classifier.");
        number(merged.get(WARNING_PERCENT), "SLA warning percentage", 50, 99);
        String escalation = merged.getOrDefault(ESCALATION_ENABLED, "true");
        if (!escalation.equals("true") && !escalation.equals("false")) throw new ValidationException("Escalation must be true or false.");
        for (Priority p : Priority.values()) {
            int response = number(merged.get(responseKey(p)), p + " response minutes", 1, 43200);
            int resolution = number(merged.get(resolutionKey(p)), p + " resolution minutes", 1, 43200);
            if (response > resolution) throw new ValidationException(p + " response time cannot exceed resolution time.");
        }
        Map<String, String> before = snapshot();
        values.forEach((key, value) -> {
            String clean = key.endsWith(".strategy") ? value.strip().toUpperCase() : value.strip();
            SystemSetting setting = repository.findById(key).orElseGet(() -> new SystemSetting(key, clean));
            setting.change(clean);
            repository.save(setting);
            if (!Objects.equals(before.get(key), clean)) {
                audit.recordSystem(actor.getId(), "SETTING_CHANGED", key + ": " + before.get(key) + " -> " + clean);
            }
        });
        return snapshot();
    }

    /** Package-level helper for tests and the seeder: updates without an actor or audit row. */
    @Transactional
    public void put(String key, String value) {
        SystemSetting setting = repository.findById(key).orElseGet(() -> new SystemSetting(key, value));
        setting.change(value);
        repository.save(setting);
    }

    public SupportContactDto supportContact() {
        String phone = supportPhone == null || supportPhone.isBlank() ? "Not configured" : supportPhone;
        return new SupportContactDto(supportEmail, phone, "Contact via email; response governed by the incident SLA",
                "For incident updates, use the conversation inside your ticket.");
    }

    private static int number(String raw, String label, int min, int max) {
        int value;
        try {
            value = Integer.parseInt(raw == null ? "" : raw.strip());
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a whole number.");
        }
        if (value < min || value > max) throw new ValidationException(label + " must be between " + min + " and " + max + ".");
        return value;
    }
}
