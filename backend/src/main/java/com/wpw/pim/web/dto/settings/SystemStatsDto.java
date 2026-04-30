package com.wpw.pim.web.dto.settings;

import java.time.OffsetDateTime;

public record SystemStatsDto(
    // Товары
    long totalActiveProducts,
    long productsWithOwnMedia,
    long productsWithoutOwnMedia,
    double mediaCoveragePct,

    // Медиафайлы
    long totalMediaFiles,

    // Публичный прайс-лист (stock)
    long stockPriceListItems,
    long productsInStockPriceList,
    long activeProductsWithStockPrice,
    long activeProductsWithoutStockPrice,
    double stockPriceCoveragePct,

    // Дилеры
    long totalDealers,
    long activeDealers,
    long dealersWithPriceList,
    long dealersWithoutPriceList,
    long dealersWithSkuMapping,
    long totalSkuMappings,

    // Каталог
    long totalSections,
    long totalCategories,
    long totalProductGroups,
    long emptyProductGroups,
    long catalogNodesWithImage,

    OffsetDateTime generatedAt
) {}
