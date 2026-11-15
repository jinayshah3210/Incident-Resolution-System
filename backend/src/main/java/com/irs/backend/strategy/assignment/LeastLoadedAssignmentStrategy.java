package com.irs.backend.strategy.assignment;

import java.util.List;
import java.util.Comparator;

import com.irs.backend.model.Incident;
import com.irs.backend.model.Technician;
import com.irs.backend.exception.TechnicianUnavailableException;

public class LeastLoadedAssignmentStrategy implements TechnicianAssignmentStrategy {

    @Override
    public Technician assign(Incident incident, List<Technician> technicians) {

        return technicians.stream()
                .filter(Technician::isAvailable)
                .min(Comparator.comparingInt(Technician::getActiveIncidentCount))
                .orElseThrow(() -> new TechnicianUnavailableException(
                        "No available technician found"));
    }
}