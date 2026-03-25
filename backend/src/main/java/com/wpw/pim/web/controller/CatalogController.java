package com.wpw.pim.web.controller;

import com.wpw.pim.service.catalog.CatalogService;
import com.wpw.pim.web.dto.catalog.SectionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public List<SectionDto> getTree(@RequestParam(defaultValue = "en") String locale) {
        return catalogService.getSectionTree(locale);
    }
}
