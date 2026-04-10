package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.media.ProductMediaService;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.product.*;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link ProductController}.
 * Проверяют все эндпоинты: публичные GET, защищённые POST/PUT/DELETE.
 */
@Import(SecurityConfig.class)
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProductService productService;
    @MockitoBean private ProductMediaService productMediaService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Nested
    @DisplayName("GET /api/v1/products")
    class ListProducts {

        @Test
        @DisplayName("возвращает пагинированный список продуктов")
        void list_returnsPagedResponse() throws Exception {
            PagedResponse<ProductSummaryDto> response = PagedResponse.of(List.of(), 0, 1, 48);
            when(productService.findAll(any(ProductFilter.class))).thenReturn(response);

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.page").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{toolNo}")
    class GetByToolNo {

        @Test
        @DisplayName("возвращает детали продукта")
        void getByToolNo_existingProduct_returns200() throws Exception {
            ProductDetailDto detail = createDetailDto("TOOL-001");
            when(productService.findByToolNo("TOOL-001", "en")).thenReturn(detail);

            mockMvc.perform(get("/api/v1/products/TOOL-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.toolNo").value("TOOL-001"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/filter-options")
    class FilterOptions {

        @Test
        @DisplayName("возвращает доступные фильтры")
        void getFilterOptions_returnsMap() throws Exception {
            Map<String, List<String>> options = Map.of("toolMaterial", List.of("HSS"));
            when(productService.getFilterOptions()).thenReturn(options);

            mockMvc.perform(get("/api/v1/products/filter-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.toolMaterial[0]").value("HSS"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products (protected)")
    class CreateProduct {

        @Test
        @WithMockUser(authorities = "MODIFY_PRODUCTS")
        @DisplayName("авторизованный пользователь создаёт продукт -- 201")
        void create_authorized_returns201() throws Exception {
            ProductDetailDto detail = createDetailDto("NEW-001");
            ProductCreateDto createDto = new ProductCreateDto("NEW-001", null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null);

            when(productService.createProduct(any(ProductCreateDto.class))).thenReturn(detail);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.toolNo").value("NEW-001"));
        }

        @Test
        @DisplayName("неавторизованный пользователь -- 401")
        void create_unauthenticated_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"toolNo\":\"TEST\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "BULK_EXPORT")
        @DisplayName("пользователь без привилегии MODIFY_PRODUCTS -- 403")
        void create_forbidden_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"toolNo\":\"TEST\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id} (protected)")
    class DeleteProduct {

        @Test
        @WithMockUser(authorities = "MODIFY_PRODUCTS")
        @DisplayName("авторизованный пользователь удаляет продукт -- 204")
        void delete_authorized_returns204() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/products/" + id))
                    .andExpect(status().isNoContent());

            verify(productService).deleteProduct(id);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}/spare-parts")
    class SpareParts {

        @Test
        @DisplayName("возвращает список запчастей")
        void getSpareParts_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getSpareParts(id, "en")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/products/" + id + "/spare-parts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // --- Helpers ---

    private ProductDetailDto createDetailDto(String toolNo) {
        return new ProductDetailDto(
                UUID.randomUUID(), toolNo, null, ProductType.main, ProductStatus.active, true, null,
                "Test Product", null, null, null, null, null, false,
                "en", false, null,
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                List.of(), null, null, null, null, null
        );
    }
}
