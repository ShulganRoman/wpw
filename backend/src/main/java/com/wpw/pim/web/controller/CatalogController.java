package com.wpw.pim.web.controller;

import com.wpw.pim.service.catalog.CatalogService;
import com.wpw.pim.service.settings.SystemSettingsService;
import com.wpw.pim.web.dto.catalog.SectionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Дерево каталога: секции → категории → группы товаров")
public class CatalogController {

    private final CatalogService catalogService;
    private final SystemSettingsService systemSettings;

    @GetMapping
    @Operation(summary = "Получить дерево каталога", description = "Администратор видит все узлы включая пустые. Дилеры и пользователи видят только узлы с доступными товарами.")
    public List<SectionDto> getTree(@RequestParam(defaultValue = "en") String locale) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = systemSettings.isAdminRole(auth);
        boolean requireImages = !isAdmin && systemSettings.shouldRequireImages(auth);
        UUID priceListId = (!isAdmin && systemSettings.shouldRequirePrice(auth))
            ? systemSettings.getDealerPriceListId(auth)
            : null;
        return catalogService.getSectionTree(locale, !isAdmin, requireImages, priceListId);
    }
}
