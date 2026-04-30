package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.catalog.CatalogImageService;
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
    @MockitoBean private CatalogImageService catalogImageService;
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
            SectionDto dto = new SectionDto(UUID.randomUUID(), "tools", "Tools", 1, null, List.of());
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
            SectionDto dto = new SectionDto(id, "updated", "Updated", 1, null, List.of());
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
            CategoryDto dto = new CategoryDto(UUID.randomUUID(), "bits", "Bits", 0, null, List.of());
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
            ProductGroupDto dto = new ProductGroupDto(UUID.randomUUID(), "straight", "GRP-001", "Straight", 0, null, 0L);
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

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("PUT /api/v1/admin/catalog/product-groups/{id} -- обновляет группу")
        void updateProductGroup_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            ProductGroupDto dto = new ProductGroupDto(id, "updated", "GRP-99", "Updated", 0, null, 0L);
            when(catalogService.updateProductGroup(eq(id), any(UpdateProductGroupRequest.class), eq("en")))
                    .thenReturn(dto);

            mockMvc.perform(put("/api/v1/admin/catalog/product-groups/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"slug\":\"updated\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("updated"));
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("PUT /api/v1/admin/catalog/product-groups/reorder -- переупорядочивает")
        void reorderProductGroups_authorized_returns200() throws Exception {
            mockMvc.perform(put("/api/v1/admin/catalog/product-groups/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\":[]}"))
                    .andExpect(status().isOk());

            verify(catalogService).reorderProductGroups(any(ReorderRequest.class));
        }
    }

    @Nested
    @DisplayName("Categories — additional")
    class CategoriesAdditional {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("PUT /api/v1/admin/catalog/categories/{id} -- обновляет категорию")
        void updateCategory_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            CategoryDto dto = new CategoryDto(id, "updated", "Updated", 0, null, List.of());
            when(catalogService.updateCategory(eq(id), any(UpdateCategoryRequest.class), eq("en")))
                    .thenReturn(dto);

            mockMvc.perform(put("/api/v1/admin/catalog/categories/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"slug\":\"updated\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("DELETE /api/v1/admin/catalog/categories/{id} -- удаляет категорию")
        void deleteCategory_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/admin/catalog/categories/" + id))
                    .andExpect(status().isOk());

            verify(catalogService).deleteCategory(id, false);
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("DELETE /api/v1/admin/catalog/categories/{id}?cascade=true")
        void deleteCategory_cascade_returns200() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/admin/catalog/categories/" + id).param("cascade", "true"))
                    .andExpect(status().isOk());

            verify(catalogService).deleteCategory(id, true);
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("PUT reorder для categories")
        void reorderCategories_returns200() throws Exception {
            mockMvc.perform(put("/api/v1/admin/catalog/categories/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\":[]}"))
                    .andExpect(status().isOk());

            verify(catalogService).reorderCategories(any(ReorderRequest.class));
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("GET /categories/{id}/children-count")
        void categoryChildrenCount_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            when(catalogService.getCategoryChildrenCount(id)).thenReturn(7L);

            mockMvc.perform(get("/api/v1/admin/catalog/categories/" + id + "/children-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productGroups").value(7));
        }
    }

    @Nested
    @DisplayName("Sections reorder")
    class SectionsReorder {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("PUT /api/v1/admin/catalog/sections/reorder")
        void reorderSections_returns200() throws Exception {
            mockMvc.perform(put("/api/v1/admin/catalog/sections/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\":[]}"))
                    .andExpect(status().isOk());

            verify(catalogService).reorderSections(any(ReorderRequest.class));
        }
    }

    @Nested
    @DisplayName("Node images")
    class NodeImages {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("POST /admin/catalog/{nodeType}/{id}/image -- uploads")
        void uploadImage_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            String url = "/media/products/catalog/sections/" + id + ".webp";
            when(catalogImageService.upload(eq("sections"), eq(id), any())).thenReturn(url);

            org.springframework.mock.web.MockMultipartFile file =
                    new org.springframework.mock.web.MockMultipartFile(
                            "file", "img.jpg", "image/jpeg", new byte[]{1, 2, 3});

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .multipart("/api/v1/admin/catalog/sections/" + id + "/image").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.imageUrl").value(url));
        }

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("DELETE /admin/catalog/{nodeType}/{id}/image -- 204")
        void deleteImage_returns204() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/admin/catalog/categories/" + id + "/image"))
                    .andExpect(status().isNoContent());

            verify(catalogImageService).delete("categories", id);
        }
    }
}
