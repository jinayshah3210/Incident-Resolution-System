package com.resolveit.repository;

import com.resolveit.domain.catalog.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: dynamic incident categories and their keywords. */
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    List<Category> findByActiveTrueOrderByNameAsc();
    List<Category> findAllByOrderByNameAsc();
}
