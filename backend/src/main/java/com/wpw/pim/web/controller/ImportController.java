package com.wpw.pim.web.controller;

import com.wpw.pim.service.excel.ExcelImportV4Service;
import com.wpw.pim.service.excel.ExcelTemplateV4Generator;
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
@Tag(name = "Import", description = "Массовый импорт данных из Excel (формат v4)")
public class ImportController {

    private final ExcelImportV4Service     importService;
    private final ExcelTemplateV4Generator templateGenerator;

    @GetMapping(value = "/template",
                produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Скачать шаблон импорта",
               description = "Возвращает .xlsx шаблон v4: один лист Products, группы создаются автоматически из Category + Group Name.")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        byte[] bytes = templateGenerator.generate();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"wpw-pim-import-template.xlsx\"")
            .body(bytes);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Валидация Excel-файла перед импортом",
               description = "Разбирает файл и проверяет без записи в БД. "
                           + "Возвращает ValidationReport: список ошибок (ERROR — строка пропускается) "
                           + "и предупреждений (WARNING — строка импортируется). "
                           + "canProceed=true означает что ошибок нет.")
    public ResponseEntity<ValidationReport> validate(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        ValidationReport report = importService.validate(file);
        return ResponseEntity.ok(report);
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "Выполнить импорт из Excel",
               description = "Импортирует товары. Группы создаются автоматически из Category + Group Name. "
                           + "Возвращает Markdown-отчёт: сколько создано, обновлено, пропущено.")
    public ResponseEntity<String> execute(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        String report = importService.execute(file);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
            .body(report);
    }
}
