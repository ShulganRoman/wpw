package com.wpw.pim.web.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
    List<CartItemDto> items,
    String currency,
    BigDecimal total,
    int totalItems,
    List<String> removedToolNos
) {}
