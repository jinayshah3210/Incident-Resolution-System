package com.resolveit.repository;

import com.resolveit.domain.user.AdminLevel;
import com.resolveit.domain.user.Administrator;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: administrators and IT managers. */
public interface AdministratorRepository extends JpaRepository<Administrator, String> {
    List<Administrator> findByAdminLevelAndActiveTrueOrderByIdAsc(AdminLevel level);
}
