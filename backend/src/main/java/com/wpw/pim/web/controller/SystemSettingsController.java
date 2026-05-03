package com.wpw.pim.web.controller;

import com.wpw.pim.service.settings.SystemSettingsService;
import com.wpw.pim.service.settings.SystemStatsService;
import com.wpw.pim.web.dto.settings.SystemSettingsDto;
import com.wpw.pim.web.dto.settings.SystemStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasAuthority('MANAGE_PRODUCTS') or hasAuthority('MODIFY_PRODUCTS') or hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin: Settings", description = "System settings: product filtering by media availability, system statistics.")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Admin/MANAGE_PRODUCTS required")
})
public class SystemSettingsController {

    private final SystemSettingsService settingsService;
    private final SystemStatsService statsService;

    @GetMapping
    @Operation(summary = "Get system settings")
    @ApiResponse(responseCode = "200", description = "Current settings")
    public SystemSettingsDto get() {
        return settingsService.get();
    }

    @PutMapping
    @Operation(summary = "Update system settings")
    @ApiResponse(responseCode = "200", description = "Updated settings")
    public SystemSettingsDto update(@RequestBody SystemSettingsDto dto) {
        return settingsService.update(dto);
    }

    @GetMapping("/stats")
    @Operation(summary = "System statistics", description = "Media coverage, price lists, dealers, catalog — for analytics and accounting.")
    @ApiResponse(responseCode = "200", description = "System statistics")
    public SystemStatsDto stats() {
        return statsService.getStats();
    }
}
