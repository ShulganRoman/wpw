package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.settings.SystemSettingsService;
import com.wpw.pim.service.settings.SystemStatsService;
import com.wpw.pim.web.dto.settings.SystemSettingsDto;
import com.wpw.pim.web.dto.settings.SystemStatsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-тесты для {@link SystemSettingsController}.
 * Эндпоинты требуют MANAGE_PRODUCTS / MODIFY_PRODUCTS / ROLE_ADMIN.
 */
@Import(SecurityConfig.class)
@WebMvcTest(SystemSettingsController.class)
class SystemSettingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SystemSettingsService settingsService;
    @MockitoBean private SystemStatsService statsService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Test
    @WithMockUser(authorities = "MANAGE_PRODUCTS")
    @DisplayName("GET /settings -- returns 200 and JSON with three boolean fields")
    void getSettings_returnsDto() throws Exception {
        when(settingsService.get()).thenReturn(new SystemSettingsDto(true, false, true, false, false, false));

        mockMvc.perform(get("/api/v1/admin/settings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requireImagesAdmin").value(true))
            .andExpect(jsonPath("$.requireImagesDealer").value(false))
            .andExpect(jsonPath("$.requireImagesPublic").value(true));
    }

    @Test
    @DisplayName("GET /settings -- without authentication returns 4xx")
    void getSettings_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRODUCTS")
    @DisplayName("PUT /settings -- returns 200 and updated settings")
    void updateSettings_returnsUpdatedDto() throws Exception {
        SystemSettingsDto updated = new SystemSettingsDto(true, true, false, false, false, false);
        when(settingsService.update(any(SystemSettingsDto.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requireImagesAdmin").value(true))
            .andExpect(jsonPath("$.requireImagesDealer").value(true))
            .andExpect(jsonPath("$.requireImagesPublic").value(false));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRODUCTS")
    @DisplayName("GET /settings/stats -- returns 200 and JSON with statistics")
    void getStats_returnsStatsDto() throws Exception {
        SystemStatsDto stats = new SystemStatsDto(
            10L, 5L, 5L, 50.0,
            200L,
            50L, 30L, 7L, 3L, 70.0,
            8L, 5L, 3L, 5L,
            2L, 100L,
            4L, 12L, 40L, 5L, 30L,
            OffsetDateTime.now()
        );
        when(statsService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/settings/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalActiveProducts").value(10))
            .andExpect(jsonPath("$.productsWithOwnMedia").value(5))
            .andExpect(jsonPath("$.mediaCoveragePct").value(50.0))
            .andExpect(jsonPath("$.totalDealers").value(8))
            .andExpect(jsonPath("$.totalSections").value(4))
            .andExpect(jsonPath("$.generatedAt").exists());
    }
}
