package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.dealer.SkuMappingService;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для {@link AdminSkuMappingController}.
 */
@Import(SecurityConfig.class)
@WebMvcTest(AdminSkuMappingController.class)
class AdminSkuMappingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SkuMappingService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private MockMultipartFile xlsxFile() {
        return new MockMultipartFile("file", "f.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1});
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET / -- список маппингов")
    void list() throws Exception {
        UUID dealerId = UUID.randomUUID();
        when(service.list(dealerId)).thenReturn(List.of(
            new SkuMappingDto("WPW-1", "D-1", "BX")));

        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/sku-mapping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].wpwSku").value("WPW-1"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("PUT / -- upsert маппинга")
    void upsert() throws Exception {
        UUID dealerId = UUID.randomUUID();
        SkuMappingDto dto = new SkuMappingDto("WPW-1", "D-1", "BX");
        when(service.upsert(eq(dealerId), any(SkuMappingDto.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/dealers/" + dealerId + "/sku-mapping")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dealerSku").value("D-1"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("DELETE /{wpwSku} -- удаляет, 204")
    void delete204() throws Exception {
        UUID dealerId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/dealers/" + dealerId + "/sku-mapping/WPW-1"))
            .andExpect(status().isNoContent());

        verify(service).delete(dealerId, "WPW-1");
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("POST /validate -- валидация без записи")
    void validate() throws Exception {
        UUID dealerId = UUID.randomUUID();
        SkuMappingService.ValidationReport report = new SkuMappingService.ValidationReport(
            2, List.of(new SkuMappingDto("WPW-1", "D-1", null)),
            List.of(), List.of());
        when(service.validate(any())).thenReturn(report);

        mockMvc.perform(multipart("/api/v1/admin/dealers/" + dealerId + "/sku-mapping/validate")
                .file(xlsxFile()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("POST /execute -- импорт SKU")
    void execute() throws Exception {
        UUID dealerId = UUID.randomUUID();
        SkuMappingService.SkuMappingImportResult result = new SkuMappingService.SkuMappingImportResult(
            2, 1, 1, 0, List.of());
        when(service.execute(eq(dealerId), any(), anyBoolean())).thenReturn(result);

        mockMvc.perform(multipart("/api/v1/admin/dealers/" + dealerId + "/sku-mapping/execute")
                .file(xlsxFile())
                .param("skipGhosts", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(2))
            .andExpect(jsonPath("$.created").value(1));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /export -- xlsx")
    void export() throws Exception {
        UUID dealerId = UUID.randomUUID();
        when(service.export(dealerId)).thenReturn(new byte[]{0x50, 0x4b});

        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/sku-mapping/export"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"sku-mapping-" + dealerId + ".xlsx\""));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /template -- xlsx шаблон")
    void template() throws Exception {
        when(service.template()).thenReturn(new byte[]{0x50, 0x4b});

        UUID dealerId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/sku-mapping/template"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"sku-mapping-template.xlsx\""));
    }

    @Test
    @DisplayName("без авторизации — 4xx")
    void unauthenticated() throws Exception {
        UUID dealerId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/sku-mapping"))
            .andExpect(status().is4xxClientError());
    }
}
