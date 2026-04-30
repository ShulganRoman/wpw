package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.repository.pricing.CurrencyRepository;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.pricing.StockPriceService;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import com.wpw.pim.web.dto.pricing.PriceListItemDto;
import com.wpw.pim.web.dto.pricing.PriceListItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link AdminStockPriceController}.
 * Эндпоинты требуют MANAGE_PRICES.
 */
@Import(SecurityConfig.class)
@WebMvcTest(AdminStockPriceController.class)
class AdminStockPriceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private StockPriceService stockPriceService;
    @MockitoBean private CurrencyRepository currencyRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private Currency usd() {
        Currency c = new Currency();
        c.setCode("USD");
        c.setSymbol("$");
        c.setActive(true);
        return c;
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("GET /currencies -- возвращает активные валюты")
    void getCurrencies() throws Exception {
        when(currencyRepository.findByIsActiveTrueOrderByCode()).thenReturn(List.of(usd()));

        mockMvc.perform(get("/api/v1/admin/price/stock/currencies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("USD"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("GET / -- возвращает позиции прайс-листа")
    void list() throws Exception {
        PriceListItemDto dto = new PriceListItemDto("TOOL-1", 1, BigDecimal.valueOf(10));
        when(stockPriceService.getItems()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/admin/price/stock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].toolNo").value("TOOL-1"))
            .andExpect(jsonPath("$[0].minQty").value(1));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("PUT / -- создаёт/обновляет позицию")
    void upsert() throws Exception {
        PriceListItemRequest req = new PriceListItemRequest("TOOL-1", 1, BigDecimal.valueOf(20));
        PriceListItemDto dto = new PriceListItemDto("TOOL-1", 1, BigDecimal.valueOf(20));
        when(stockPriceService.upsertItem(any(PriceListItemRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/price/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.toolNo").value("TOOL-1"))
            .andExpect(jsonPath("$.price").value(20));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("DELETE /{toolNo}/{minQty} -- удаляет позицию (204)")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/price/stock/TOOL-1/1"))
            .andExpect(status().isNoContent());

        verify(stockPriceService).deleteItem("TOOL-1", 1);
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("POST /import -- импортирует Excel и возвращает отчёт")
    void importExcel() throws Exception {
        when(stockPriceService.importExcel(any())).thenReturn(new PriceImportResult(3, 1, List.of("e")));

        MockMultipartFile file = new MockMultipartFile("file", "p.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/admin/price/stock/import").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(3))
            .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("GET /export -- возвращает Excel с правильным Content-Disposition")
    void export() throws Exception {
        when(stockPriceService.export()).thenReturn(new byte[]{0x50, 0x4b});

        mockMvc.perform(get("/api/v1/admin/price/stock/export"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"stock-prices.xlsx\""));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_PRICES")
    @DisplayName("GET /template -- возвращает Excel шаблон")
    void template() throws Exception {
        when(stockPriceService.template()).thenReturn(new byte[]{0x50, 0x4b});

        mockMvc.perform(get("/api/v1/admin/price/stock/template"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"stock-prices-template.xlsx\""));
    }

    @Test
    @DisplayName("без аутентификации возвращает 4xx")
    void unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/admin/price/stock"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("без MANAGE_PRICES возвращает 403")
    void wrongAuthority_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/price/stock"))
            .andExpect(status().isForbidden());
    }
}
