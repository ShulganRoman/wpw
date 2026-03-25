package com.wpw.pim.web.dto.catalog;
import java.util.Map;
public record UpdateCategoryRequest(String slug, Map<String, String> translations, Integer sortOrder, Boolean isActive) {}
