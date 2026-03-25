package com.wpw.pim.web.controller;

import com.wpw.pim.service.catalog.CatalogService;
import com.wpw.pim.web.dto.catalog.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasAuthority('MANAGE_CATALOG')")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CatalogService catalogService;

    // --- Sections ---
    @PostMapping("/sections")
    public SectionDto createSection(@RequestBody CreateSectionRequest req,
                                    @RequestParam(defaultValue = "en") String locale) {
        return catalogService.createSection(req, locale);
    }

    @PutMapping("/sections/{id}")
    public SectionDto updateSection(@PathVariable UUID id, @RequestBody UpdateSectionRequest req,
                                    @RequestParam(defaultValue = "en") String locale) {
        return catalogService.updateSection(id, req, locale);
    }

    @DeleteMapping("/sections/{id}")
    public void deleteSection(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean cascade) {
        catalogService.deleteSection(id, cascade);
    }

    @PutMapping("/sections/reorder")
    public void reorderSections(@RequestBody ReorderRequest req) {
        catalogService.reorderSections(req);
    }

    @GetMapping("/sections/{id}/children-count")
    public ChildrenCountResponse sectionChildrenCount(@PathVariable UUID id) {
        return catalogService.getChildrenCount(id);
    }

    // --- Categories ---
    @PostMapping("/categories")
    public CategoryDto createCategory(@RequestBody CreateCategoryRequest req,
                                      @RequestParam(defaultValue = "en") String locale) {
        return catalogService.createCategory(req, locale);
    }

    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable UUID id, @RequestBody UpdateCategoryRequest req,
                                      @RequestParam(defaultValue = "en") String locale) {
        return catalogService.updateCategory(id, req, locale);
    }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean cascade) {
        catalogService.deleteCategory(id, cascade);
    }

    @PutMapping("/categories/reorder")
    public void reorderCategories(@RequestBody ReorderRequest req) {
        catalogService.reorderCategories(req);
    }

    @GetMapping("/categories/{id}/children-count")
    public Map<String, Long> categoryChildrenCount(@PathVariable UUID id) {
        return Map.of("productGroups", catalogService.getCategoryChildrenCount(id));
    }

    // --- Product Groups ---
    @PostMapping("/product-groups")
    public ProductGroupDto createProductGroup(@RequestBody CreateProductGroupRequest req,
                                              @RequestParam(defaultValue = "en") String locale) {
        return catalogService.createProductGroup(req, locale);
    }

    @PutMapping("/product-groups/{id}")
    public ProductGroupDto updateProductGroup(@PathVariable UUID id, @RequestBody UpdateProductGroupRequest req,
                                              @RequestParam(defaultValue = "en") String locale) {
        return catalogService.updateProductGroup(id, req, locale);
    }

    @DeleteMapping("/product-groups/{id}")
    public void deleteProductGroup(@PathVariable UUID id) {
        catalogService.deleteProductGroup(id);
    }

    @PutMapping("/product-groups/reorder")
    public void reorderProductGroups(@RequestBody ReorderRequest req) {
        catalogService.reorderProductGroups(req);
    }
}
