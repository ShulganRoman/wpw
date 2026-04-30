package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.dealer.DealerSkuResolverService;
import com.wpw.pim.service.media.ProductMediaService;
import com.wpw.pim.service.pricing.PriceResolverService;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.media.MediaImageDto;
import com.wpw.pim.web.dto.product.*;
import com.wpw.pim.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
    @MockitoBean private PriceResolverService priceResolverService;
    @MockitoBean private DealerSkuResolverService dealerSkuResolverService;
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

    @Nested
    @DisplayName("GET /api/v1/products/{id}/compatible-tools")
    class CompatibleTools {

        @Test
        @DisplayName("возвращает список совместимых инструментов")
        void getCompatibleTools_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getCompatibleTools(id, "en")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/products/" + id + "/compatible-tools"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("возвращает с указанной локалью")
        void getCompatibleTools_withLocale_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getCompatibleTools(id, "ru")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/products/" + id + "/compatible-tools").param("locale", "ru"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProduct {

        @Test
        @WithMockUser(authorities = "MODIFY_PRODUCTS")
        @DisplayName("авторизованный пользователь обновляет продукт -- 200")
        void update_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            ProductDetailDto detail = createDetailDto("UPDATED-001");
            when(productService.updateProduct(eq(id), eq("en"), any(ProductUpdateDto.class)))
                    .thenReturn(detail);

            mockMvc.perform(put("/api/v1/products/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Updated\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.toolNo").value("UPDATED-001"));
        }

        @Test
        @DisplayName("неавторизованный -- 403")
        void update_unauthenticated_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(put("/api/v1/products/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "BULK_EXPORT")
        @DisplayName("без MODIFY_PRODUCTS -- 403")
        void update_forbidden_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(put("/api/v1/products/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "MODIFY_PRODUCTS")
        @DisplayName("обновление с другой локалью")
        void update_withDifferentLocale_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            ProductDetailDto detail = createDetailDto("RU-001");
            when(productService.updateProduct(eq(id), eq("ru"), any(ProductUpdateDto.class)))
                    .thenReturn(detail);

            mockMvc.perform(put("/api/v1/products/" + id)
                            .param("locale", "ru")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id} -- security")
    class DeleteProductSecurity {

        @Test
        @DisplayName("неавторизованный -- 403")
        void delete_unauthenticated_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(delete("/api/v1/products/" + id))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "BULK_EXPORT")
        @DisplayName("без MODIFY_PRODUCTS -- 403")
        void delete_forbidden_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            mockMvc.perform(delete("/api/v1/products/" + id))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}/images")
    class GetImages {

        @Test
        @DisplayName("возвращает список изображений")
        void getImages_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            UUID imgId = UUID.randomUUID();
            when(productMediaService.getImages(id))
                    .thenReturn(List.of(new MediaImageDto(imgId, "/media/products/T-001/1.webp", 0)));

            mockMvc.perform(get("/api/v1/products/" + id + "/images"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].url").value("/media/products/T-001/1.webp"))
                    .andExpect(jsonPath("$[0].sortOrder").value(0));
        }

        @Test
        @DisplayName("пустой список -- 200")
        void getImages_empty_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            when(productMediaService.getImages(id)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/products/" + id + "/images"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products/{id}/images")
    class AddImages {

        @Test
        @WithMockUser(authorities = "MODIFY_PRODUCTS")
        @DisplayName("авторизованный пользователь добавляет изображения -- 200")
        void addImages_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            MockMultipartFile file = new MockMultipartFile(
                    "files", "img.jpg", "image/jpeg", new byte[]{1, 2, 3});
            UUID imgId = UUID.randomUUID();
            when(productMediaService.addImages(eq(id), any()))
                    .thenReturn(List.of(new MediaImageDto(imgId, "/media/products/T-001/1.webp", 0)));

            mockMvc.perform(multipart("/api/v1/products/" + id + "/images").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].url").value("/media/products/T-001/1.webp"));
        }

        @Test
        @DisplayName("неавторизованный -- 403")
        void addImages_unauthenticated_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            MockMultipartFile file = new MockMultipartFile(
                    "files", "img.jpg", "image/jpeg", new byte[]{1});
            mockMvc.perform(multipart("/api/v1/products/" + id + "/images").file(file))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}/images/{imageId}")
    class DeleteImage {

        @Test
        @WithMockUser(authorities = "MODIFY_PRODUCTS")
        @DisplayName("авторизованный пользователь удаляет изображение -- 200")
        void deleteImage_authorized_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            UUID imgId = UUID.randomUUID();
            when(productMediaService.deleteImage(id, imgId)).thenReturn(List.of());

            mockMvc.perform(delete("/api/v1/products/" + id + "/images/" + imgId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(productMediaService).deleteImage(id, imgId);
        }

        @Test
        @DisplayName("неавторизованный -- 403")
        void deleteImage_unauthenticated_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            UUID imgId = UUID.randomUUID();
            mockMvc.perform(delete("/api/v1/products/" + id + "/images/" + imgId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "BULK_EXPORT")
        @DisplayName("без MODIFY_PRODUCTS -- 403")
        void deleteImage_forbidden_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            UUID imgId = UUID.randomUUID();
            mockMvc.perform(delete("/api/v1/products/" + id + "/images/" + imgId))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products -- with filters")
    class ListWithFilters {

        @Test
        @DisplayName("фильтр по sectionId/categoryId/groupId")
        void list_withCatalogFilters_returns200() throws Exception {
            PagedResponse<ProductSummaryDto> response = PagedResponse.of(List.of(), 0, 1, 48);
            when(productService.findAll(any(ProductFilter.class))).thenReturn(response);

            mockMvc.perform(get("/api/v1/products")
                            .param("sectionId", UUID.randomUUID().toString())
                            .param("categoryId", UUID.randomUUID().toString())
                            .param("groupId", UUID.randomUUID().toString())
                            .param("operation", "drilling")
                            .param("toolMaterial", "HSS", "carbide")
                            .param("workpieceMaterial", "steel")
                            .param("machineType", "lathe")
                            .param("machineBrand", "BrandA")
                            .param("cuttingType", "rough")
                            .param("dMmMin", "1.5")
                            .param("dMmMax", "5.0")
                            .param("shankMm", "6.0")
                            .param("hasBallBearing", "true")
                            .param("productType", "main")
                            .param("inStock", "true")
                            .param("priceMin", "10")
                            .param("priceMax", "100")
                            .param("page", "2")
                            .param("perPage", "24"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("список с авторизацией и непустым результатом -- enrich applied")
        @WithMockUser(authorities = "BULK_EXPORT")
        void list_authenticated_enrichesItems() throws Exception {
            UUID id = UUID.randomUUID();
            ProductSummaryDto item = new ProductSummaryDto(
                    id, "TOOL-001", null, "Tool", null, ProductType.main, ProductStatus.active,
                    true, BigDecimal.valueOf(1.5), null, null, null, null, "en", false,
                    null, null);
            PagedResponse<ProductSummaryDto> response = PagedResponse.of(List.of(item), 1, 1, 48);
            when(productService.findAll(any(ProductFilter.class))).thenReturn(response);
            when(priceResolverService.resolveBatch(any(), any())).thenReturn(Map.of());
            when(dealerSkuResolverService.resolveBatch(any(), any())).thenReturn(Map.of());

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("список с авторизацией и enrichment с ценами и SKU")
        @WithMockUser(authorities = "BULK_EXPORT")
        void list_authenticated_enrichesWithPrices() throws Exception {
            UUID id = UUID.randomUUID();
            ProductSummaryDto item = new ProductSummaryDto(
                    id, "TOOL-001", null, "Tool", null, ProductType.main, ProductStatus.active,
                    true, BigDecimal.valueOf(1.5), null, null, null, null, "en", false,
                    null, null);
            PagedResponse<ProductSummaryDto> response = PagedResponse.of(List.of(item), 1, 1, 48);
            when(productService.findAll(any(ProductFilter.class))).thenReturn(response);

            PriceInfoDto price = new PriceInfoDto("EUR", "€", List.of(), false, null);
            when(priceResolverService.resolveBatch(any(), any())).thenReturn(Map.of(id, price));
            when(dealerSkuResolverService.resolveBatch(any(), any()))
                    .thenReturn(Map.of("TOOL-001", "DEALER-X"));

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].dealerSku").value("DEALER-X"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{toolNo} -- with auth/price")
    class GetByToolNoWithPrice {

        @Test
        @DisplayName("с ценой и dealerSku возвращает enriched DTO")
        @WithMockUser(authorities = "BULK_EXPORT")
        void getByToolNo_withPrice_returnsEnriched() throws Exception {
            ProductDetailDto detail = createDetailDto("TOOL-001");
            when(productService.findByToolNo("TOOL-001", "en")).thenReturn(detail);
            PriceInfoDto price = new PriceInfoDto("EUR", "€", List.of(), false, null);
            when(priceResolverService.resolve(eq("TOOL-001"), any())).thenReturn(price);
            when(dealerSkuResolverService.resolve(eq("TOOL-001"), any())).thenReturn("MY-SKU");

            mockMvc.perform(get("/api/v1/products/TOOL-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.toolNo").value("TOOL-001"))
                    .andExpect(jsonPath("$.dealerSku").value("MY-SKU"));
        }

        @Test
        @DisplayName("без price и dealerSku возвращает оригинальный DTO")
        void getByToolNo_noPrice_returnsOriginal() throws Exception {
            ProductDetailDto detail = createDetailDto("TOOL-002");
            when(productService.findByToolNo("TOOL-002", "en")).thenReturn(detail);
            when(priceResolverService.resolve(eq("TOOL-002"), any())).thenReturn(null);
            when(dealerSkuResolverService.resolve(eq("TOOL-002"), any())).thenReturn(null);

            mockMvc.perform(get("/api/v1/products/TOOL-002"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.toolNo").value("TOOL-002"));
        }
    }

    // --- Helpers ---

    private ProductDetailDto createDetailDto(String toolNo) {
        return new ProductDetailDto(
                UUID.randomUUID(), toolNo, null, ProductType.main, ProductStatus.active, true, null,
                "Test Product", null, null, null, null, null, false,
                "en", false, null,
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                List.of(), null, null, null, null, null, null, null
        );
    }
}
