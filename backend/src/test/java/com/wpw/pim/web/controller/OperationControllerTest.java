package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.domain.operation.Operation;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.operation.OperationService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.operation.ApplicationTagUpsertDto;
import com.wpw.pim.web.dto.operation.OperationDto;
import com.wpw.pim.web.dto.product.ProductSummaryDto;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link OperationController}.
 * Публичные GET и защищённые CRUD для application tags.
 */
@Import(SecurityConfig.class)
@WebMvcTest(OperationController.class)
class OperationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OperationService operationService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Nested
    @DisplayName("GET /api/v1/operations")
    class ListOperations {

        @Test
        @DisplayName("возвращает список операций")
        void list_returnsOperations() throws Exception {
            Operation op = new Operation();
            op.setCode("drilling");
            op.setName("Drilling");
            op.setSortOrder(0);

            when(operationService.findAll()).thenReturn(List.of(op));

            mockMvc.perform(get("/api/v1/operations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].code").value("drilling"))
                    .andExpect(jsonPath("$[0].name").value("Drilling"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/operations/{code}/products")
    class ProductsByOperation {

        @Test
        @DisplayName("возвращает продукты по операции")
        void productsByOperation_returnsPagedResponse() throws Exception {
            PagedResponse<ProductSummaryDto> response = PagedResponse.of(List.of(), 0, 1, 48);
            when(operationService.findProductsByOperation("drilling", "en", 1, 48)).thenReturn(response);

            mockMvc.perform(get("/api/v1/operations/drilling/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/operations (protected)")
    class CreateOperation {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("авторизованный пользователь создаёт операцию -- 201")
        void create_authorized_returns201() throws Exception {
            OperationDto dto = new OperationDto("new-op", "New Op", 0);
            when(operationService.create(any(ApplicationTagUpsertDto.class))).thenReturn(dto);

            mockMvc.perform(post("/api/v1/operations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ApplicationTagUpsertDto("New Op", 0))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("new-op"));
        }

        @Test
        @DisplayName("неавторизованный пользователь -- 401")
        void create_unauthenticated_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/operations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "MODIFY_PRODUCTS")
        @DisplayName("пользователь без MANAGE_CATALOG -- 403")
        void create_forbidden_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/operations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/operations/{code} (protected)")
    class UpdateOperation {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("обновляет операцию")
        void update_authorized_returns200() throws Exception {
            OperationDto dto = new OperationDto("drilling", "Updated Drilling", 5);
            when(operationService.update(eq("drilling"), any(ApplicationTagUpsertDto.class))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/operations/drilling")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ApplicationTagUpsertDto("Updated Drilling", 5))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Drilling"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/operations/{code} (protected)")
    class DeleteOperation {

        @Test
        @WithMockUser(authorities = "MANAGE_CATALOG")
        @DisplayName("удаляет операцию -- 204")
        void delete_authorized_returns204() throws Exception {
            mockMvc.perform(delete("/api/v1/operations/drilling"))
                    .andExpect(status().isNoContent());

            verify(operationService).delete("drilling");
        }
    }
}
