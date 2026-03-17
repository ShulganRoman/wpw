package com.wpw.pim.service.catalog;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.catalog.Section;
import com.wpw.pim.repository.catalog.CategoryRepository;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.catalog.SectionRepository;
import com.wpw.pim.web.dto.catalog.CategoryDto;
import com.wpw.pim.web.dto.catalog.ProductGroupDto;
import com.wpw.pim.web.dto.catalog.SectionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final SectionRepository sectionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductGroupRepository productGroupRepository;

    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<SectionDto> getSectionTree(String locale) {
        List<Section> sections = sectionRepository.findAllByIsActiveTrueOrderBySortOrder();
        List<Category> categories = categoryRepository.findAllActiveWithSection();
        List<ProductGroup> groups = productGroupRepository.findAllActiveWithCategory();

        Map<UUID, List<ProductGroupDto>> groupsByCategory = groups.stream()
            .collect(Collectors.groupingBy(
                g -> g.getCategory().getId(),
                Collectors.mapping(g -> toGroupDto(g, locale), Collectors.toList())
            ));

        Map<UUID, List<CategoryDto>> categoriesBySection = categories.stream()
            .collect(Collectors.groupingBy(
                c -> c.getSection().getId(),
                Collectors.mapping(c -> toCategoryDto(c, locale, groupsByCategory.getOrDefault(c.getId(), List.of())),
                    Collectors.toList())
            ));

        return sections.stream()
            .map(s -> toSectionDto(s, locale, categoriesBySection.getOrDefault(s.getId(), List.of())))
            .toList();
    }

    private String translate(Map<String, String> translations, String locale) {
        return translations.getOrDefault(locale, translations.getOrDefault("en", ""));
    }

    private SectionDto toSectionDto(Section s, String locale, List<CategoryDto> categories) {
        return new SectionDto(s.getId(), s.getSlug(), translate(s.getTranslations(), locale), s.getSortOrder(), categories);
    }

    private CategoryDto toCategoryDto(Category c, String locale, List<ProductGroupDto> groups) {
        return new CategoryDto(c.getId(), c.getSlug(), translate(c.getTranslations(), locale), c.getSortOrder(), groups);
    }

    private ProductGroupDto toGroupDto(ProductGroup g, String locale) {
        return new ProductGroupDto(g.getId(), g.getSlug(), g.getGroupCode(), translate(g.getTranslations(), locale), g.getSortOrder());
    }
}
