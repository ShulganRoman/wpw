package com.wpw.pim.web.controller;

import com.wpw.pim.service.dealer.SkuMappingService;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
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
public class AdminSkuMappingController {

    private final SkuMappingService service;

    @GetMapping
    public List<SkuMappingDto> list(@PathVariable UUID dealerId) {
        return service.list(dealerId);
    }

    @PutMapping
    public SkuMappingDto upsert(@PathVariable UUID dealerId, @RequestBody SkuMappingDto req) {
        return service.upsert(dealerId, req);
    }

    @DeleteMapping("/{wpwSku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID dealerId, @PathVariable String wpwSku) {
        service.delete(dealerId, wpwSku);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkuMappingService.ValidationReport validate(
        @PathVariable UUID dealerId,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        return service.validate(file);
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkuMappingService.SkuMappingImportResult execute(
        @PathVariable UUID dealerId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "false") boolean skipGhosts
    ) throws IOException {
        return service.execute(dealerId, file, skipGhosts);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@PathVariable UUID dealerId) throws IOException {
        byte[] bytes = service.export(dealerId);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sku-mapping-" + dealerId + ".xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] bytes = service.template();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sku-mapping-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }
}
