package com.wpw.pim.service.product;

import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
import com.wpw.pim.domain.enums.StockStatus;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.web.dto.product.ProductFilter;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link ProductFilterSpec}.
 * Проверяют формирование JPA Predicate на основе фильтра.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductFilterSpecTest {

    @Mock private Root<Product> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Join<Object, Object> attrsJoin;
    @Mock private Predicate predicate;
    @Mock private Path<Object> path;
    @Mock private Subquery<Long> subquery;

    @BeforeEach
    void setUp() {
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(predicate);
        lenient().when(cb.greaterThan(any(Expression.class), anyLong())).thenReturn(predicate);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(path.get(anyString())).thenReturn(path);
    }

    // ========================= Минимальный фильтр =========================

    @Test
    @DisplayName("toPredicate — пустой фильтр добавляет только status=active")
    void toPredicate_emptyFilter_onlyStatusActive() {
        ProductFilter filter = new ProductFilter("en", null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, 1, 48);

        ProductFilterSpec spec = new ProductFilterSpec(filter);
        spec.toPredicate(root, query, cb);

        // Должен быть вызван cb.equal для status
        verify(cb).equal(any(), eq(ProductStatus.active));
        // Не должен joinить attributes — нет фильтров по атрибутам
        verify(root, never()).join(eq("attributes"), any(JoinType.class));
    }

    // ========================= Фильтры по атрибутам =========================

    @Nested
    @DisplayName("Attribute filters")
    class AttributeFilters {

        @BeforeEach
        void setUpJoin() {
            when(root.join(eq("attributes"), eq(JoinType.LEFT))).thenReturn(attrsJoin);
            when(attrsJoin.get(anyString())).thenReturn(path);
        }

        @Test
        void toPredicate_dMmRange_joinsAttributesAndAddsPredicate() {
            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                null, null, null, null, null,
                new BigDecimal("10"), new BigDecimal("20"), null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(root).join("attributes", JoinType.LEFT);
            verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(new BigDecimal("10")));
            verify(cb).lessThanOrEqualTo(any(Expression.class), eq(new BigDecimal("20")));
        }

        @Test
        void toPredicate_shankMm_addsPredicate() {
            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                null, null, null, null, null,
                null, null, new BigDecimal("8"), null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(cb).equal(any(), eq(new BigDecimal("8")));
        }

        @Test
        void toPredicate_hasBallBearing_addsPredicate() {
            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                null, null, null, null, null,
                null, null, null, true, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(cb).equal(any(), eq(true));
        }

        @Test
        void toPredicate_inStock_addsStockStatusPredicate() {
            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, true, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(cb).equal(any(), eq(StockStatus.in_stock));
        }

        @Test
        void toPredicate_cuttingType_addsInPredicate() {
            when(path.in(anyList())).thenReturn(predicate);

            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                null, null, null, null, List.of("straight", "compression"),
                null, null, null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(path).in(List.of("straight", "compression"));
        }
    }

    // ========================= Каталог фильтры =========================

    @Nested
    @DisplayName("Catalog filters")
    class CatalogFilters {

        @Test
        void toPredicate_groupId_addsGroupFilter() {
            UUID groupId = UUID.randomUUID();
            ProductFilter filter = new ProductFilter("en", null, null, groupId, null,
                null, null, null, null, null, null, null, null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(root, atLeastOnce()).get("group");
        }

        @Test
        void toPredicate_categoryId_addsCategoryFilter() {
            UUID categoryId = UUID.randomUUID();
            ProductFilter filter = new ProductFilter("en", null, categoryId, null, null,
                null, null, null, null, null, null, null, null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(root, atLeastOnce()).get("group");
        }

        @Test
        void toPredicate_sectionId_addsSectionFilter() {
            UUID sectionId = UUID.randomUUID();
            ProductFilter filter = new ProductFilter("en", sectionId, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(root, atLeastOnce()).get("group");
        }
    }

    // ========================= ProductType фильтр =========================

    @Test
    @DisplayName("toPredicate — productType=main добавляет фильтр")
    void toPredicate_productType_addsPredicate() {
        ProductFilter filter = new ProductFilter("en", null, null, null, null,
            null, null, null, null, null, null, null, null, null, "main", null, 1, 48);

        new ProductFilterSpec(filter).toPredicate(root, query, cb);

        verify(cb).equal(any(), eq(ProductType.main));
    }

    @Test
    @DisplayName("toPredicate — invalid productType игнорируется")
    void toPredicate_invalidProductType_ignored() {
        ProductFilter filter = new ProductFilter("en", null, null, null, null,
            null, null, null, null, null, null, null, null, null, "invalid_type", null, 1, 48);

        // Should not throw, just ignore
        new ProductFilterSpec(filter).toPredicate(root, query, cb);

        // Only status=active predicate should be created
        verify(cb).equal(any(), eq(ProductStatus.active));
    }

    // ========================= M2M subquery filters =========================

    @Nested
    @DisplayName("M2M subquery filters")
    class M2MFilters {

        @BeforeEach
        void setUpSubquery() {
            when(query.subquery(Long.class)).thenReturn(subquery);
            when(subquery.correlate(root)).thenReturn(root);
            Join mockJoin = mock(Join.class);
            when(root.join(anyString())).thenReturn(mockJoin);
            when(mockJoin.in(anyList())).thenReturn(predicate);
            when(subquery.select(any())).thenReturn(subquery);
            when(subquery.where(any(Predicate.class))).thenReturn(subquery);
            when(cb.count(any())).thenReturn(mock(Expression.class));
        }

        @Test
        void toPredicate_toolMaterial_addsSubquery() {
            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                List.of("carbide"), null, null, null, null, null, null, null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(query).subquery(Long.class);
        }

        @Test
        void toPredicate_workpieceMaterial_addsSubquery() {
            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                null, List.of("wood"), null, null, null, null, null, null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(query).subquery(Long.class);
        }

        @Test
        void toPredicate_machineType_addsSubquery() {
            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                null, null, List.of("cnc_router"), null, null, null, null, null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(query).subquery(Long.class);
        }

        @Test
        void toPredicate_machineBrand_addsSubquery() {
            ProductFilter filter = new ProductFilter("en", null, null, null, null,
                null, null, null, List.of("biesse"), null, null, null, null, null, null, null, 1, 48);

            new ProductFilterSpec(filter).toPredicate(root, query, cb);

            verify(query).subquery(Long.class);
        }
    }

    // ========================= Operation filter =========================

    @Test
    @DisplayName("toPredicate — operation добавляет subquery")
    void toPredicate_operation_addsSubquery() {
        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.correlate(root)).thenReturn(root);
        Join mockJoin = mock(Join.class);
        when(root.join(anyString())).thenReturn(mockJoin);
        when(subquery.select(any())).thenReturn(subquery);
        when(subquery.where(any(Predicate.class))).thenReturn(subquery);
        when(cb.count(any())).thenReturn(mock(Expression.class));
        when(cb.equal(any(), anyString())).thenReturn(predicate);

        ProductFilter filter = new ProductFilter("en", null, null, null, "routing",
            null, null, null, null, null, null, null, null, null, null, null, 1, 48);

        new ProductFilterSpec(filter).toPredicate(root, query, cb);

        verify(query).subquery(Long.class);
    }

    // ========================= Distinct =========================

    @Test
    @DisplayName("toPredicate — content query sets distinct=true")
    void toPredicate_contentQuery_setsDistinct() {
        when(query.getResultType()).thenReturn((Class) Product.class);

        ProductFilter filter = new ProductFilter("en", null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, 1, 48);

        new ProductFilterSpec(filter).toPredicate(root, query, cb);

        verify(query).distinct(true);
    }

    @Test
    @DisplayName("toPredicate — count query не sets distinct")
    void toPredicate_countQuery_doesNotSetDistinct() {
        when(query.getResultType()).thenReturn((Class) Long.class);

        ProductFilter filter = new ProductFilter("en", null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, 1, 48);

        new ProductFilterSpec(filter).toPredicate(root, query, cb);

        verify(query, never()).distinct(anyBoolean());
    }
}
