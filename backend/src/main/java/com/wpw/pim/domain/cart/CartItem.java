package com.wpw.pim.domain.cart;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cart_items")
@Getter @Setter @NoArgsConstructor
public class CartItem {

    @EmbeddedId
    private CartItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("dealerId")
    @JoinColumn(name = "dealer_id")
    private Dealer dealer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int qty = 1;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();

    public CartItem(Dealer dealer, Product product, int qty) {
        this.id = new CartItemId(dealer.getId(), product.getId());
        this.dealer = dealer;
        this.product = product;
        this.qty = qty;
        this.addedAt = OffsetDateTime.now();
    }
}
