package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.export.ExportService;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.product.ProductFilter;
import com.wpw.pim.web.dto.product.ProductSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(ExportController.class)
class ExportControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ExportService exportService;
    @MockitoBean private ProductService productService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    @Test
    @DisplayName("GET /api/v1/export/preview -- returns paged product preview")
    void preview_returnsPagedResponse() throws Exception {
        PagedResponse<ProductSummaryDto> response = PagedResponse.of(List.of(), 0, 1, 20);
        when(productService.findAll(any(ProductFilter.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/export/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/export -- CSV format returns text/csv")
    void export_csv_returnsTextCsv() throws Exception {
        byte[] csvData = "toolNo,name\nWPW-001,Bit".getBytes();
        when(exportService.export(eq("csv"), eq("en"), any(ProductFilter.class))).thenReturn(csvData);

        mockMvc.perform(get("/api/v1/export")
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=wpw-products.csv"))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @DisplayName("GET /api/v1/export -- XLSX format returns spreadsheet content type")
    void export_xlsx_returnsSpreadsheetContentType() throws Exception {
        byte[] xlsxData = new byte[]{0x50, 0x4B};
        when(exportService.export(eq("xlsx"), eq("en"), any(ProductFilter.class))).thenReturn(xlsxData);

        mockMvc.perform(get("/api/v1/export")
                        .param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=wpw-products.xlsx"))
                .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @DisplayName("GET /api/v1/export -- XML format returns application/xml")
    void export_xml_returnsApplicationXml() throws Exception {
        byte[] xmlData = "<products/>".getBytes();
        when(exportService.export(eq("xml"), eq("en"), any(ProductFilter.class))).thenReturn(xmlData);

        mockMvc.perform(get("/api/v1/export")
                        .param("format", "xml"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=wpw-products.xml"))
                .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    @Test
    @DisplayName("GET /api/v1/export -- default format is csv")
    void export_defaultFormat_isCsv() throws Exception {
        byte[] csvData = "data".getBytes();
        when(exportService.export(eq("csv"), eq("en"), any(ProductFilter.class))).thenReturn(csvData);

        mockMvc.perform(get("/api/v1/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=wpw-products.csv"));
    }
}
