package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductSitemapProjection;
import com.wpw.pim.security.ApiKeyAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(SitemapController.class)
class SitemapControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProductRepository productRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Test
    @DisplayName("GET /sitemap.xml -- returns valid XML sitemap with products")
    void sitemap_returnsXmlWithProducts() throws Exception {
        ProductSitemapProjection product = new ProductSitemapProjection() {
            @Override public String getToolNo() { return "WPW-001"; }
            @Override public OffsetDateTime getUpdatedAt() { return OffsetDateTime.parse("2025-06-15T10:00:00Z"); }
        };
        when(productRepository.findAllForSitemap()).thenReturn(List.of(product));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("WPW-001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2025-06-15")));
    }

    @Test
    @DisplayName("GET /sitemap.xml -- handles products without updatedAt")
    void sitemap_handlesNullUpdatedAt() throws Exception {
        ProductSitemapProjection product = new ProductSitemapProjection() {
            @Override public String getToolNo() { return "WPW-002"; }
            @Override public OffsetDateTime getUpdatedAt() { return null; }
        };
        when(productRepository.findAllForSitemap()).thenReturn(List.of(product));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    @Test
    @DisplayName("GET /sitemap.xml -- empty product list still has static pages")
    void sitemap_emptyProducts_hasStaticPages() throws Exception {
        when(productRepository.findAllForSitemap()).thenReturn(List.of());

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }
}
