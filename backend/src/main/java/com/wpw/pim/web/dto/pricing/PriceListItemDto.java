package com.wpw.pim.web.dto.pricing;

import java.math.BigDecimal;

public record PriceListItemDto(String toolNo, int minQty, BigDecimal price) {}
