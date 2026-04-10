package com.wpw.pim.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.dto.UserRequest;
import com.wpw.pim.auth.dto.UserResponse;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.auth.service.UserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private UserResponse sampleUser() {
        return new UserResponse(1L, "admin", 1L, "ADMIN", true, LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class FindAll {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns list of users for admin")
        void findAll_returnsUsers() throws Exception {
            when(userService.findAll()).thenReturn(List.of(sampleUser()));

            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].username").value("admin"));
        }

        @Test
        @DisplayName("unauthenticated returns 401/403")
        void findAll_unauthenticated_returns4xx() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("non-admin returns 403")
        void findAll_nonAdmin_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users")
    class Create {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("creates user and returns 201")
        void create_returnsCreated() throws Exception {
            UserRequest request = new UserRequest("newuser", "password", 1L, true);
            when(userService.create(any())).thenReturn(sampleUser());

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("admin"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("duplicate username returns 400")
        void create_duplicate_returns400() throws Exception {
            UserRequest request = new UserRequest("admin", "password", 1L, true);
            when(userService.create(any())).thenThrow(new IllegalArgumentException("Username already exists"));

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Username already exists"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
    class Update {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("updates user and returns 200")
        void update_returnsOk() throws Exception {
            UserRequest request = new UserRequest("admin", "newpass", 1L, true);
            when(userService.update(eq(1L), any())).thenReturn(sampleUser());

            mockMvc.perform(put("/api/v1/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("admin"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("update non-existing user returns 400")
        void update_notFound_returns400() throws Exception {
            UserRequest request = new UserRequest("admin", "newpass", 1L, true);
            when(userService.update(eq(999L), any())).thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(put("/api/v1/users/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class Delete {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deletes user and returns 204")
        void delete_returnsNoContent() throws Exception {
            doNothing().when(userService).delete(1L);

            mockMvc.perform(delete("/api/v1/users/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("delete non-existing user returns 400")
        void delete_notFound_returns400() throws Exception {
            doThrow(new IllegalArgumentException("User not found")).when(userService).delete(999L);

            mockMvc.perform(delete("/api/v1/users/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }
}
