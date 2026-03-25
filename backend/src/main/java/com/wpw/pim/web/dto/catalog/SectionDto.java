package com.wpw.pim.web.dto.catalog;

import java.util.List;
import java.util.UUID;

public record SectionDto(UUID id, String slug, String name, int sortOrder, List<CategoryDto> categories) {}
