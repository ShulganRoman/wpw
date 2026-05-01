package com.wpw.pim.repository.cart;

import com.wpw.pim.domain.cart.CartItem;
import com.wpw.pim.domain.cart.CartItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {

    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product p WHERE ci.dealer.id = :dealerId ORDER BY ci.addedAt ASC")
    List<CartItem> findByDealerIdWithProduct(UUID dealerId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.dealer.id = :dealerId")
    void deleteByDealerId(UUID dealerId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.dealer.id = :dealerId AND ci.product.id = :productId")
    void deleteByDealerIdAndProductId(UUID dealerId, UUID productId);

    @Query("SELECT ci.product.id FROM CartItem ci WHERE ci.dealer.id = :dealerId")
    List<UUID> findProductIdsByDealerId(UUID dealerId);
}
