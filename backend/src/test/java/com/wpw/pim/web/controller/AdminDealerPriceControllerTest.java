package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.pricing.DealerPriceService;
import com.wpw.pim.web.dto.pricing.DealerPriceListDto;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link AdminDealerPriceController}.
 */
@Import(SecurityConfig.class)
@WebMvcTest(AdminDealerPriceController.class)
class AdminDealerPriceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DealerPriceService dealerPriceService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private MockMultipartFile xlsxFile() {
        return new MockMultipartFile("file", "p.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1, 2, 3});
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("GET / -- returns dealer price list")
    void getPriceList() throws Exception {
        UUID dealerId = UUID.randomUUID();
        DealerPriceListDto dto = new DealerPriceListDto("USD", "$",
            LocalDate.of(2024, 1, 1), null, false, List.of());
        when(dealerPriceService.getForDealer(dealerId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/price-list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currencyCode").value("USD"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("GET / -- 204 if price list is absent")
    void getPriceList_noContent() throws Exception {
        UUID dealerId = UUID.randomUUID();
        when(dealerPriceService.getForDealer(dealerId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/price-list"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("POST /import -- price list import")
    void importPriceList() throws Exception {
        UUID dealerId = UUID.randomUUID();
        when(dealerPriceService.importPriceList(eq(dealerId), any(), eq("USD"), any()))
            .thenReturn(new PriceImportResult(5, 1, List.of("err")));

        mockMvc.perform(multipart("/api/v1/admin/dealers/" + dealerId + "/price-list/import")
                .file(xlsxFile())
                .param("currencyCode", "USD")
                .param("validTo", "2025-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(5));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("DELETE / -- deletes price list (204)")
    void deletePriceList() throws Exception {
        UUID dealerId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/dealers/" + dealerId + "/price-list"))
            .andExpect(status().isNoContent());

        verify(dealerPriceService).deletePriceList(dealerId);
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("GET /export -- returns Excel")
    void export() throws Exception {
        UUID dealerId = UUID.randomUUID();
        when(dealerPriceService.export(dealerId)).thenReturn(new byte[]{0x50, 0x4b});

        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/price-list/export"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"dealer-price-list.xlsx\""));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("GET /template -- returns Excel template")
    void template() throws Exception {
        when(dealerPriceService.template()).thenReturn(new byte[]{0x50, 0x4b});

        UUID dealerId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/price-list/template"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"price-list-template.xlsx\""));
    }

    @Test
    @DisplayName("without authentication returns 4xx")
    void unauthenticated() throws Exception {
        UUID dealerId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/price-list"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("without MANAGE_PRICES returns 403")
    void wrongAuthority() throws Exception {
        UUID dealerId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/price-list"))
            .andExpect(status().isForbidden());
    }
}
