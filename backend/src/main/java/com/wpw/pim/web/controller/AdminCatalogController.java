package com.wpw.pim.web.controller;

import com.wpw.pim.service.catalog.CatalogImageService;
import com.wpw.pim.service.catalog.CatalogService;
import com.wpw.pim.web.dto.catalog.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasAuthority('MANAGE_CATALOG')")
@RequiredArgsConstructor
@Tag(name = "Admin: Catalog", description = "Управление структурой каталога: секции, категории, группы товаров, изображения узлов. Требует MANAGE_CATALOG.")
public class AdminCatalogController {

    private final CatalogService catalogService;
    private final CatalogImageService catalogImageService;

    // --- Sections ---
    @Operation(summary = "Создать секцию")
    @PostMapping("/sections")
    public SectionDto createSection(@RequestBody CreateSectionRequest req,
                                    @RequestParam(defaultValue = "en") String locale) {
        return catalogService.createSection(req, locale);
    }

    @Operation(summary = "Обновить секцию")
    @PutMapping("/sections/{id}")
    public SectionDto updateSection(@PathVariable UUID id, @RequestBody UpdateSectionRequest req,
                                    @RequestParam(defaultValue = "en") String locale) {
        return catalogService.updateSection(id, req, locale);
    }

    @Operation(summary = "Удалить секцию", description = "cascade=true — каскадное удаление всех дочерних элементов.")
    @DeleteMapping("/sections/{id}")
    public void deleteSection(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean cascade) {
        catalogService.deleteSection(id, cascade);
    }

    @Operation(summary = "Переупорядочить секции")
    @PutMapping("/sections/reorder")
    public void reorderSections(@RequestBody ReorderRequest req) {
        catalogService.reorderSections(req);
    }

    @Operation(summary = "Количество дочерних элементов секции", description = "Возвращает количество категорий и групп товаров внутри секции.")
    @GetMapping("/sections/{id}/children-count")
    public ChildrenCountResponse sectionChildrenCount(@PathVariable UUID id) {
        return catalogService.getChildrenCount(id);
    }

    // --- Categories ---
    @Operation(summary = "Создать категорию")
    @PostMapping("/categories")
    public CategoryDto createCategory(@RequestBody CreateCategoryRequest req,
                                      @RequestParam(defaultValue = "en") String locale) {
        return catalogService.createCategory(req, locale);
    }

    @Operation(summary = "Обновить категорию")
    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable UUID id, @RequestBody UpdateCategoryRequest req,
                                      @RequestParam(defaultValue = "en") String locale) {
        return catalogService.updateCategory(id, req, locale);
    }

    @Operation(summary = "Удалить категорию", description = "cascade=true — каскадное удаление групп товаров.")
    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean cascade) {
        catalogService.deleteCategory(id, cascade);
    }

    @Operation(summary = "Переупорядочить категории")
    @PutMapping("/categories/reorder")
    public void reorderCategories(@RequestBody ReorderRequest req) {
        catalogService.reorderCategories(req);
    }

    @Operation(summary = "Количество групп товаров в категории")
    @GetMapping("/categories/{id}/children-count")
    public Map<String, Long> categoryChildrenCount(@PathVariable UUID id) {
        return Map.of("productGroups", catalogService.getCategoryChildrenCount(id));
    }

    // --- Product Groups ---
    @Operation(summary = "Создать группу товаров")
    @PostMapping("/product-groups")
    public ProductGroupDto createProductGroup(@RequestBody CreateProductGroupRequest req,
                                              @RequestParam(defaultValue = "en") String locale) {
        return catalogService.createProductGroup(req, locale);
    }

    @Operation(summary = "Обновить группу товаров")
    @PutMapping("/product-groups/{id}")
    public ProductGroupDto updateProductGroup(@PathVariable UUID id, @RequestBody UpdateProductGroupRequest req,
                                              @RequestParam(defaultValue = "en") String locale) {
        return catalogService.updateProductGroup(id, req, locale);
    }

    @Operation(summary = "Удалить группу товаров")
    @DeleteMapping("/product-groups/{id}")
    public void deleteProductGroup(@PathVariable UUID id) {
        catalogService.deleteProductGroup(id);
    }

    @Operation(summary = "Переупорядочить группы товаров")
    @PutMapping("/product-groups/reorder")
    public void reorderProductGroups(@RequestBody ReorderRequest req) {
        catalogService.reorderProductGroups(req);
    }

    // --- Node images ---

    @Operation(summary = "Загрузить изображение узла каталога", description = "nodeType: sections, categories, product-groups. Конвертирует в WebP.")
    @PostMapping("/{nodeType}/{id}/image")
    public Map<String, String> uploadImage(
        @PathVariable String nodeType,
        @PathVariable UUID id,
        @RequestParam("file") MultipartFile file
    ) {
        String url = catalogImageService.upload(nodeType, id, file);
        return Map.of("imageUrl", url);
    }

    @Operation(summary = "Удалить изображение узла каталога")
    @DeleteMapping("/{nodeType}/{id}/image")
    public ResponseEntity<Void> deleteImage(
        @PathVariable String nodeType,
        @PathVariable UUID id
    ) {
        catalogImageService.delete(nodeType, id);
        return ResponseEntity.noContent().build();
    }
}
