package com.resolveit.repository;

import com.resolveit.domain.catalog.ItService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: the IT service catalog. */
public interface ItServiceRepository extends JpaRepository<ItService, Long> {
    Optional<ItService> findByNameIgnoreCase(String name);
    List<ItService> findByActiveTrueOrderByNameAsc();
    List<ItService> findAllByOrderByNameAsc();
}
