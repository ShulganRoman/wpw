package com.wpw.pim.web.controller;

import com.wpw.pim.service.excel.ExcelImportService;
import com.wpw.pim.service.excel.WpwCatalogImportService;
import com.wpw.pim.service.excel.dto.ValidationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private final ExcelImportService    importService;
    private final WpwCatalogImportService wpwCatalogImportService;

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
}
