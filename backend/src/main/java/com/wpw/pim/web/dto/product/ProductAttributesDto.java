package com.wpw.pim.web.dto.product;

import com.wpw.pim.domain.enums.BoreType;
import com.wpw.pim.domain.enums.RotationDirection;
import com.wpw.pim.domain.enums.StockStatus;

import java.math.BigDecimal;

public record ProductAttributesDto(
    BigDecimal dMm,
    BigDecimal d1Mm,
    BigDecimal d2Mm,
    BigDecimal bMm,
    BigDecimal b1Mm,
    BigDecimal lMm,
    BigDecimal l1Mm,
    BigDecimal rMm,
    BigDecimal aMm,
    BigDecimal angleDeg,
    BigDecimal shankMm,
    String shankInch,
    Integer flutes,
    Integer bladeNo,
    String cuttingType,
    boolean hasBallBearing,
    String ballBearingCode,
    boolean hasRetainer,
    String retainerCode,
    boolean canResharpen,
    RotationDirection rotationDirection,
    BoreType boreType,
    String ean13,
    String upc12,
    String hsCode,
    String countryOfOrigin,
    Integer weightG,
    Integer pkgQty,
    Integer cartonQty,
    StockStatus stockStatus,
    Integer stockQty
) {}
