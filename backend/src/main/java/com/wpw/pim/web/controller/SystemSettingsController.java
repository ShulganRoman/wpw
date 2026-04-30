package com.wpw.pim.web.controller;

import com.wpw.pim.service.settings.SystemSettingsService;
import com.wpw.pim.service.settings.SystemStatsService;
import com.wpw.pim.web.dto.settings.SystemSettingsDto;
import com.wpw.pim.web.dto.settings.SystemStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasAuthority('MANAGE_PRODUCTS') or hasAuthority('MODIFY_PRODUCTS') or hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin: Settings", description = "Системные настройки: фильтрация товаров по наличию медиа, статистика системы.")
public class SystemSettingsController {

    private final SystemSettingsService settingsService;
    private final SystemStatsService statsService;

    @GetMapping
    @Operation(summary = "Получить системные настройки")
    public SystemSettingsDto get() {
        return settingsService.get();
    }

    @PutMapping
    @Operation(summary = "Обновить системные настройки")
    public SystemSettingsDto update(@RequestBody SystemSettingsDto dto) {
        return settingsService.update(dto);
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика системы", description = "Покрытие медиа, прайс-листы, дилеры, каталог — для аналитики и бухгалтера.")
    public SystemStatsDto stats() {
        return statsService.getStats();
    }
}
