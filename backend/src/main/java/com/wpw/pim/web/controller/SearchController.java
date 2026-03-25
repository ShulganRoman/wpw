package com.wpw.pim.web.controller;

import com.wpw.pim.service.search.SearchService;
import com.wpw.pim.web.dto.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
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
