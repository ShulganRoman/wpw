package com.wpw.pim.web.dto.catalog;
import java.util.Map;
public record UpdateSectionRequest(String slug, Map<String, String> translations, Integer sortOrder, Boolean isActive) {}
