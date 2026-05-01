package com.wpw.pim.repository.pricing;

import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.pricing.PriceListItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceListItemRepository extends JpaRepository<PriceListItem, PriceListItemId> {

    List<PriceListItem> findByPriceListIdOrderByIdMinQtyAsc(UUID priceListId);

    @Query("""
        SELECT pli FROM PriceListItem pli
        WHERE pli.priceList.id = :priceListId
          AND pli.product.id = :productId
          AND pli.id.minQty <= :qty
        ORDER BY pli.id.minQty DESC
        LIMIT 1
        """)
    Optional<PriceListItem> findBestPrice(UUID priceListId, UUID productId, int qty);

    @Modifying
    @Query("DELETE FROM PriceListItem i WHERE i.priceList.id = :priceListId")
    void deleteByPriceListId(UUID priceListId);

    @Query("SELECT i FROM PriceListItem i WHERE i.priceList.id = :priceListId AND i.product.id = :productId ORDER BY i.id.minQty ASC")
    List<PriceListItem> findByPriceListIdAndProductId(UUID priceListId, UUID productId);

    @Query("SELECT i FROM PriceListItem i WHERE i.priceList.id = :priceListId AND i.product.id IN :productIds ORDER BY i.product.id ASC, i.id.minQty ASC")
    List<PriceListItem> findByPriceListIdAndProductIds(UUID priceListId, List<UUID> productIds);
}
