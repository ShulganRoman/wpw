package com.wpw.pim.web.dto.catalog;
import java.util.Map;
public record UpdateProductGroupRequest(String slug, String groupCode, Map<String, String> translations, Integer sortOrder, Boolean isActive) {}
