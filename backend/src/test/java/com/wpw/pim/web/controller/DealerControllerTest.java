package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.dealer.DealerService;
import com.wpw.pim.web.dto.dealer.PriceListDto;
import com.wpw.pim.web.dto.dealer.SkuMappingCreateRequest;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(DealerController.class)
class DealerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private DealerService dealerService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private DealerPrincipal dealerPrincipal() {
        Dealer dealer = new Dealer();
        dealer.setId(UUID.randomUUID());
        dealer.setName("TestDealer");
        dealer.setApiKeyHash("$2a$10$hash");
        dealer.setActive(true);
        return new DealerPrincipal(dealer);
    }

    @Test
    @DisplayName("GET /api/v1/dealer/sku-mapping -- returns SKU mappings for dealer")
    void getSkuMapping_returnsList() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        UUID productId = UUID.randomUUID();
        SkuMappingDto dto = new SkuMappingDto(productId, "WPW-001", "DEALER-SKU-1");
        when(dealerService.getSkuMapping(principal.getDealer().getId())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/dealer/sku-mapping")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].toolNo").value("WPW-001"))
                .andExpect(jsonPath("$[0].dealerSku").value("DEALER-SKU-1"));
    }

    @Test
    @DisplayName("POST /api/v1/dealer/sku-mapping -- creates new SKU mapping")
    void addSkuMapping_returnsCreatedMapping() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        UUID productId = UUID.randomUUID();
        SkuMappingCreateRequest request = new SkuMappingCreateRequest(productId, "MY-SKU");
        SkuMappingDto result = new SkuMappingDto(productId, "WPW-001", "MY-SKU");

        when(dealerService.saveSkuMapping(eq(principal.getDealer().getId()), any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/dealer/sku-mapping")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealerSku").value("MY-SKU"));
    }

    @Test
    @DisplayName("GET /api/v1/dealer/price-list -- returns price list for dealer")
    void getPriceList_returnsPriceList() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        PriceListDto priceList = new PriceListDto(
                UUID.randomUUID(), "Standard", "EUR", "\u20ac",
                List.of(new PriceListDto.PriceItemDto(UUID.randomUUID(), "WPW-001", BigDecimal.valueOf(25.50), 1))
        );
        when(dealerService.getPriceList(any(Dealer.class))).thenReturn(priceList);

        mockMvc.perform(get("/api/v1/dealer/price-list")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Standard"))
                .andExpect(jsonPath("$.items[0].toolNo").value("WPW-001"));
    }

    @Test
    @DisplayName("GET /api/v1/dealer/sku-mapping -- unauthenticated returns 401/403")
    void getSkuMapping_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/dealer/sku-mapping"))
                .andExpect(status().is4xxClientError());
    }
}
