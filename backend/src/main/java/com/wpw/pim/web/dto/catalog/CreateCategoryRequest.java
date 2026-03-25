package com.wpw.pim.web.dto.catalog;
import java.util.Map;
import java.util.UUID;
public record CreateCategoryRequest(UUID sectionId, String slug, Map<String, String> translations, int sortOrder, boolean isActive) {}
