package com.wpw.pim.web.dto.catalog;
import java.util.Map;
import java.util.UUID;
public record CreateProductGroupRequest(UUID categoryId, String slug, String groupCode, Map<String, String> translations, int sortOrder, boolean isActive) {}
