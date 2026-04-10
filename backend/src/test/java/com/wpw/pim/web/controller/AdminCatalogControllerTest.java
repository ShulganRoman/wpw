package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.catalog.CatalogService;
import com.wpw.pim.web.dto.catalog.*;
import com.wpw.pim.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link AdminCatalogController}.
 * Все эндпоинты требуют привилегии MANAGE_CATALOG.
 */
@Import(SecurityConfig.class)
@WebMvcTest(AdminCatalogController.class)
class AdminCatalogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CatalogService catalogService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Nested
    @DisplayName("Sections")
    class Sections {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("POST /api/v1/admin/catalog/sections -- создаёт секцию")
        void createSection_authorized_returns200() throws Exception {
            SectionDto dto = new SectionDto(UUID.randomUUID(), "tools", "Tools", 1, List.of());
            when(catalogService.createSection(any(CreateSectionRequest.class), eq("en"))).thenReturn(dto);

            CreateSectionRequest req = new CreateSectionRequest("tools", Map.of("en", "Tools"), 1, true);

            mockMvc.perform(post("/api/v1/admin/catalog/sections")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("tools"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/catalog/sections -- без авторизации возвращает 401")
        void createSection_unauthenticated_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/admin/catalog/sections")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"slug\":\"test\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "BULK_EXPORT")
        @DisplayName("POST /api/v1/admin/catalog/sections -- без MANAGE_CATALOG возвращает 403")
        void createSection_forbidden_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/admin/catalog/sections")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"slug\":\"test\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("PUT /api/v1/admin/catalog/sections/{id} -- обновляет секцию")
        void updateSection_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            SectionDto dto = new SectionDto(id, "updated", "Updated", 1, List.of());
            when(catalogService.updateSection(eq(id), any(UpdateSectionRequest.class), eq("en"))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/admin/catalog/sections/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"slug\":\"updated\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("updated"));
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("DELETE /api/v1/admin/catalog/sections/{id} -- удаляет секцию")
        void deleteSection_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/admin/catalog/sections/" + id))
                    .andExpect(status().isOk());

            verify(catalogService).deleteSection(id, false);
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("DELETE с cascade=true -- каскадное удаление")
        void deleteSection_cascade_deletesAll() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/admin/catalog/sections/" + id).param("cascade", "true"))
                    .andExpect(status().isOk());

            verify(catalogService).deleteSection(id, true);
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("GET /api/v1/admin/catalog/sections/{id}/children-count -- возвращает счётчики")
        void childrenCount_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            when(catalogService.getChildrenCount(id)).thenReturn(new ChildrenCountResponse(3, 10));

            mockMvc.perform(get("/api/v1/admin/catalog/sections/" + id + "/children-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categories").value(3))
                    .andExpect(jsonPath("$.productGroups").value(10));
        }
    }

    @Nested
    @DisplayName("Categories")
    class Categories {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("POST /api/v1/admin/catalog/categories -- создаёт категорию")
        void createCategory_authorized_returns200() throws Exception {
            UUID sectionId = UUID.randomUUID();
            CategoryDto dto = new CategoryDto(UUID.randomUUID(), "bits", "Bits", 0, List.of());
            when(catalogService.createCategory(any(CreateCategoryRequest.class), eq("en"))).thenReturn(dto);

            CreateCategoryRequest req = new CreateCategoryRequest(sectionId, "bits", Map.of("en", "Bits"), 0, true);

            mockMvc.perform(post("/api/v1/admin/catalog/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("bits"));
        }
    }

    @Nested
    @DisplayName("Product Groups")
    class ProductGroups {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("POST /api/v1/admin/catalog/product-groups -- создаёт группу")
        void createProductGroup_authorized_returns200() throws Exception {
            UUID categoryId = UUID.randomUUID();
            ProductGroupDto dto = new ProductGroupDto(UUID.randomUUID(), "straight", "GRP-001", "Straight", 0);
            when(catalogService.createProductGroup(any(CreateProductGroupRequest.class), eq("en"))).thenReturn(dto);

            CreateProductGroupRequest req = new CreateProductGroupRequest(
                    categoryId, "straight", "GRP-001", Map.of("en", "Straight"), 0, true);

            mockMvc.perform(post("/api/v1/admin/catalog/product-groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("straight"));
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("DELETE /api/v1/admin/catalog/product-groups/{id} -- удаляет группу")
        void deleteProductGroup_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/admin/catalog/product-groups/" + id))
                    .andExpect(status().isOk());

            verify(catalogService).deleteProductGroup(id);
        }
    }
}
