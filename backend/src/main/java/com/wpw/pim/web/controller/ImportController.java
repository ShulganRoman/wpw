package com.wpw.pim.web.controller;

import com.wpw.pim.service.excel.ExcelImportService;
import com.wpw.pim.service.excel.ExcelImportV4Service;
import com.wpw.pim.service.excel.ExcelTemplateGenerator;
import com.wpw.pim.service.excel.ExcelTemplateV4Generator;
import com.wpw.pim.service.excel.WpwCatalogImportService;
import com.wpw.pim.service.excel.dto.ValidationReport;
import io.swagger.v3.oas.annotations.Operation;
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
 * Эндпоинты массового импорта из Excel.
 *
 * Рекомендуемый порядок работы:
 *  1. POST /validate — загрузить файл и получить ValidationReport с ошибками/предупреждениями
 *  2. Исправить данные в Excel (если есть ошибки)
 *  3. POST /execute — запустить импорт, получить MD-отчёт со статистикой
 */
@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "Массовый импорт данных из Excel")
public class ImportController {

    private final ExcelImportService      importService;
    private final ExcelImportV4Service    importV4Service;
    private final ExcelTemplateGenerator  templateGenerator;
    private final ExcelTemplateV4Generator templateV4Generator;
    private final WpwCatalogImportService wpwCatalogImportService;

    @GetMapping(value = "/template",
                produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download import template",
               description = "Returns an .xlsx template with column headers, example data, and instructions.")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        byte[] bytes = templateGenerator.generate();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"wpw-pim-import-template.xlsx\"")
            .body(bytes);
    }

    /**
     * Шаг 1: Предимпортная валидация.
     * Файл разбирается и проверяется без записи в БД.
     * Возвращает список ошибок/предупреждений с указанием листа, строки и поля.
     */
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Валидация Excel-файла перед импортом",
               description = "Возвращает ValidationReport: список ошибок (ERROR — строка пропускается) "
                           + "и предупреждений (WARNING — строка импортируется, но требует внимания). "
                           + "Поле canProceed=true означает что ошибок нет и можно запускать /execute.")
    public ResponseEntity<ValidationReport> validate(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        ValidationReport report = importService.validate(file);
        return ResponseEntity.ok(report);
    }

    /**
     * Шаг 2: Выполнение импорта.
     * Возвращает Markdown-отчёт со статистикой.
     */
    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "Выполнить импорт из Excel",
               description = "Импортирует товары и группы. "
                           + "Возвращает Markdown-отчёт: сколько создано, обновлено, пропущено, "
                           + "и список всех ошибок выполнения.")
    public ResponseEntity<String> execute(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        String report = importService.execute(file);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
            .body(report);
    }

    // -------------------------------------------------------------------------
    // WPW Catalog v3 format (single sheet, no SEO columns)
    // -------------------------------------------------------------------------

    @PostMapping(value = "/wpw-catalog/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Валидация файла формата WPW Catalog v3",
               description = "Принимает файл WPW_Catalog_v3.xlsx (лист Sheet1, без SEO-колонок). "
                           + "Возвращает ValidationReport без записи в БД.")
    public ResponseEntity<ValidationReport> validateWpwCatalog(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        ValidationReport report = wpwCatalogImportService.validate(file);
        return ResponseEntity.ok(report);
    }

    @PostMapping(value = "/wpw-catalog/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "Выполнить импорт из WPW Catalog v3",
               description = "Импортирует товары из файла WPW_Catalog_v3.xlsx. "
                           + "Возвращает Markdown-отчёт со статистикой.")
    public ResponseEntity<String> executeWpwCatalog(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        String report = wpwCatalogImportService.execute(file);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
            .body(report);
    }

    // -------------------------------------------------------------------------
    // PIM v4 format (single sheet, no Group ID)
    // -------------------------------------------------------------------------

    @GetMapping(value = "/v4/template",
                produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download v4 import template",
               description = "Returns an .xlsx template for v4 format — single Products sheet, "
                           + "groups auto-created from Category + Group Name.")
    public ResponseEntity<byte[]> downloadV4Template() throws Exception {
        byte[] bytes = templateV4Generator.generate();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"wpw-pim-import-v4.xlsx\"")
            .body(bytes);
    }

    @PostMapping(value = "/v4/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Валидация Excel-файла формата v4",
               description = "Разбирает файл v4 (один лист Products, без Group ID) и возвращает ValidationReport.")
    public ResponseEntity<ValidationReport> validateV4(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        ValidationReport report = importV4Service.validate(file);
        return ResponseEntity.ok(report);
    }

    @PostMapping(value = "/v4/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "Выполнить импорт из формата v4",
               description = "Импортирует товары из файла v4. Группы создаются автоматически "
                           + "из пар Category + Group Name. Возвращает Markdown-отчёт.")
    public ResponseEntity<String> executeV4(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        String report = importV4Service.execute(file);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
            .body(report);
    }
}
