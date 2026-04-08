package com.wpw.pim.service.excel.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Одна строка из WPW_Catalog_v3.xlsx (без SEO-колонок).
 * Все значения — строки; конвертация происходит в сервисе.
 */
@Getter
@Builder
public class WpwCatalogRow {
    private final int    rowNum;
    private final String sku;
    private final String productType;
    private final String category;
    private final String group;
    private final String descriptionEn;
    private final String nameRu;
    private final String aiDescriptionEn;
    private final String dMm;
    private final String d1Mm;
    private final String bMm;
    private final String lMm;
    private final String rMm;
    private final String angleDeg;
    private final String shankMm;
    private final String shankInch;
    private final String flutes;
    private final String toolMaterial;
    private final String cuttingType;
    private final String applicationTags;
    private final String workpieceMaterial;
    private final String machineType;
    private final String dataCompleteness;
    private final String spareParts;
    private final String relatedProducts;
    private final String setComponents;
}
