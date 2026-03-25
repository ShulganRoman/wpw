package com.wpw.pim.web.dto.dealer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SkuMappingCreateRequest(
    @NotNull UUID productId,
    @NotBlank String dealerSku
) {}
