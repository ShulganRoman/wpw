package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.excel.ExcelImportService;
import com.wpw.pim.service.excel.ExcelImportV4Service;
import com.wpw.pim.service.excel.ExcelTemplateGenerator;
import com.wpw.pim.service.excel.ExcelTemplateV4Generator;
import com.wpw.pim.service.excel.WpwCatalogImportService;
import com.wpw.pim.service.excel.dto.ValidationReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(ImportController.class)
class ImportControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ExcelImportService importService;
    @MockitoBean private ExcelImportV4Service importV4Service;
    @MockitoBean private ExcelTemplateGenerator templateGenerator;
    @MockitoBean private ExcelTemplateV4Generator templateV4Generator;
    @MockitoBean private WpwCatalogImportService wpwCatalogImportService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private MockMultipartFile excelFile() {
        return new MockMultipartFile("file", "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
    }

    @Nested
    @DisplayName("Standard Excel format")
    class StandardFormat {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/import/validate -- returns validation report")
        void validate_returnsReport() throws Exception {
            ValidationReport report = ValidationReport.builder()
                    .totalProductRows(10)
                    .totalGroupRows(2)
                    .errorCount(0)
                    .warningCount(1)
                    .canProceed(true)
                    .issues(List.of())
                    .unknownHeaders(List.of())
                    .build();
            when(importService.validate(any())).thenReturn(report);

            mockMvc.perform(multipart("/api/v1/admin/import/validate")
                            .file(excelFile()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProductRows").value(10))
                    .andExpect(jsonPath("$.canProceed").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/import/execute -- returns markdown report")
        void execute_returnsMarkdown() throws Exception {
            when(importService.execute(any())).thenReturn("# Import Report\n- Created: 5");

            mockMvc.perform(multipart("/api/v1/admin/import/execute")
                            .file(excelFile()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("# Import Report\n- Created: 5"));
        }
    }

    @Nested
    @DisplayName("WPW Catalog v3 format")
    class WpwFormat {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/import/wpw-catalog/validate -- returns validation report")
        void validateWpw_returnsReport() throws Exception {
            ValidationReport report = ValidationReport.builder()
                    .totalProductRows(50)
                    .totalGroupRows(0)
                    .errorCount(2)
                    .warningCount(3)
                    .canProceed(false)
                    .issues(List.of())
                    .unknownHeaders(List.of("UnknownCol"))
                    .build();
            when(wpwCatalogImportService.validate(any())).thenReturn(report);

            mockMvc.perform(multipart("/api/v1/admin/import/wpw-catalog/validate")
                            .file(excelFile()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProductRows").value(50))
                    .andExpect(jsonPath("$.canProceed").value(false))
                    .andExpect(jsonPath("$.errorCount").value(2));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/v1/admin/import/wpw-catalog/execute -- returns markdown report")
        void executeWpw_returnsMarkdown() throws Exception {
            when(wpwCatalogImportService.execute(any())).thenReturn("# WPW Import\n- Done");

            mockMvc.perform(multipart("/api/v1/admin/import/wpw-catalog/execute")
                            .file(excelFile()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("# WPW Import\n- Done"));
        }
    }

    @Test
    @DisplayName("POST /api/v1/admin/import/validate -- unauthenticated returns 401/403")
    void validate_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/import/validate")
                        .file(excelFile()))
                .andExpect(status().is4xxClientError());
    }
}
