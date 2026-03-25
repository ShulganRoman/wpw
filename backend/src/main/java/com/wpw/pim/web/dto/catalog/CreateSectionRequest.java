package com.wpw.pim.web.dto.catalog;
import java.util.Map;
public record CreateSectionRequest(String slug, Map<String, String> translations, int sortOrder, boolean isActive) {}
