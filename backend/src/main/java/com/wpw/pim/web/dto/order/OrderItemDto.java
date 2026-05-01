package com.wpw.pim.web.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
    UUID productId,
    String toolNo,
    String name,
    int qty,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {}
