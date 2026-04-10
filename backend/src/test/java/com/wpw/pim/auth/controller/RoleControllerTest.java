package com.wpw.pim.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.dto.RoleRequest;
import com.wpw.pim.auth.dto.RoleResponse;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.auth.service.RoleService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private RoleService roleService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private RoleResponse sampleRole() {
        return new RoleResponse(1L, "ADMIN", true, Set.of("MODIFY_PRODUCTS", "BULK_IMPORT"), LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/v1/roles")
    class FindAll {

        @Test
        @WithMockUser(authorities = "CREATE_ROLES")
        @DisplayName("returns list of roles")
        void findAll_returnsRoles() throws Exception {
            when(roleService.findAll()).thenReturn(List.of(sampleRole()));

            mockMvc.perform(get("/api/v1/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("ADMIN"));
        }

        @Test
        @DisplayName("unauthenticated returns 401/403")
        void findAll_unauthenticated_returns4xx() throws Exception {
            mockMvc.perform(get("/api/v1/roles"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/roles/{id}")
    class FindById {

        @Test
        @WithMockUser(authorities = "CREATE_ROLES")
        @DisplayName("returns role by id")
        void findById_returnsRole() throws Exception {
            when(roleService.findById(1L)).thenReturn(sampleRole());

            mockMvc.perform(get("/api/v1/roles/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("ADMIN"));
        }

        @Test
        @WithMockUser(authorities = "CREATE_ROLES")
        @DisplayName("non-existing role returns 404")
        void findById_notFound_returns404() throws Exception {
            when(roleService.findById(999L)).thenThrow(new IllegalArgumentException("Role not found"));

            mockMvc.perform(get("/api/v1/roles/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Role not found"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/roles")
    class Create {

        @Test
        @WithMockUser(authorities = "CREATE_ROLES")
        @DisplayName("creates role and returns 201")
        void create_returnsCreated() throws Exception {
            RoleRequest request = new RoleRequest("EDITOR", Set.of("MODIFY_PRODUCTS"));
            when(roleService.create(any())).thenReturn(sampleRole());

            mockMvc.perform(post("/api/v1/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(authorities = "CREATE_ROLES")
        @DisplayName("duplicate role returns 400")
        void create_duplicate_returns400() throws Exception {
            RoleRequest request = new RoleRequest("ADMIN", Set.of("MODIFY_PRODUCTS"));
            when(roleService.create(any())).thenThrow(new IllegalArgumentException("Role already exists"));

            mockMvc.perform(post("/api/v1/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Role already exists"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/roles/{id}")
    class Update {

        @Test
        @WithMockUser(authorities = "MODIFY_ROLES")
        @DisplayName("updates role and returns 200")
        void update_returnsOk() throws Exception {
            RoleRequest request = new RoleRequest("EDITOR", Set.of("MODIFY_PRODUCTS"));
            when(roleService.update(eq(1L), any())).thenReturn(sampleRole());

            mockMvc.perform(put("/api/v1/roles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "MODIFY_ROLES")
        @DisplayName("update non-existing role returns 400")
        void update_notFound_returns400() throws Exception {
            RoleRequest request = new RoleRequest("EDITOR", Set.of("MODIFY_PRODUCTS"));
            when(roleService.update(eq(999L), any())).thenThrow(new IllegalArgumentException("Role not found"));

            mockMvc.perform(put("/api/v1/roles/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/roles/{id}")
    class Delete {

        @Test
        @WithMockUser(authorities = "DELETE_ROLES")
        @DisplayName("deletes role and returns 204")
        void delete_returnsNoContent() throws Exception {
            doNothing().when(roleService).delete(1L);

            mockMvc.perform(delete("/api/v1/roles/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(authorities = "DELETE_ROLES")
        @DisplayName("delete non-existing role returns 400")
        void delete_notFound_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Role not found")).when(roleService).delete(999L);

            mockMvc.perform(delete("/api/v1/roles/999"))
                    .andExpect(status().isBadRequest());
        }
    }
}
