package com.wpw.pim.web.dto.dealer;

import jakarta.validation.constraints.NotBlank;

public record SkuMappingCreateRequest(
    @NotBlank String wpwSku,
    @NotBlank String dealerSku,
    String dealerBrand
) {}
