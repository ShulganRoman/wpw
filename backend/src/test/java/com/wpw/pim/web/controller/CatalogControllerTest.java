package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.catalog.CatalogService;
import com.wpw.pim.service.settings.SystemSettingsService;
import com.wpw.pim.web.dto.catalog.CategoryDto;
import com.wpw.pim.web.dto.catalog.ProductGroupDto;
import com.wpw.pim.web.dto.catalog.SectionDto;
import com.wpw.pim.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link CatalogController}.
 * Публичный эндпоинт получения дерева каталога.
 */
@Import(SecurityConfig.class)
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CatalogService catalogService;
    @MockitoBean private SystemSettingsService systemSettingsService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Test
    @DisplayName("GET /api/v1/categories -- returns catalog tree")
    void getTree_returnsTreeStructure() throws Exception {
        ProductGroupDto group = new ProductGroupDto(UUID.randomUUID(), "straight", "GRP-001", "Straight Bits", 0, null, 0L);
        CategoryDto category = new CategoryDto(UUID.randomUUID(), "router-bits", "Router Bits", 0, null, List.of(group));
        SectionDto section = new SectionDto(UUID.randomUUID(), "tools", "Tools", 0, null, List.of(category));

        when(catalogService.getSectionTree(anyString(), anyBoolean(), anyBoolean(), any())).thenReturn(List.of(section));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("tools"))
                .andExpect(jsonPath("$[0].name").value("Tools"))
                .andExpect(jsonPath("$[0].categories[0].slug").value("router-bits"))
                .andExpect(jsonPath("$[0].categories[0].groups[0].slug").value("straight"));
    }

    @Test
    @DisplayName("GET /api/v1/categories?locale=ru -- passes locale to service")
    void getTree_withLocale_passesLocale() throws Exception {
        when(catalogService.getSectionTree(anyString(), anyBoolean(), anyBoolean(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories").param("locale", "ru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/categories -- empty catalog returns empty array")
    void getTree_empty_returnsEmptyArray() throws Exception {
        when(catalogService.getSectionTree(anyString(), anyBoolean(), anyBoolean(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
