package com.wpw.pim.web.dto.dealer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PriceListDto(
    UUID priceListId,
    String name,
    String currencyCode,
    String currencySymbol,
    List<PriceItemDto> items
) {
    public record PriceItemDto(UUID productId, String toolNo, BigDecimal price, int minQty) {}
}
