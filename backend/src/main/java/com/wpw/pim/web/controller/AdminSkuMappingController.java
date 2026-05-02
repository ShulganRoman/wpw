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
@Tag(name = "Admin: SKU Mapping", description = "Dealer SKU mapping management (WPW SKU → dealer SKU). Requires MANAGE_DEALERS.")
public class AdminSkuMappingController {

    private final SkuMappingService service;

    @GetMapping
    @Operation(summary = "List dealer SKU mappings")
    public List<SkuMappingDto> list(@PathVariable UUID dealerId) {
        return service.list(dealerId);
    }

    @PutMapping
    @Operation(summary = "Create or update SKU mapping")
    public SkuMappingDto upsert(@PathVariable UUID dealerId, @RequestBody SkuMappingDto req) {
        return service.upsert(dealerId, req);
    }

    @DeleteMapping("/{wpwSku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete SKU mapping")
    public void delete(@PathVariable UUID dealerId, @PathVariable String wpwSku) {
        service.delete(dealerId, wpwSku);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate SKU mapping file", description = "Validates Excel without writing to DB.")
    public SkuMappingService.ValidationReport validate(
        @PathVariable UUID dealerId,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        return service.validate(file);
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import SKU mapping from Excel", description = "skipGhosts=true — skip rows with non-existent WPW SKUs.")
    public SkuMappingService.SkuMappingImportResult execute(
        @PathVariable UUID dealerId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "false") boolean skipGhosts
    ) throws IOException {
        return service.execute(dealerId, file, skipGhosts);
    }

    @GetMapping("/export")
    @Operation(summary = "Export SKU mapping to Excel")
    public ResponseEntity<byte[]> export(@PathVariable UUID dealerId) throws IOException {
        byte[] bytes = service.export(dealerId);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sku-mapping-" + dealerId + ".xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @GetMapping("/template")
    @Operation(summary = "Download SKU mapping template")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] bytes = service.template();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sku-mapping-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }
}
