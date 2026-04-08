package com.wpw.pim.repository.product;

import com.wpw.pim.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByToolNo(String toolNo);

    @Query("SELECT p.group.id FROM Product p WHERE p.id = :productId")
    UUID findGroupIdByProductId(UUID productId);

    boolean existsByToolNo(String toolNo);

    @Query("SELECT p.toolNo as toolNo, p.updatedAt as updatedAt FROM Product p WHERE p.status = com.wpw.pim.domain.enums.ProductStatus.active")
    List<ProductSitemapProjection> findAllForSitemap();

    @Query("SELECT DISTINCT m FROM Product p JOIN p.toolMaterials m ORDER BY m")
    List<String> findDistinctToolMaterials();

    @Query("SELECT DISTINCT m FROM Product p JOIN p.workpieceMaterials m ORDER BY m")
    List<String> findDistinctWorkpieceMaterials();

    @Query("SELECT DISTINCT m FROM Product p JOIN p.machineTypes m ORDER BY m")
    List<String> findDistinctMachineTypes();

    @Query("SELECT DISTINCT m FROM Product p JOIN p.machineBrands m ORDER BY m")
    List<String> findDistinctMachineBrands();

    @Query("SELECT DISTINCT a.cuttingType FROM com.wpw.pim.domain.product.ProductAttributes a WHERE a.cuttingType IS NOT NULL AND a.cuttingType <> '' ORDER BY a.cuttingType")
    List<String> findDistinctCuttingTypes();

    @Query("SELECT DISTINCT a.shankMm FROM com.wpw.pim.domain.product.ProductAttributes a WHERE a.shankMm IS NOT NULL ORDER BY a.shankMm")
    List<BigDecimal> findDistinctShankMm();
}
