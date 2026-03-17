package com.wpw.pim.repository.product;

import com.wpw.pim.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByToolNo(String toolNo);

    @Query("SELECT p.group.id FROM Product p WHERE p.id = :productId")
    UUID findGroupIdByProductId(UUID productId);

    boolean existsByToolNo(String toolNo);
}
