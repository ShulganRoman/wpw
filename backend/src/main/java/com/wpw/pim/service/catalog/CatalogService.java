package com.wpw.pim.service.catalog;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.catalog.Section;
import com.wpw.pim.repository.catalog.CategoryRepository;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.catalog.SectionRepository;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.catalog.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final ProductService productService;

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

    // --- Sections ---

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public SectionDto createSection(CreateSectionRequest req, String locale) {
        Section s = new Section();
        s.setSlug(req.slug());
        s.setTranslations(req.translations());
        s.setSortOrder(req.sortOrder());
        s.setActive(req.isActive());
        s = sectionRepository.save(s);
        return toSectionDto(s, locale, List.of());
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public SectionDto updateSection(UUID id, UpdateSectionRequest req, String locale) {
        Section s = sectionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (req.slug() != null) s.setSlug(req.slug());
        if (req.translations() != null) s.setTranslations(req.translations());
        if (req.sortOrder() != null) s.setSortOrder(req.sortOrder());
        if (req.isActive() != null) s.setActive(req.isActive());
        s = sectionRepository.save(s);
        return toSectionDto(s, locale, List.of());
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deleteSection(UUID id, boolean cascade) {
        Section s = sectionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Category> cats = categoryRepository.findBySectionId(id);
        if (!cascade && !cats.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Section has " + cats.size() + " categories. Use cascade=true to delete all.");
        }
        if (cascade) {
            List<UUID> catIds = cats.stream().map(Category::getId).toList();
            if (!catIds.isEmpty()) {
                List<UUID> groupIds = productGroupRepository.findByCategoryIdIn(catIds)
                    .stream().map(ProductGroup::getId).toList();
                productService.deleteProductsByGroupIds(groupIds);
                for (UUID catId : catIds) {
                    productGroupRepository.deleteByCategoryId(catId);
                }
                categoryRepository.deleteBySectionId(id);
            }
        }
        sectionRepository.delete(s);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void reorderSections(ReorderRequest req) {
        for (var item : req.items()) {
            sectionRepository.findById(item.id()).ifPresent(s -> {
                s.setSortOrder(item.sortOrder());
                sectionRepository.save(s);
            });
        }
    }

    // --- Categories ---

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest req, String locale) {
        Section section = sectionRepository.findById(req.sectionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        Category c = new Category();
        c.setSection(section);
        c.setSlug(req.slug());
        c.setTranslations(req.translations());
        c.setSortOrder(req.sortOrder());
        c.setActive(req.isActive());
        c = categoryRepository.save(c);
        return toCategoryDto(c, locale, List.of());
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryDto updateCategory(UUID id, UpdateCategoryRequest req, String locale) {
        Category c = categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (req.slug() != null) c.setSlug(req.slug());
        if (req.translations() != null) c.setTranslations(req.translations());
        if (req.sortOrder() != null) c.setSortOrder(req.sortOrder());
        if (req.isActive() != null) c.setActive(req.isActive());
        c = categoryRepository.save(c);
        return toCategoryDto(c, locale, List.of());
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deleteCategory(UUID id, boolean cascade) {
        Category c = categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<ProductGroup> groups = productGroupRepository.findByCategoryId(id);
        if (!cascade && !groups.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Category has " + groups.size() + " product groups. Use cascade=true to delete all.");
        }
        if (cascade) {
            List<UUID> groupIds = groups.stream().map(ProductGroup::getId).toList();
            productService.deleteProductsByGroupIds(groupIds);
            productGroupRepository.deleteByCategoryId(id);
        }
        categoryRepository.delete(c);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void reorderCategories(ReorderRequest req) {
        for (var item : req.items()) {
            categoryRepository.findById(item.id()).ifPresent(c -> {
                c.setSortOrder(item.sortOrder());
                categoryRepository.save(c);
            });
        }
    }

    // --- Product Groups ---

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public ProductGroupDto createProductGroup(CreateProductGroupRequest req, String locale) {
        Category category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        ProductGroup g = new ProductGroup();
        g.setCategory(category);
        g.setSlug(req.slug());
        g.setGroupCode(req.groupCode());
        g.setTranslations(req.translations());
        g.setSortOrder(req.sortOrder());
        g.setActive(req.isActive());
        g = productGroupRepository.save(g);
        return toGroupDto(g, locale);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public ProductGroupDto updateProductGroup(UUID id, UpdateProductGroupRequest req, String locale) {
        ProductGroup g = productGroupRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (req.slug() != null) g.setSlug(req.slug());
        if (req.groupCode() != null) g.setGroupCode(req.groupCode());
        if (req.translations() != null) g.setTranslations(req.translations());
        if (req.sortOrder() != null) g.setSortOrder(req.sortOrder());
        if (req.isActive() != null) g.setActive(req.isActive());
        g = productGroupRepository.save(g);
        return toGroupDto(g, locale);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deleteProductGroup(UUID id) {
        ProductGroup g = productGroupRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        productService.deleteProductsByGroupIds(List.of(id));
        productGroupRepository.delete(g);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void reorderProductGroups(ReorderRequest req) {
        for (var item : req.items()) {
            productGroupRepository.findById(item.id()).ifPresent(g -> {
                g.setSortOrder(item.sortOrder());
                productGroupRepository.save(g);
            });
        }
    }

    // --- Children count ---

    public ChildrenCountResponse getChildrenCount(UUID sectionId) {
        long cats = categoryRepository.countBySectionId(sectionId);
        List<Category> catList = categoryRepository.findBySectionId(sectionId);
        List<UUID> catIds = catList.stream().map(Category::getId).toList();
        long groups = catIds.isEmpty() ? 0 : productGroupRepository.countByCategoryIdIn(catIds);
        return new ChildrenCountResponse(cats, groups);
    }

    public long getCategoryChildrenCount(UUID categoryId) {
        return productGroupRepository.countByCategoryId(categoryId);
    }

    // --- Private helpers ---

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
