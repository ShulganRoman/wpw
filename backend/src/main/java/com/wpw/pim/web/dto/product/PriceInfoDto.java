package com.wpw.pim.web.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PriceInfoDto(
    String currencyCode,
    String currencySymbol,
    List<TierDto> tiers,
    boolean expired,
    LocalDate validTo
) {
    public record TierDto(int minQty, BigDecimal price) {}
}
