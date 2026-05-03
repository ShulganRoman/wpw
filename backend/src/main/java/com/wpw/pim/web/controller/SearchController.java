package com.wpw.pim.web.controller;

import com.wpw.pim.service.search.SearchService;
import com.wpw.pim.web.dto.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text product search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search products", description = "Full-text search by name, description and SKU. Supports pagination.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results with pagination"),
        @ApiResponse(responseCode = "400", description = "Query parameter required")
    })
    public PagedResponse<Map<String, Object>> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "en") String locale,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int perPage
    ) {
        List<Map<String, Object>> results = searchService.search(q, locale, page, perPage);
        long total = searchService.countSearch(q, locale);
        return PagedResponse.of(results, total, page, perPage);
    }
}
