package com.wpw.pim.web.controller;

import com.wpw.pim.service.search.SearchService;
import com.wpw.pim.web.dto.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Полнотекстовый поиск по товарам")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Поиск товаров", description = "Полнотекстовый поиск по названию, описанию и артикулу. Поддерживает пагинацию.")
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
