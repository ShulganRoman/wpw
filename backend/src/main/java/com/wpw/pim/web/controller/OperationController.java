package com.wpw.pim.web.controller;

import com.wpw.pim.service.operation.OperationService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.operation.OperationDto;
import com.wpw.pim.web.dto.product.ProductSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @GetMapping
    public List<OperationDto> list() {
        return operationService.findAll().stream()
            .map(OperationDto::from)
            .toList();
    }

    @GetMapping("/{code}/products")
    public PagedResponse<ProductSummaryDto> productsByOperation(
        @PathVariable String code,
        @RequestParam(defaultValue = "en") String locale,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "48") int perPage
    ) {
        return operationService.findProductsByOperation(code, locale, page, perPage);
    }
}
