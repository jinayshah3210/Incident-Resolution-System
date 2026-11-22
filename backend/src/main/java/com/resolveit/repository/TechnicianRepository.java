package com.resolveit.repository;

import com.resolveit.domain.user.Technician;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: technicians only (JOINED subclass). */
public interface TechnicianRepository extends JpaRepository<Technician, String> {
    List<Technician> findByActiveTrueOrderByIdAsc();
    List<Technician> findByTeamLeadTrueAndActiveTrue();
}
