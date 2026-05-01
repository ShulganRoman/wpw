package com.wpw.pim.service.settings;

import com.wpw.pim.web.dto.settings.SystemStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link SystemStatsService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemStatsServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    private SystemStatsService service;

    @BeforeEach
    void setUp() {
        service = new SystemStatsService(jdbc);
    }

    /**
     * Настройка моков с указанным количеством активных продуктов и продуктов с медиа.
     * Остальные значения — фиксированные осмысленные дефолты.
     */
    private void setupMocks(long totalActive, long withMedia, Long activeWithPrice, Long totalFiles) {
        Map<String, Object> prodRow = new HashMap<>();
        prodRow.put("total_active", totalActive);
        prodRow.put("with_media", withMedia);
        prodRow.put("total_files", 999L);
        when(jdbc.queryForMap(contains("FROM products p"))).thenReturn(prodRow);

        when(jdbc.queryForObject(contains("FROM media_files"), eq(Long.class))).thenReturn(totalFiles);

        Map<String, Object> stockRow = new HashMap<>();
        stockRow.put("total_items", 50L);
        stockRow.put("distinct_products", 30L);
        when(jdbc.queryForMap(contains("price_list_items"))).thenReturn(stockRow);

        when(jdbc.queryForObject(contains("JOIN price_list_items"), eq(Long.class))).thenReturn(activeWithPrice);

        Map<String, Object> dealerRow = new HashMap<>();
        dealerRow.put("total", 8L);
        dealerRow.put("active", 5L);
        dealerRow.put("with_price_list", 3L);
        when(jdbc.queryForMap(contains("FROM dealers"))).thenReturn(dealerRow);

        Map<String, Object> skuRow = new HashMap<>();
        skuRow.put("dealers_with_mapping", 2L);
        skuRow.put("total_mappings", 100L);
        when(jdbc.queryForMap(contains("dealer_sku_mapping"))).thenReturn(skuRow);

        Map<String, Object> catRow = new HashMap<>();
        catRow.put("sections", 4L);
        catRow.put("categories", 12L);
        catRow.put("groups", 40L);
        catRow.put("empty_groups", 5L);
        catRow.put("nodes_with_image", 30L);
        when(jdbc.queryForMap(contains("FROM sections"))).thenReturn(catRow);
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("getStats -- возвращает DTO с правильными значениями")
        void getStats_returnsDtoWithCorrectValues() {
            setupMocks(10L, 5L, 7L, 200L);

            SystemStatsDto dto = service.getStats();

            assertThat(dto.totalActiveProducts()).isEqualTo(10L);
            assertThat(dto.productsWithOwnMedia()).isEqualTo(5L);
            assertThat(dto.productsWithoutOwnMedia()).isEqualTo(5L);
            assertThat(dto.totalMediaFiles()).isEqualTo(200L);

            assertThat(dto.stockPriceListItems()).isEqualTo(50L);
            assertThat(dto.productsInStockPriceList()).isEqualTo(30L);
            assertThat(dto.activeProductsWithStockPrice()).isEqualTo(7L);
            assertThat(dto.activeProductsWithoutStockPrice()).isEqualTo(3L);

            assertThat(dto.totalDealers()).isEqualTo(8L);
            assertThat(dto.activeDealers()).isEqualTo(5L);
            assertThat(dto.dealersWithPriceList()).isEqualTo(3L);
            assertThat(dto.dealersWithoutPriceList()).isEqualTo(5L);
            assertThat(dto.dealersWithSkuMapping()).isEqualTo(2L);
            assertThat(dto.totalSkuMappings()).isEqualTo(100L);

            assertThat(dto.totalSections()).isEqualTo(4L);
            assertThat(dto.totalCategories()).isEqualTo(12L);
            assertThat(dto.totalProductGroups()).isEqualTo(40L);
            assertThat(dto.emptyProductGroups()).isEqualTo(5L);
            assertThat(dto.catalogNodesWithImage()).isEqualTo(30L);
        }

        @Test
        @DisplayName("getStats -- null от queryForObject обрабатывается как 0")
        void getStats_nullFromQueryForObject_returnsZero() {
            setupMocks(10L, 5L, null, null);

            SystemStatsDto dto = service.getStats();

            assertThat(dto.totalMediaFiles()).isZero();
            assertThat(dto.activeProductsWithStockPrice()).isZero();
            assertThat(dto.activeProductsWithoutStockPrice()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getStats -- mediaCoveragePct = 0 когда totalActive = 0")
        void getStats_mediaCoveragePct_zeroWhenNoActive() {
            setupMocks(0L, 0L, 0L, 0L);

            SystemStatsDto dto = service.getStats();

            assertThat(dto.mediaCoveragePct()).isZero();
            assertThat(dto.stockPriceCoveragePct()).isZero();
        }

        @Test
        @DisplayName("getStats -- mediaCoveragePct = 50.0 при 5 из 10")
        void getStats_mediaCoveragePct_calculatedCorrectly() {
            setupMocks(10L, 5L, 5L, 100L);

            SystemStatsDto dto = service.getStats();

            assertThat(dto.mediaCoveragePct()).isEqualTo(50.0);
            assertThat(dto.stockPriceCoveragePct()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("getStats -- generatedAt не null")
        void getStats_generatedAtNotNull() {
            setupMocks(1L, 1L, 1L, 1L);

            SystemStatsDto dto = service.getStats();

            assertThat(dto.generatedAt()).isNotNull();
        }
    }
}
