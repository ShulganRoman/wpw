package com.wpw.pim.web.dto.catalog;

import java.util.List;
import java.util.UUID;

public record CategoryDto(UUID id, String slug, String name, int sortOrder, List<ProductGroupDto> groups) {}
