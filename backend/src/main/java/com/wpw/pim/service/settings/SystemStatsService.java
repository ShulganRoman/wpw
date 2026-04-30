package com.wpw.pim.service.settings;

import com.wpw.pim.web.dto.settings.SystemStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemStatsService {

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public SystemStatsDto getStats() {
        // --- Products ---
        Map<String, Object> prodRow = jdbc.queryForMap("""
            SELECT
              COUNT(*) FILTER (WHERE p.status = 'active') AS total_active,
              COUNT(DISTINCT mf.product_id) FILTER (WHERE p.status = 'active') AS with_media,
              COUNT(*) AS total_files
            FROM products p
            LEFT JOIN media_files mf ON mf.product_id = p.id AND mf.file_type = 'image'
            """);

        long totalActive = toLong(prodRow.get("total_active"));
        long withMedia   = toLong(prodRow.get("with_media"));
        long withoutMedia = totalActive - withMedia;
        double mediaCoverage = totalActive > 0 ? Math.round(withMedia * 1000.0 / totalActive) / 10.0 : 0.0;

        Long totalFiles = jdbc.queryForObject(
            "SELECT COUNT(*) FROM media_files WHERE file_type = 'image'", Long.class);

        // --- Stock price list ---
        Map<String, Object> stockRow = jdbc.queryForMap("""
            SELECT
              COUNT(pli.*)                            AS total_items,
              COUNT(DISTINCT pli.product_id)          AS distinct_products
            FROM price_list_items pli
            JOIN price_lists pl ON pl.id = pli.price_list_id
            WHERE pl.type = 'stock'
            """);
        long stockItems    = toLong(stockRow.get("total_items"));
        long stockProducts = toLong(stockRow.get("distinct_products"));

        Long activeWithPrice = jdbc.queryForObject("""
            SELECT COUNT(DISTINCT p.id)
            FROM products p
            JOIN price_list_items pli ON pli.product_id = p.id
            JOIN price_lists pl ON pl.id = pli.price_list_id AND pl.type = 'stock'
            WHERE p.status = 'active'
            """, Long.class);
        if (activeWithPrice == null) activeWithPrice = 0L;
        long activeWithoutPrice = totalActive - activeWithPrice;
        double priceCoverage = totalActive > 0
            ? Math.round(activeWithPrice * 1000.0 / totalActive) / 10.0 : 0.0;

        // --- Dealers ---
        Map<String, Object> dealerRow = jdbc.queryForMap("""
            SELECT
              COUNT(*)                                     AS total,
              COUNT(*) FILTER (WHERE is_active = TRUE)     AS active,
              COUNT(*) FILTER (WHERE price_list_id IS NOT NULL) AS with_price_list
            FROM dealers
            """);
        long totalDealers       = toLong(dealerRow.get("total"));
        long activeDealers      = toLong(dealerRow.get("active"));
        long dealersWithPriceList = toLong(dealerRow.get("with_price_list"));
        long dealersWithoutPriceList = totalDealers - dealersWithPriceList;

        Map<String, Object> skuRow = jdbc.queryForMap("""
            SELECT
              COUNT(DISTINCT dealer_id) AS dealers_with_mapping,
              COUNT(*)                  AS total_mappings
            FROM dealer_sku_mapping
            """);
        long dealersWithSku = toLong(skuRow.get("dealers_with_mapping"));
        long totalSku       = toLong(skuRow.get("total_mappings"));

        // --- Catalog ---
        Map<String, Object> catRow = jdbc.queryForMap("""
            SELECT
              (SELECT COUNT(*) FROM sections)       AS sections,
              (SELECT COUNT(*) FROM categories)     AS categories,
              (SELECT COUNT(*) FROM product_groups) AS groups,
              (SELECT COUNT(*) FROM product_groups pg
               WHERE NOT EXISTS (
                 SELECT 1 FROM products p WHERE p.group_id = pg.id AND p.status = 'active'
               )) AS empty_groups,
              (
                SELECT COUNT(*) FROM (
                  SELECT image_url FROM sections  WHERE image_url IS NOT NULL
                  UNION ALL
                  SELECT image_url FROM categories WHERE image_url IS NOT NULL
                  UNION ALL
                  SELECT image_url FROM product_groups WHERE image_url IS NOT NULL
                ) imgs
              ) AS nodes_with_image
            """);
        long totalSections  = toLong(catRow.get("sections"));
        long totalCategories = toLong(catRow.get("categories"));
        long totalGroups    = toLong(catRow.get("groups"));
        long emptyGroups    = toLong(catRow.get("empty_groups"));
        long nodesWithImage = toLong(catRow.get("nodes_with_image"));

        return new SystemStatsDto(
            totalActive, withMedia, withoutMedia, mediaCoverage,
            totalFiles != null ? totalFiles : 0L,
            stockItems, stockProducts, activeWithPrice, activeWithoutPrice, priceCoverage,
            totalDealers, activeDealers, dealersWithPriceList, dealersWithoutPriceList,
            dealersWithSku, totalSku,
            totalSections, totalCategories, totalGroups, emptyGroups, nodesWithImage,
            OffsetDateTime.now()
        );
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}
