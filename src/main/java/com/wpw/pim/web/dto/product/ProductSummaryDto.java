package com.wpw.pim.web.dto.product;

import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
import com.wpw.pim.domain.enums.StockStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryDto(
    UUID id,
    String toolNo,
    String altToolNo,
    String name,
    String shortDescription,
    ProductType productType,
    ProductStatus status,
    boolean isOrderable,
    BigDecimal dMm,
    BigDecimal shankMm,
    String cuttingType,
    StockStatus stockStatus,
    String thumbnailUrl,
    String locale,
    boolean isRtl
) {}
