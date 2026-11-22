package com.resolveit.repository;

import com.resolveit.domain.user.PortalUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repository pattern: polymorphic access to every account (Hibernate returns the concrete subclass). */
public interface UserRepository extends JpaRepository<PortalUser, String> {
    Optional<PortalUser> findByEmailIgnoreCase(String email);
    Optional<PortalUser> findByIdIgnoreCase(String id);
    boolean existsByEmailIgnoreCase(String email);
    List<PortalUser> findAllByOrderByIdAsc();

    @Query("select u.id from PortalUser u where u.id like concat(:prefix, '%')")
    List<String> findIdsStartingWith(String prefix);
}
