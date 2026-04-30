package com.wpw.pim.web.controller;

import com.wpw.pim.service.operation.OperationService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.operation.ApplicationTagUpsertDto;
import com.wpw.pim.web.dto.operation.OperationDto;
import com.wpw.pim.web.dto.product.ProductSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
@Tag(name = "Operations", description = "Операции обработки (теги применения): список, товары по операции, CRUD для администратора")
public class OperationController {

    private final OperationService operationService;

    @GetMapping
    @Operation(summary = "Список операций", description = "Все активные операции обработки.")
    public List<OperationDto> list() {
        return operationService.findAll().stream()
            .map(OperationDto::from)
            .toList();
    }

    @GetMapping("/{code}/products")
    @Operation(summary = "Товары по операции", description = "Список товаров, привязанных к операции обработки.")
    public PagedResponse<ProductSummaryDto> productsByOperation(
        @PathVariable String code,
        @RequestParam(defaultValue = "en") String locale,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "48") int perPage
    ) {
        return operationService.findProductsByOperation(code, locale, page, perPage);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('MANAGE_CATALOG')")
    @PostMapping
    @Operation(summary = "Создать операцию", description = "Требует MANAGE_CATALOG.")
    public ResponseEntity<OperationDto> create(@RequestBody ApplicationTagUpsertDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operationService.create(dto));
    }

    @PreAuthorize("hasAuthority('MANAGE_CATALOG')")
    @PutMapping("/{code}")
    @Operation(summary = "Обновить операцию", description = "Требует MANAGE_CATALOG.")
    public OperationDto update(@PathVariable String code, @RequestBody ApplicationTagUpsertDto dto) {
        return operationService.update(code, dto);
    }

    @PreAuthorize("hasAuthority('MANAGE_CATALOG')")
    @DeleteMapping("/{code}")
    @Operation(summary = "Удалить операцию", description = "Требует MANAGE_CATALOG.")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        operationService.delete(code);
        return ResponseEntity.noContent().build();
    }
}
