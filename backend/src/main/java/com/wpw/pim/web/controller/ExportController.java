package com.wpw.pim.web.controller;

import com.wpw.pim.service.export.ExportService;
import com.wpw.pim.web.dto.product.ProductFilter;
import com.wpw.pim.web.dto.product.ProductSummaryDto;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final ProductService productService;

    @GetMapping("/preview")
    public PagedResponse<ProductSummaryDto> preview(
        @RequestParam(defaultValue = "en") String locale,
        @RequestParam(required = false) UUID sectionId,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) UUID groupId,
        @RequestParam(required = false) String operation,
        @RequestParam(required = false) List<String> toolMaterial,
        @RequestParam(required = false) List<String> workpieceMaterial,
        @RequestParam(required = false) List<String> machineType,
        @RequestParam(required = false) List<String> machineBrand,
        @RequestParam(required = false) List<String> cuttingType,
        @RequestParam(required = false) BigDecimal dMmMin,
        @RequestParam(required = false) BigDecimal dMmMax,
        @RequestParam(required = false) BigDecimal shankMm,
        @RequestParam(required = false) Boolean hasBallBearing,
        @RequestParam(required = false) String productType,
        @RequestParam(required = false) Boolean inStock,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int perPage
    ) {
        ProductFilter filter = new ProductFilter(locale, sectionId, categoryId, groupId, operation,
            toolMaterial, workpieceMaterial, machineType, machineBrand, cuttingType,
            dMmMin, dMmMax, shankMm, hasBallBearing, productType, inStock, page, perPage);
        return productService.findAll(filter);
    }

    @GetMapping
    public ResponseEntity<byte[]> export(
        @RequestParam(defaultValue = "csv") String format,
        @RequestParam(defaultValue = "en") String locale,
        @RequestParam(required = false) UUID sectionId,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) UUID groupId,
        @RequestParam(required = false) String operation,
        @RequestParam(required = false) List<String> toolMaterial,
        @RequestParam(required = false) List<String> workpieceMaterial,
        @RequestParam(required = false) List<String> machineType,
        @RequestParam(required = false) List<String> machineBrand,
        @RequestParam(required = false) List<String> cuttingType,
        @RequestParam(required = false) BigDecimal dMmMin,
        @RequestParam(required = false) BigDecimal dMmMax,
        @RequestParam(required = false) BigDecimal shankMm,
        @RequestParam(required = false) Boolean hasBallBearing,
        @RequestParam(required = false) String productType,
        @RequestParam(required = false) Boolean inStock
    ) {
        ProductFilter filter = new ProductFilter(locale, sectionId, categoryId, groupId, operation,
            toolMaterial, workpieceMaterial, machineType, machineBrand, cuttingType,
            dMmMin, dMmMax, shankMm, hasBallBearing, productType, inStock, 1, 10_000);

        byte[] data = exportService.export(format, locale, filter);

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
