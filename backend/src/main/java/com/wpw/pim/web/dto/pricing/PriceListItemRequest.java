package com.wpw.pim.web.dto.pricing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PriceListItemRequest(
    @NotBlank String toolNo,
    @Min(1) int minQty,
    @NotNull @Positive BigDecimal price
) {}
