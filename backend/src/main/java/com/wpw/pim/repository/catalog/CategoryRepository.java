package com.wpw.pim.repository.catalog;

import com.wpw.pim.domain.catalog.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    @Query("SELECT c FROM Category c JOIN FETCH c.section WHERE c.isActive = true ORDER BY c.sortOrder")
    List<Category> findAllActiveWithSection();
    Optional<Category> findBySlug(String slug);

    @Query(value = "SELECT * FROM categories WHERE translations->>'en' ILIKE :nameEn LIMIT 1", nativeQuery = true)
    Optional<Category> findByNameEnIgnoreCase(@Param("nameEn") String nameEn);

    List<Category> findBySectionId(UUID sectionId);
    void deleteBySectionId(UUID sectionId);
    long countBySectionId(UUID sectionId);
}
