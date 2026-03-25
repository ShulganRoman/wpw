package com.wpw.pim.web.dto.catalog;

import java.util.UUID;

public record ProductGroupDto(UUID id, String slug, String groupCode, String name, int sortOrder) {}
