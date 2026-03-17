package com.wpw.pim.web.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductFilter(
    String locale,
    String operation,
    List<String> toolMaterial,
    List<String> workpieceMaterial,
    List<String> machineType,
    List<String> machineBrand,
    List<String> cuttingType,
    BigDecimal dMmMin,
    BigDecimal dMmMax,
    BigDecimal shankMm,
    Boolean hasBallBearing,
    String productType,
    Boolean inStock,
    int page,
    int perPage
) {
    public ProductFilter {
        if (locale == null) locale = "en";
        if (page <= 0) page = 1;
        if (perPage <= 0) perPage = 48;
    }
}
