package com.wpw.pim.repository.product;

import com.wpw.pim.domain.product.ProductSparePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductSparePartRepository extends JpaRepository<ProductSparePart, ProductSparePart.SparePartId> {

    @Query("SELECT sp FROM ProductSparePart sp JOIN FETCH sp.part WHERE sp.product.id = :productId")
    List<ProductSparePart> findByProductId(UUID productId);

    @Query("SELECT sp FROM ProductSparePart sp JOIN FETCH sp.product WHERE sp.part.id = :partId")
    List<ProductSparePart> findByPartId(UUID partId);
}
