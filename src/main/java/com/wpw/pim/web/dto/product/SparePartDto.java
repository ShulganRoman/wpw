package com.wpw.pim.web.dto.product;

import com.wpw.pim.domain.enums.PartRole;

import java.util.UUID;

public record SparePartDto(UUID id, String toolNo, String name, PartRole partRole) {}
