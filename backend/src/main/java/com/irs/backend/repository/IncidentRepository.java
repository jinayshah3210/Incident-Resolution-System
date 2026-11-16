package com.irs.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.irs.backend.model.Incident;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByReporterId(Long reporterId);

    List<Incident> findByAssignedTechnicianId(Long technicianId);
}