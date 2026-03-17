package com.wpw.pim.web.controller;

import com.wpw.pim.service.export.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping
    public ResponseEntity<byte[]> export(
        @RequestParam(defaultValue = "csv") String format,
        @RequestParam(defaultValue = "en") String locale,
        @RequestParam(required = false) List<String> toolMaterial,
        @RequestParam(required = false) List<String> workpieceMaterial,
        @RequestParam(required = false) BigDecimal dMmMin,
        @RequestParam(required = false) BigDecimal dMmMax
    ) {
        byte[] data = exportService.export(format, locale, toolMaterial, workpieceMaterial, dMmMin, dMmMax);

        String contentType = switch (format.toLowerCase()) {
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xml"  -> "application/xml";
            default     -> "text/csv";
        };
        String filename = "wpw-products." + format.toLowerCase();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType(contentType))
            .body(data);
    }
}
