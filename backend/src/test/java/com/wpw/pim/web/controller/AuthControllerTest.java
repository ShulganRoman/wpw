package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.controller.AuthController;
import com.wpw.pim.auth.dto.LoginRequest;
import com.wpw.pim.auth.dto.LoginResponse;
import com.wpw.pim.auth.service.AuthService;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link AuthController}.
 * Проверяют HTTP-контракт аутентификации: login, logout.
 */
@Import(SecurityConfig.class)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Test
    @DisplayName("POST /api/v1/auth/login -- successful authentication returns 200 with token")
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginResponse response = new LoginResponse("jwt-token", "admin", "ADMIN",
                Set.of("MODIFY_PRODUCTS", "BULK_IMPORT"));

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.privileges").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login -- invalid credentials return 401")
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login -- empty username returns 400")
    void login_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"pass\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login -- empty password returns 400")
    void login_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout -- returns 200 with message")
    void logout_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Logged out successfully"));
    }
}
