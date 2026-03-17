package com.wpw.pim.repository.catalog;

import com.wpw.pim.domain.catalog.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findAllByIsActiveTrueOrderBySortOrder();
    Optional<Section> findBySlug(String slug);
}
