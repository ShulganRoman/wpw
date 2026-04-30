package com.wpw.pim.web.controller;

import com.wpw.pim.service.catalog.CatalogService;
import com.wpw.pim.web.dto.catalog.SectionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Дерево каталога: секции → категории → группы товаров")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    @Operation(summary = "Получить дерево каталога", description = "Возвращает полное дерево: секции, категории и группы товаров с переводами для указанной локали.")
    public List<SectionDto> getTree(@RequestParam(defaultValue = "en") String locale) {
        return catalogService.getSectionTree(locale);
    }
}
