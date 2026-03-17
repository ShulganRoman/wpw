package com.wpw.pim.service.product;

import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
import com.wpw.pim.domain.enums.StockStatus;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.web.dto.product.ProductFilter;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductFilterSpec implements Specification<Product> {

    private final ProductFilter filter;

    public ProductFilterSpec(ProductFilter filter) {
        this.filter = filter;
    }

    @Override
    public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Join on attributes when any attribute-based filter is needed
        Join<Object, Object> attrs = null;

        boolean needsAttrs = hasAny(filter.dMmMin(), filter.dMmMax(), filter.shankMm(), filter.hasBallBearing(), filter.inStock())
            || notEmpty(filter.cuttingType());

        if (needsAttrs) {
            attrs = root.join("attributes", JoinType.LEFT);
        }

        // d_mm range
        if (filter.dMmMin() != null && attrs != null) {
            predicates.add(cb.greaterThanOrEqualTo(attrs.get("dMm"), filter.dMmMin()));
        }
        if (filter.dMmMax() != null && attrs != null) {
            predicates.add(cb.lessThanOrEqualTo(attrs.get("dMm"), filter.dMmMax()));
        }

        // shank_mm
        if (filter.shankMm() != null && attrs != null) {
            predicates.add(cb.equal(attrs.get("shankMm"), filter.shankMm()));
        }

        // hasBallBearing
        if (filter.hasBallBearing() != null && attrs != null) {
            predicates.add(cb.equal(attrs.get("hasBallBearing"), filter.hasBallBearing()));
        }

        // inStock
        if (Boolean.TRUE.equals(filter.inStock()) && attrs != null) {
            predicates.add(cb.equal(attrs.get("stockStatus"), StockStatus.in_stock));
        }

        // cuttingType
        if (notEmpty(filter.cuttingType()) && attrs != null) {
            predicates.add(attrs.get("cuttingType").in(filter.cuttingType()));
        }

        // productType
        if (filter.productType() != null) {
            try {
                predicates.add(cb.equal(root.get("productType"), ProductType.valueOf(filter.productType())));
            } catch (IllegalArgumentException ignored) {}
        }

        // status — по умолчанию только active
        predicates.add(cb.equal(root.get("status"), ProductStatus.active));

        // tool_material (M2M via ElementCollection subquery)
        if (notEmpty(filter.toolMaterial())) {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Product> subRoot = sub.correlate(root);
            Join<?, ?> mat = subRoot.join("toolMaterials");
            sub.select(cb.count(mat)).where(mat.in(filter.toolMaterial()));
            predicates.add(cb.greaterThan(sub, 0L));
        }

        // workpiece_material
        if (notEmpty(filter.workpieceMaterial())) {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Product> subRoot = sub.correlate(root);
            Join<?, ?> mat = subRoot.join("workpieceMaterials");
            sub.select(cb.count(mat)).where(mat.in(filter.workpieceMaterial()));
            predicates.add(cb.greaterThan(sub, 0L));
        }

        // machine_type
        if (notEmpty(filter.machineType())) {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Product> subRoot = sub.correlate(root);
            Join<?, ?> mt = subRoot.join("machineTypes");
            sub.select(cb.count(mt)).where(mt.in(filter.machineType()));
            predicates.add(cb.greaterThan(sub, 0L));
        }

        // machine_brand
        if (notEmpty(filter.machineBrand())) {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Product> subRoot = sub.correlate(root);
            Join<?, ?> mb = subRoot.join("machineBrands");
            sub.select(cb.count(mb)).where(mb.in(filter.machineBrand()));
            predicates.add(cb.greaterThan(sub, 0L));
        }

        // operation
        if (filter.operation() != null && !filter.operation().isBlank()) {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Product> subRoot = sub.correlate(root);
            Join<?, ?> op = subRoot.join("operationCodes");
            sub.select(cb.count(op)).where(cb.equal(op, filter.operation()));
            predicates.add(cb.greaterThan(sub, 0L));
        }

        // distinct только для content-запроса, не для count-запроса
        if (query.getResultType() != Long.class) {
            query.distinct(true);
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private boolean hasAny(Object... values) {
        for (Object v : values) if (v != null) return true;
        return false;
    }

    private boolean notEmpty(java.util.List<?> list) {
        return list != null && !list.isEmpty();
    }
}
