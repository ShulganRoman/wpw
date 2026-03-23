package com.wpw.pim.web.controller;

import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PagedResponse<ProductSummaryDto> list(
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
        @RequestParam(defaultValue = "48") int perPage
    ) {
        ProductFilter filter = new ProductFilter(locale, sectionId, categoryId, groupId, operation, toolMaterial, workpieceMaterial,
            machineType, machineBrand, cuttingType, dMmMin, dMmMax, shankMm, hasBallBearing,
            productType, inStock, page, perPage);
        return productService.findAll(filter);
    }

    @GetMapping("/{toolNo}")
    public ProductDetailDto getByToolNo(
        @PathVariable String toolNo,
        @RequestParam(defaultValue = "en") String locale
    ) {
        return productService.findByToolNo(toolNo, locale);
    }

    @GetMapping("/{id}/spare-parts")
    public List<SparePartDto> getSpareParts(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "en") String locale
    ) {
        return productService.getSpareParts(id, locale);
    }

    @GetMapping("/{id}/compatible-tools")
    public List<SparePartDto> getCompatibleTools(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "en") String locale
    ) {
        return productService.getCompatibleTools(id, locale);
    }
}
