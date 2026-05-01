package com.wpw.pim.web.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductFilter(
    String locale,
    UUID sectionId,
    UUID categoryId,
    UUID groupId,
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
    Integer page,
    Integer perPage,
    BigDecimal priceMin,
    BigDecimal priceMax,
    UUID priceListId   // resolved from auth in controller, not sent by client
) {
    public ProductFilter {
        if (locale == null) locale = "en";
        if (page == null || page <= 0) page = 1;
        if (perPage == null || perPage <= 0) perPage = 48;
    }
}
