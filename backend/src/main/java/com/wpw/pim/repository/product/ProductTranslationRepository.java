package com.wpw.pim.repository.product;

import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.domain.product.ProductTranslationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductTranslationRepository extends JpaRepository<ProductTranslation, ProductTranslationId> {

    Optional<ProductTranslation> findByIdProductIdAndIdLocale(UUID productId, String locale);

    @Query("SELECT pt FROM ProductTranslation pt WHERE pt.id.productId IN :productIds AND pt.id.locale = :locale")
    List<ProductTranslation> findByProductIdsAndLocale(List<UUID> productIds, String locale);
}
