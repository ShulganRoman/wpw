package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link SpaFallbackController}.
 * Контроллер отдаёт index.html для SPA маршрутов, чтобы React Router мог их обработать.
 */
@Import(SecurityConfig.class)
@WebMvcTest(SpaFallbackController.class)
class SpaFallbackControllerTest {

    @Autowired private MockMvc mockMvc;

    // Security beans нужны для @Import(SecurityConfig.class)
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Test
    @WithMockUser
    @DisplayName("GET /catalog -- serves index.html")
    void catalog() throws Exception {
        mockMvc.perform(get("/catalog").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /catalog/some/nested -- serves index.html")
    void catalogNested() throws Exception {
        mockMvc.perform(get("/catalog/section/sub").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /product/{slug} -- serves index.html")
    void product() throws Exception {
        mockMvc.perform(get("/product/some-tool").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /export -- serves index.html")
    void export() throws Exception {
        mockMvc.perform(get("/export").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /admin -- serves index.html")
    void admin() throws Exception {
        mockMvc.perform(get("/admin").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /login -- serves index.html")
    void login() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
