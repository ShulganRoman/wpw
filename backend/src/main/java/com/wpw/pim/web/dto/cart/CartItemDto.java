package com.wpw.pim.web.dto.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItemDto(
    UUID productId,
    String toolNo,
    String name,
    String imageUrl,
    int qty,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    List<PriceTierDto> tiers
) {}
