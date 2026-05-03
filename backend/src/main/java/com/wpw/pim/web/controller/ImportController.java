package com.wpw.pim.web.controller;

import com.wpw.pim.service.excel.ExcelImportV4Service;
import com.wpw.pim.service.excel.ExcelTemplateV4Generator;
import com.wpw.pim.service.excel.dto.ValidationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Эндпоинты массового импорта из Excel (формат v4).
 *
 * Рекомендуемый порядок работы:
 *  1. GET /template — скачать шаблон .xlsx
 *  2. POST /validate — загрузить файл и получить ValidationReport
 *  3. POST /execute — выполнить импорт, получить MD-отчёт
 */
@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "Bulk data import from Excel (v4 format)")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Admin access required")
})
public class ImportController {

    private final ExcelImportV4Service     importService;
    private final ExcelTemplateV4Generator templateGenerator;

    @GetMapping(value = "/template",
                produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download import template",
               description = "Returns .xlsx v4 template: single Products sheet, groups created automatically from Category + Group Name.")
    @ApiResponse(responseCode = "200", description = "Template file (.xlsx)")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        byte[] bytes = templateGenerator.generate();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"wpw-pim-import-template.xlsx\"")
            .body(bytes);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate Excel file before import",
               description = "Parses the file and validates without writing to DB. "
                           + "Returns ValidationReport: list of errors (ERROR — row is skipped) "
                           + "and warnings (WARNING — row is imported). "
                           + "canProceed=true means no errors.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation report"),
        @ApiResponse(responseCode = "400", description = "File parse error")
    })
    public ResponseEntity<ValidationReport> validate(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        ValidationReport report = importService.validate(file);
        return ResponseEntity.ok(report);
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "Execute Excel import",
               description = "Imports products. Groups are created automatically from Category + Group Name. "
                           + "Returns a Markdown report: how many created, updated, skipped.")
    @ApiResponse(responseCode = "200", description = "Import result (Markdown)")
    public ResponseEntity<String> execute(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        String report = importService.execute(file);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
            .body(report);
    }
}
