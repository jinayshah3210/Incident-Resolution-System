package com.resolveit.repository;

import com.resolveit.domain.catalog.ServiceTechnician;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: service-to-technician associations. */
public interface ServiceTechnicianRepository extends JpaRepository<ServiceTechnician, Long> {
    List<ServiceTechnician> findByService_Id(Long serviceId);
    List<ServiceTechnician> findByTechnicianId(String technicianId);
    boolean existsByService_IdAndTechnicianId(Long serviceId, String technicianId);
    void deleteByService_IdAndTechnicianId(Long serviceId, String technicianId);
    void deleteByService_Id(Long serviceId);
}
