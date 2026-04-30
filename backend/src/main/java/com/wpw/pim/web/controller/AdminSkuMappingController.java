package com.wpw.pim.web.controller;

import com.wpw.pim.service.dealer.SkuMappingService;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dealers/{dealerId}/sku-mapping")
@PreAuthorize("hasAuthority('MANAGE_DEALERS')")
@RequiredArgsConstructor
@Tag(name = "Admin: SKU Mapping", description = "Управление маппингом артикулов дилера (WPW SKU → дилерский SKU). Требует MANAGE_DEALERS.")
public class AdminSkuMappingController {

    private final SkuMappingService service;

    @GetMapping
    @Operation(summary = "Список маппингов SKU дилера")
    public List<SkuMappingDto> list(@PathVariable UUID dealerId) {
        return service.list(dealerId);
    }

    @PutMapping
    @Operation(summary = "Создать или обновить маппинг SKU")
    public SkuMappingDto upsert(@PathVariable UUID dealerId, @RequestBody SkuMappingDto req) {
        return service.upsert(dealerId, req);
    }

    @DeleteMapping("/{wpwSku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить маппинг SKU")
    public void delete(@PathVariable UUID dealerId, @PathVariable String wpwSku) {
        service.delete(dealerId, wpwSku);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Валидация файла маппинга SKU", description = "Проверяет Excel без записи в БД.")
    public SkuMappingService.ValidationReport validate(
        @PathVariable UUID dealerId,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        return service.validate(file);
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Импортировать маппинг SKU из Excel", description = "skipGhosts=true — пропускать строки с несуществующими WPW SKU.")
    public SkuMappingService.SkuMappingImportResult execute(
        @PathVariable UUID dealerId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "false") boolean skipGhosts
    ) throws IOException {
        return service.execute(dealerId, file, skipGhosts);
    }

    @GetMapping("/export")
    @Operation(summary = "Экспортировать маппинг SKU в Excel")
    public ResponseEntity<byte[]> export(@PathVariable UUID dealerId) throws IOException {
        byte[] bytes = service.export(dealerId);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sku-mapping-" + dealerId + ".xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @GetMapping("/template")
    @Operation(summary = "Скачать шаблон маппинга SKU")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] bytes = service.template();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sku-mapping-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }
}
