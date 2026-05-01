package com.wpw.pim.web.dto.cart;

import java.math.BigDecimal;

public record PriceTierDto(int minQty, BigDecimal price) {}
