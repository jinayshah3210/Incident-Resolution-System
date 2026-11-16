
package com.irs.backend.strategy.assignment;

import java.util.List;
import java.util.Comparator;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.irs.backend.model.Incident;
import com.irs.backend.model.Technician;
import com.irs.backend.exception.TechnicianUnavailableException;

@Component
@Primary
public class SkillBasedAssignmentStrategy implements TechnicianAssignmentStrategy {

    private static final Map<String, String> SKILL_MAPPING = Map.of(
            "APPLICATION", "Software",
            "NETWORK", "Network",
            "ENDPOINT", "Hardware"
    );

    @Override
    public Technician assign(Incident incident, List<Technician> technicians) {

        String requiredSkill = SKILL_MAPPING.getOrDefault(
                incident.getCategory().toUpperCase(),
                incident.getCategory()
        );

        return technicians.stream()
                .filter(Technician::isAvailable)
                .filter(t -> t.getSkills() != null &&
                        t.getSkills().stream().anyMatch(skill ->
                                skill != null &&
                                skill.equalsIgnoreCase(requiredSkill)))
                .min(Comparator.comparingInt(Technician::getActiveIncidentCount))
                .orElseThrow(() -> new TechnicianUnavailableException(
                        "No available technician found with skill: " + requiredSkill
                ));
    }
}
