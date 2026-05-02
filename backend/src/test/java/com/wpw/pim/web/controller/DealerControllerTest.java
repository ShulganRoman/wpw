package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.service.dealer.DealerService;
import com.wpw.pim.service.dealer.SkuMappingService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(DealerController.class)
class DealerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private DealerService dealerService;
    @MockitoBean private SkuMappingService skuMappingService;
    @MockitoBean private DealerRepository dealerRepository;
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
        SkuMappingDto dto = new SkuMappingDto("WPW-001", "DEALER-SKU-1", null);
        when(dealerService.getSkuMapping(principal.getDealer().getId())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/dealer/sku-mapping")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wpwSku").value("WPW-001"))
                .andExpect(jsonPath("$[0].dealerSku").value("DEALER-SKU-1"));
    }

    @Test
    @DisplayName("POST /api/v1/dealer/sku-mapping -- creates new SKU mapping")
    void addSkuMapping_returnsCreatedMapping() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        SkuMappingCreateRequest request = new SkuMappingCreateRequest("WPW-001", "MY-SKU", "BrandX");
        SkuMappingDto result = new SkuMappingDto("WPW-001", "MY-SKU", "BrandX");

        when(dealerService.saveSkuMapping(eq(principal.getDealer().getId()), any(), any())).thenReturn(result);

        mockMvc.perform(put("/api/v1/dealer/sku-mapping")
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
                UUID.randomUUID(), "Standard", "EUR", "€",
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

    @Test
    @DisplayName("DELETE /api/v1/dealer/sku-mapping/{wpwSku} -- 204")
    void deleteSkuMapping_returns204() throws Exception {
        DealerPrincipal principal = dealerPrincipal();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/dealer/sku-mapping/WPW-001")
                        .with(user(principal)))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(dealerService).deleteSkuMapping(principal.getDealer().getId(), "WPW-001");
    }

    @Test
    @DisplayName("POST /api/v1/dealer/sku-mapping/validate -- returns validation report")
    void validate_returnsReport() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        SkuMappingService.ValidationReport report = new SkuMappingService.ValidationReport(
                10, java.util.List.of(), java.util.List.of(), java.util.List.of());
        when(skuMappingService.validate(any())).thenReturn(report);

        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "mapping.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[]{1, 2, 3});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/dealer/sku-mapping/validate")
                        .file(file)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/dealer/sku-mapping/execute -- imports SKU mapping")
    void execute_importsSkuMapping() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        SkuMappingService.SkuMappingImportResult result = new SkuMappingService.SkuMappingImportResult(
                10, 8, 2, 0, java.util.List.of());
        when(skuMappingService.execute(eq(principal.getDealer().getId()), any(), eq(false))).thenReturn(result);

        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "mapping.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[]{1, 2, 3});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/dealer/sku-mapping/execute")
                        .file(file)
                        .with(user(principal)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST execute with skipGhosts=true is passed to service")
    void execute_withSkipGhosts_passesParam() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        SkuMappingService.SkuMappingImportResult result = new SkuMappingService.SkuMappingImportResult(
                5, 5, 0, 0, java.util.List.of());
        when(skuMappingService.execute(eq(principal.getDealer().getId()), any(), eq(true))).thenReturn(result);

        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "m.xlsx", "application/octet-stream", new byte[]{1});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/dealer/sku-mapping/execute")
                        .file(file)
                        .param("skipGhosts", "true")
                        .with(user(principal)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/dealer/sku-mapping/export -- returns xlsx bytes")
    void export_returnsXlsx() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        byte[] xlsx = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(skuMappingService.export(principal.getDealer().getId())).thenReturn(xlsx);

        mockMvc.perform(get("/api/v1/dealer/sku-mapping/export").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"my-sku-mapping.xlsx\""));
    }

    @Test
    @DisplayName("GET /api/v1/dealer/sku-mapping/template -- returns template xlsx")
    void template_returnsXlsx() throws Exception {
        DealerPrincipal principal = dealerPrincipal();
        byte[] xlsx = new byte[]{0x50, 0x4B};
        when(skuMappingService.template()).thenReturn(xlsx);

        mockMvc.perform(get("/api/v1/dealer/sku-mapping/template").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"sku-mapping-template.xlsx\""));
    }

    @Test
    @DisplayName("Non-DealerPrincipal user — DealerRepository is called")
    void resolveDealer_nonDealerPrincipal_usesRepository() throws Exception {
        Dealer dealer = new Dealer();
        dealer.setId(UUID.randomUUID());
        dealer.setName("Lookup");
        dealer.setApiKeyHash("$2a$hash");
        dealer.setActive(true);

        when(dealerRepository.findByUserUsername("john"))
                .thenReturn(java.util.Optional.of(dealer));
        when(dealerService.getSkuMapping(dealer.getId())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/dealer/sku-mapping")
                        .with(user("john").roles("DEALER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Non-DealerPrincipal user without Dealer record — 404")
    void resolveDealer_dealerProfileNotFound_returns404() throws Exception {
        when(dealerRepository.findByUserUsername("ghost"))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/dealer/sku-mapping")
                        .with(user("ghost").roles("DEALER")))
                .andExpect(status().isNotFound());
    }
}
