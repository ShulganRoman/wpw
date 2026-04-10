package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.search.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SearchService searchService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Test
    @DisplayName("GET /api/v1/search -- returns paged search results")
    void search_returnsPagedResults() throws Exception {
        Map<String, Object> item = Map.of("toolNo", "WPW-001", "name", "Router Bit");
        when(searchService.search("router", "en", 1, 20)).thenReturn(List.of(item));
        when(searchService.countSearch("router", "en")).thenReturn(1L);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "router"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].toolNo").value("WPW-001"));
    }

    @Test
    @DisplayName("GET /api/v1/search -- empty results")
    void search_noResults_returnsEmptyPage() throws Exception {
        when(searchService.search("xyz", "en", 1, 20)).thenReturn(List.of());
        when(searchService.countSearch("xyz", "en")).thenReturn(0L);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/search -- custom locale and pagination")
    void search_customLocaleAndPagination() throws Exception {
        when(searchService.search("bit", "ru", 2, 10)).thenReturn(List.of());
        when(searchService.countSearch("bit", "ru")).thenReturn(15L);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "bit")
                        .param("locale", "ru")
                        .param("page", "2")
                        .param("perPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.perPage").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/search -- missing q param returns 5xx (server error)")
    void search_missingQuery_returnsError() throws Exception {
        mockMvc.perform(get("/api/v1/search"))
                .andExpect(status().is5xxServerError());
    }
}
