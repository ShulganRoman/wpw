package com.wpw.pim.repository.catalog;

import com.wpw.pim.domain.catalog.ProductGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductGroupRepository extends JpaRepository<ProductGroup, UUID> {
    @Query("SELECT g FROM ProductGroup g JOIN FETCH g.category WHERE g.isActive = true ORDER BY g.sortOrder")
    List<ProductGroup> findAllActiveWithCategory();

    @Query("SELECT g.category.id FROM ProductGroup g WHERE g.id = :groupId")
    UUID findCategoryIdByGroupId(UUID groupId);

    Optional<ProductGroup> findBySlug(String slug);
    List<ProductGroup> findByCategoryId(UUID categoryId);
    void deleteByCategoryId(UUID categoryId);
    long countByCategoryId(UUID categoryId);
    List<ProductGroup> findByCategoryIdIn(List<UUID> categoryIds);
    long countByCategoryIdIn(List<UUID> categoryIds);
}
