package com.irs.backend.strategy.assignment;

import java.util.List;
import com.irs.backend.model.Incident;
import com.irs.backend.model.Technician;

public interface TechnicianAssignmentStrategy {

    Technician assign(
        Incident incident,
        List<Technician> technicians
    );

}